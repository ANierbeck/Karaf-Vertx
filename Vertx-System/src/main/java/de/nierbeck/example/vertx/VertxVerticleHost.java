package de.nierbeck.example.vertx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private Map<Verticle, String> deployedVerticles = new HashMap<>();
    
    @Deactivate
    public void stop() {
        LOGGER.info("Stopping Host and remove verticles");
        cleanup();
    }

    private void cleanup() {
        if (vertxService != null) {
            verticles.forEach(verticle -> vertxService.undeploy(verticle.getVertx().getOrCreateContext().deploymentID()));
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
    public void addVerticle(Verticle verticle) {
        LOGGER.info("Deploying verticle " + verticle);
        verticles.add(verticle);
        if (verticle == null)
            return;
        if (vertxService != null)
            vertxService.deployVerticle(verticle, deploy -> {
                if (deploy.succeeded()) {
                    LOGGER.info("Deployment of verticle succeeded");
                    String id = deploy.result();
                    deployedVerticles.put(verticle, id);
                } else {
                    LOGGER.log(Level.SEVERE, "Deployment of verticle failed", deploy.cause());
                }
            });
    }
    
    public void removeVerticle(Verticle verticle) {
        LOGGER.info("Undeploying verticle " + verticle);
        verticles.remove(verticle);
        if (verticle == null)
            return;
        if (vertxService != null) {
            vertxService.undeploy(deployedVerticles.get(verticle));
            deployedVerticles.remove(verticle);
        }
    }
}
