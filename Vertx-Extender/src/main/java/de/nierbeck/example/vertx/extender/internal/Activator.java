/*
   Copyright 2016 Achim Nierbeck

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package de.nierbeck.example.vertx.extender.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, BundleTrackerCustomizer<Bundle>, BundleListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private BundleContext bundleContext;
    private BundleTracker<Bundle> tracker;

    private final ConcurrentMap<Bundle, VerticleExtenderImpl> verticlesExtenders = new ConcurrentHashMap<Bundle, VerticleExtenderImpl>();
    @SuppressWarnings("rawtypes")
    private final ConcurrentMap<Bundle, FutureTask> destroying = new ConcurrentHashMap<Bundle, FutureTask>();
    private ExecutorService executors;

    private boolean synchronous;
    private boolean stopping;
    private boolean stopped;
    private boolean preemptiveShutdown;
    private VerticleObserver verticleObserver;

    @Override
    public void start(BundleContext context) throws Exception {
        logger.info("Starting Vertx-Extender");
        bundleContext = context;
        bundleContext.addBundleListener(this);
        this.tracker = new BundleTracker<>(bundleContext, Bundle.ACTIVE | Bundle.STARTING, this);
        if (!this.synchronous) {
            this.executors = Executors.newScheduledThreadPool(3);
        }
        verticleObserver = new VerticleObserver();
        this.tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        stopping = true;
        while (!verticlesExtenders.isEmpty()) {
            Collection<Bundle> toDestroy = chooseBundlesToDestroy(verticlesExtenders.keySet());
            if (toDestroy == null || toDestroy.isEmpty()) {
                toDestroy = new ArrayList<>(verticlesExtenders.keySet());
            }
            for (Bundle bundle : toDestroy) {
                destroyExtension(bundle);
            }
        }
        this.tracker.close();
        if (executors != null) {
            executors.shutdown();
            try {
                executors.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Ignore
            }
            executors = null;
        }
        stopped = true;
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        logger.debug("Bundle changed: {}", event);
        if (stopped) {
            return;
        }
        Bundle bundle = event.getBundle();
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
            // The bundle is not in STARTING or ACTIVE state anymore
            // so destroy the bundleContext. Ignore our own bundle since it
            // needs to kick the orderly shutdown.
            if (bundle != this.bundleContext.getBundle()) {
                destroyExtension(bundle);
            }
        }
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        logger.debug("adding bundle: {}", bundle.getSymbolicName());
        modifiedBundle(bundle, event, bundle);
        return bundle;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        logger.debug("modified bundle: {}", bundle.getSymbolicName());
        // If the bundle being stopped is the system bundle,
        // do an orderly shutdown of all blueprint contexts now
        // so that service usage can actually be useful
        if (bundle.getBundleId() == 0 && bundle.getState() == Bundle.STOPPING) {
            if (preemptiveShutdown) {
                try {
                    stop(bundleContext);
                } catch (Exception e) {
                    logger.error("Error while performing preemptive shutdown", e);
                }
                return;
            }
        }
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
            // The bundle is not in STARTING or ACTIVE state anymore
            // so destroy the bundleContext. Ignore our own bundle since it
            // needs to kick the orderly shutdown and not unregister the
            // namespaces.
            if (bundle != this.bundleContext.getBundle()) {
                destroyExtension(bundle);
            }
            return;
        }
        // Do not track bundles given we are stopping
        if (stopping) {
            return;
        }
        // For starting bundles, ensure, it's a lazy activation,
        // else we'll wait for the bundle to become ACTIVE
        if (bundle.getState() == Bundle.STARTING) {
            String activationPolicyHeader = (String) bundle.getHeaders().get(Constants.BUNDLE_ACTIVATIONPOLICY);
            if (activationPolicyHeader == null || !activationPolicyHeader.startsWith(Constants.ACTIVATION_LAZY)) {
                // Do not track this bundle yet
                return;
            }
        }
        createExtension(bundle);
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        logger.debug("remove bundle: {}", bundle.getSymbolicName());
        // Nothing to do
        destroyExtension(bundle);
    }

    private void createExtension(Bundle bundle) {
        logger.debug("trying to create extender for bundle: {}", bundle.getSymbolicName());
        try {
            BundleContext context = bundle.getBundleContext();
            if (context == null) {
                // The bundle has been stopped in the mean time
                logger.debug("context null ...");
                return;
            }
            final VerticleExtenderImpl verticleExtender = verticleObserver.createExtender(bundle);
            if (verticleExtender == null) {
                // This bundle is not to be extended
                logger.debug("verticleExtender == null");
                return;
            }
            synchronized (verticlesExtenders) {
                if (verticlesExtenders.putIfAbsent(bundle, verticleExtender) != null) {
                    return;
                }
            }
            if (synchronous) {
                logger.debug("Starting extension for bundle {} synchronously", bundle.getSymbolicName());
                verticleExtender.start();
            } else {
                logger.debug("Scheduling start of extension for bundle {} asynchronously", bundle.getSymbolicName());
                executors.submit(() -> verticleExtender.start());
            }
        } catch (Throwable t) {
            logger.warn("Error while creating extension for bundle " + bundle, t);
        }
    }

    @SuppressWarnings("unchecked")
    private void destroyExtension(final Bundle bundle) {
        FutureTask<Void> future;
        synchronized (verticlesExtenders) {
            logger.debug("Starting destruction process for bundle {}", bundle.getSymbolicName());
            future = destroying.get(bundle);
            if (future == null) {
                final VerticleExtenderImpl verticleExtender = verticlesExtenders.remove(bundle);
                if (verticleExtender != null) {
                    logger.debug("Scheduling extension destruction for {}.", bundle.getSymbolicName());
                    future = new FutureTask<>(() -> {
                        logger.info("Destroying extension for bundle {}", bundle.getSymbolicName());
                        try {
                            verticleExtender.destroy();
                        } finally {
                            logger.debug("Finished destroying extension for bundle {}", bundle.getSymbolicName());
                            synchronized (verticlesExtenders) {
                                destroying.remove(bundle);
                            }
                        }
                    }, null);
                    destroying.put(bundle, future);
                } else {
                    logger.debug("Not an extended bundle or destruction of extension already finished for {}.",
                            bundle.getSymbolicName());
                }
            } else {
                logger.debug("Destruction already scheduled for {}.", bundle.getSymbolicName());
            }
        }
        if (future != null) {
            try {
                logger.debug("Waiting for extension destruction for {}.", bundle.getSymbolicName());
                future.run();
                future.get();
            } catch (Throwable t) {
                logger.warn("Error while destroying extension for bundle " + bundle, t);
            }
        }
    }

    protected Collection<Bundle> chooseBundlesToDestroy(Set<Bundle> bundles) {
        return null;
    }
}
