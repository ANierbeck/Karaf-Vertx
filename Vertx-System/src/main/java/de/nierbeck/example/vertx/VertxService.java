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
package de.nierbeck.example.vertx;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.MetricsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import static de.nierbeck.example.vertx.TcclSwitch.executeWithTCCLSwitch;

@interface VertxConfig {
    int getWorkerPoolSize();
    int getEventLoopPoolSize();
    boolean getHAEnabled() default false;
    String getHAGroup();
}

@Component(immediate = true, service = {})
public class VertxService {

    private final static Logger LOGGER = Logger.getLogger("VertxPublisher");
    private ServiceRegistration<Vertx> vertxRegistration;
    private ServiceRegistration<EventBus> ebRegistration;
    private ServiceRegistration<MetricRegistry> metricsRegistration;
    private Vertx vertx;
    private MetricRegistry registry;
    
    @Reference
    private VertxMetricsFactory metrxFactory;
    
    @Reference(cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.DYNAMIC, bind="bindClusterManager", unbind="unbindClusterManager")
    private volatile ClusterManager clusterManager;
    
    private ServiceRegistration<MetricsService> metricsServiceRegistration;
    private BundleContext bundleContext;
    private VertxConfig cfg;

    @Activate
    public void start(BundleContext context, VertxConfig cfg) {
        LOGGER.info("Creating Vert.x instance");
        
        this.bundleContext = context;
        this.cfg = cfg;
        
        createAndRegisterVertx();
        
    }

    @Deactivate
    public void stop(BundleContext context) {
        deregisterVertx();
    }

    private void createAndRegisterVertx() {
        VertxOptions options = new VertxOptions().setMetricsOptions(new DropwizardMetricsOptions()
                .setJmxEnabled(true)
                .setJmxDomain("vertx-metrics")
                .setRegistryName("vertx-karaf-registry")
                .setFactory(metrxFactory)
            );
                
        if (cfg.getWorkerPoolSize() > 0) {
            options.setWorkerPoolSize(cfg.getWorkerPoolSize());
        }
        
        if (cfg.getEventLoopPoolSize() > 0) {
            options.setEventLoopPoolSize(cfg.getEventLoopPoolSize());
        }
        
        if (cfg.getHAEnabled()) {
            options.setHAEnabled(cfg.getHAEnabled());
            if (cfg.getHAGroup() != null && !cfg.getHAGroup().isEmpty()) {
                options.setHAGroup(cfg.getHAGroup());
            }
        }
        
        if (clusterManager != null) {
            LOGGER.info("Starting vertx with cluster manager");
            options.setClusterManager(clusterManager);
        }

        try {
            vertx = executeWithTCCLSwitch(() -> Vertx.vertx(options));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while creating vertx system", e);
            return;
        }
        
        vertxRegistration = bundleContext.registerService(Vertx.class, vertx, null);
        LOGGER.info("Vert.x service registered");
        ebRegistration = bundleContext.registerService(EventBus.class, vertx.eventBus(), null);
        LOGGER.info("Vert.x Event Bus service registered");
        registry = SharedMetricRegistries.getOrCreate("vertx-karaf-registry");
        metricsRegistration = bundleContext.registerService(MetricRegistry.class, registry, null);
        LOGGER.info("Vert.x MetricsService service registered");
        MetricsService metricsService = MetricsService.create(vertx);
        metricsServiceRegistration = bundleContext.registerService(MetricsService.class, metricsService, null);
    }
    
    private void deregisterVertx() {
        if (metricsServiceRegistration != null) {
            metricsServiceRegistration.unregister();
            metricsServiceRegistration = null;
        }
        if (metricsRegistration != null) {
            metricsRegistration.unregister();
            metricsRegistration = null;
        }
        if (ebRegistration != null) {
            ebRegistration.unregister();
            ebRegistration = null;
        }
        if (vertxRegistration != null) {
            vertxRegistration.unregister();
            vertxRegistration = null;
        }
        if (vertx != null) {
            vertx.close();
        }
        if (registry != null) {
            registry = null;
        }
    }
    
    public void bindClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        deregisterVertx();
        createAndRegisterVertx();
    }
    
    public void unbindClusterManager(ClusterManager clusterManager) {
        this.clusterManager = null;
        deregisterVertx();
        createAndRegisterVertx();
    }

}