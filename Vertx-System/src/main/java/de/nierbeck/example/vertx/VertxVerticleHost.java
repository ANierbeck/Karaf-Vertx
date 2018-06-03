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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.DeploymentOptions;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

@Component(immediate = true, service = {})
public class VertxVerticleHost {
    
    private final static Logger LOGGER = Logger.getLogger("VertxVerticleHost");

    private Vertx vertxService;
    
    private List<Verticle> verticles = new ArrayList<>();
    
    private Map<Verticle, String> deployedVerticles = new ConcurrentHashMap<>();

    @Reference
    private ConfigurationAdmin ca;
    
    @Deactivate
    public void stop() {
        LOGGER.info("Stopping Host and remove verticles");
        cleanup();
    }

    private void cleanup() {
        if (vertxService != null) {
            verticles.forEach(verticle -> {
                String deploymentID = verticle.getVertx().getOrCreateContext().deploymentID();
                if (deploymentID != null)
                    vertxService.undeploy(deploymentID);
                });
        }
    }
    
    @Reference(unbind = "unsetVertxService", policy=ReferencePolicy.STATIC)
    public void setVertxService(Vertx vertxService) {
        LOGGER.info("Injecting Vertx service");
        this.vertxService = vertxService;
        if (!verticles.isEmpty()) {
            LOGGER.info("register already found verticles");
            verticles.forEach(verticle -> this.vertxService.deployVerticle(verticle));
        }
    }
    
    public void unsetVertxService(Vertx vertxService) {
        cleanup();
        this.vertxService = null;
    }
     
    @Reference(unbind = "removeVerticle", policy=ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void addVerticle(Verticle verticle) throws IOException {
        LOGGER.info("Deploying verticle " + verticle);
        verticles.add(verticle);
        if (verticle == null)
            return;

        //DeploymentOptions options = createOrRetrieveDeploymentOptions(verticle);

        if (vertxService != null)
            vertxService.deployVerticle(verticle, /*options, */deploy -> {
                if (deploy.succeeded()) {
                    LOGGER.info("Deployment of verticle succeeded");
                    String id = deploy.result();
                    deployedVerticles.put(verticle, id);
                } else {
                    LOGGER.log(Level.SEVERE, "Deployment of verticle failed", deploy.cause());
                }
            });
    }

    private DeploymentOptions createOrRetrieveDeploymentOptions(Verticle verticle) throws IOException {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        String className = verticle.getClass().getCanonicalName();
        Configuration configuration = null;
        configuration = ca.getConfiguration(className);
        Dictionary<String, Object> properties = configuration.getProperties();
        if (properties.isEmpty()) {
            properties.put("ha", deploymentOptions.isHa());
            properties.put("worker", deploymentOptions.isWorker());
            properties.put("multiThreaded", deploymentOptions.isMultiThreaded());
        } else {
            deploymentOptions.setHa((Boolean) (properties.get("ha")));
            deploymentOptions.setWorker((Boolean) properties.get("worker"));
            deploymentOptions.setMultiThreaded((Boolean) properties.get("multiThreaded"));
        }
        return deploymentOptions;
    }

    public void removeVerticle(Verticle verticle) {
        LOGGER.info("Undeploying verticle " + verticle);
        verticles.remove(verticle);
        if (verticle == null)
            return;
        if (vertxService != null && deployedVerticles.get(verticle) != null) {
            String verticleId = deployedVerticles.get(verticle);
            if (vertxService.deploymentIDs().contains(verticleId)) {
                vertxService.undeploy(verticleId);
            }
            deployedVerticles.remove(verticle);
        }
    }
}
