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
package de.nierbeck.example.vertx.alive;

import static de.nierbeck.example.vertx.TcclSwitch.executeWithTCCLSwitch;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.nierbeck.example.vertx.http.Route;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;

@Component(immediate = true, service = Verticle.class)
public class AliveCheck extends AbstractVerticle {
    private final static Logger LOGGER = Logger.getLogger(AliveCheck.class.getName());
    private BundleContext bc;
    private ServiceRegistration<?> serviceRegistration;

    @Reference
    private MetricsService metricsService;
    private Router router;

    @Activate
    public void activate(BundleContext context) {
        LOGGER.info("Creating route to register in HttpServer");
        this.bc = context;
    }

    @Deactivate
    public void deActivate() {
        try {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Caught exception", e);
        }
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("starting health check router");
        router = Router.router(getVertx());

        HealthCheckHandler pingHandler = executeWithTCCLSwitch(() ->HealthCheckHandler.create(getVertx()));
        HealthCheckHandler hc = executeWithTCCLSwitch( () -> HealthCheckHandler.create(getVertx()));

        pingHandler.register("ping-handler", future -> future.complete(Status.OK()));

        hc.register("metrics-handler",
                future -> future.complete(Status.OK(metricsService.getMetricsSnapshot(getVertx()))));

//        router.route("/*");
        // Populate the router with routes...
        // Register the health check handler
        router.get("/health/status").handler(hc);
        // Or
        router.get("/health/ping").handler(pingHandler);

        Route route = new Route() {
            @Override
            public Router getRoute() {
                return router;
            }
        };

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Route.CONTEXT_PATH, "/");
        serviceRegistration = bc.registerService(Route.class, route, properties);
    }
}
