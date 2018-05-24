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

import io.vertx.core.Verticle;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.extender.BundleManifestScanner;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.ops4j.pax.swissbox.extender.ManifestFilter;
import org.ops4j.pax.swissbox.extender.RegexKeyManifestFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VerticleObserver {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public static Boolean canSeeClass(Bundle bundle, Class<?> clazz) {
        try {
            return bundle.loadClass(clazz.getName()) == clazz;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    

    public VerticleExtenderImpl createExtender(Bundle bundle) {
        NullArgumentException.validateNotNull(bundle, "Bundle");
        if (bundle.getState() != Bundle.ACTIVE) {
            logger.debug("Bundle {} is not in ACTIVE state, ignore it!", bundle);
            return null;
        }

        // Check compatibility
        Boolean canSeeServletClass = canSeeClass(bundle, Verticle.class);
        if (Boolean.FALSE.equals(canSeeServletClass)) {
            logger.debug("Ignore bundle {} which is not compatible with this extender", bundle);
            return null;
        }

        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        
        List<BundleCapability> capabilities = bundleWiring.getCapabilities("osgi.wiring.package");
        List<String> packages = capabilities.stream()
               .map(cap -> cap.getAttributes().get("osgi.wiring.package"))
               .map(o -> ((String)o))
               .distinct()
               .filter(packageName -> Verticle.class.getPackage().getName().equalsIgnoreCase(packageName))
               .collect(Collectors.toList());
            
//        if (packages.size() == 0) {
//            logger.debug("No import for Verticle package found, skipping in bundle {}", bundle);
//            return null;
//        }
        
        ManifestFilter manifestFilter = new RegexKeyManifestFilter("Provide-Capability");
        BundleManifestScanner manifestScanner = new BundleManifestScanner(manifestFilter);
        List<ManifestEntry> mfEntries = manifestScanner.scan(bundle);

        List<ManifestEntry> collect = mfEntries.stream().filter(entry -> entry.getValue().contains(Verticle.class.getPackage().getName())).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            logger.debug("Ignoring bundle {}, it's already handled by service means", bundle);
            return null;
        }
        
        
        Collection<String> classes = bundleWiring.listResources("/", "*.class", BundleWiring.LISTRESOURCES_RECURSE|BundleWiring.LISTRESOURCES_LOCAL);
        logger.debug("found {} classes", classes.size());
        
        List<String> failedClassNames = new ArrayList<>();
        
        List<Class> verticleAssignables = classes.stream()
            .map(clazzName -> clazzName.replace('/', '.'))
            .map(clazzName -> clazzName.replace(".class", ""))
            .map(clazzName -> {
                try {
                    return bundle.loadClass(clazzName);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    failedClassNames.add(clazzName);
                    //munch ... 
                }
                return null;
            })
            .filter(clazz -> clazz != null)
            .filter(clazz -> Verticle.class.isAssignableFrom(clazz))
            .collect(Collectors.toList());
        
        if (verticleAssignables.isEmpty()) {
            logger.debug("Ignoring bundle {}, no Verticle assignable classes found", bundle);
            logger.debug("following list of class names couldn't be loaded from bundle: {}", failedClassNames);
            return null;
        }
        
        return new VerticleExtenderImpl(bundle, verticleAssignables);
    }

}
