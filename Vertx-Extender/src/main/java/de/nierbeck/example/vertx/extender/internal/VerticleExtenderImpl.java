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
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Verticle;

public class VerticleExtenderImpl {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Bundle verticleBundle;
    private List<ServiceRegistration<Verticle>> verticleServices;
    private BundleContext verticleBundleContext;

    private List<Class> verticles;

    public VerticleExtenderImpl(Bundle bundle, List<Class> verticles) {
        this.verticleBundle = bundle;
        verticleBundleContext = bundle.getBundleContext();
        verticleServices = new ArrayList<>();
        this.verticles = verticles;
    }

    public void start() {
        logger.debug("VerticleExtender started for bundle {}", verticleBundle);
        logger.debug("found {} verticles", verticles.size());
        
        List<Verticle> verticleInstances = verticles.stream().map(verticleClass -> {
            Verticle verticle = null;
            try {
                verticle = (Verticle) verticleClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                // munch
            }
            return verticle;
        }).collect(Collectors.toList());
        
        logger.debug("created {} Verticle instances", verticleInstances.size());
        
        verticleServices = verticleInstances.stream()
            .map(verticle -> verticleBundleContext.registerService(Verticle.class, verticle, null))
            .collect(Collectors.toList());
        
        logger.info("Registered {} Verticle services", verticleServices.size());
    }

    public void destroy() {
        logger.debug("destroying extender for bundle {}", verticleBundle);
        verticleServices.stream().forEach(registration -> registration.unregister());
    }

}
