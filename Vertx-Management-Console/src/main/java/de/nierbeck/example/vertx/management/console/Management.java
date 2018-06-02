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

package de.nierbeck.example.vertx.management.console;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.nierbeck.example.vertx.management.console.internal.MetricsMeta;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.nierbeck.example.vertx.http.Route;
import de.nierbeck.example.vertx.management.console.internal.VerticleMeta;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

@Component(immediate=true, service=Verticle.class)
public class Management extends AbstractVerticle {
    
    private final static Logger LOGGER = Logger.getLogger(Management.class.getName());
    
    private MetricsService metricsService;
    
    private Vertx vertx;

    private BundleContext bc;

    private ServiceRegistration<Route> serviceRegistration;
    
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
        completeStartup(startWebRoutes());
    }
    
    private void completeStartup(Router router) {
        Route route = new Route() {
            @Override
            public Router getRoute() {
                return router;
            }
        };
        
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Route.CONTEXT_PATH, "/managment-service");
        serviceRegistration = bc.registerService(Route.class, route, properties);
    }
    
    protected final Router startWebRoutes() {
        LOGGER.info("starting rest router");
        Router router = Router.router(vertx);
        BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress("metrics"));

        router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));
        router.route("/overview/").handler(this::overview);
        router.route("/metrics/").handler(this::metrics);
        router.get("/metrics/:id").handler(this::receiveMetric);

        // Serve the static resources
        router.route("/*").handler(StaticHandler.create("webroot", this.getClass().getClassLoader()));

        vertx.setPeriodic(1000, t -> {
            JsonObject metrics = metricsService.getMetricsSnapshot(vertx.eventBus());
            vertx.eventBus().publish("metrics", metrics);
        });
        
        return router;
    }

    private void overview(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        List<VerticleMeta> list = vertx.deploymentIDs().stream().map((id) -> { 
            Deployment deployment = ((VertxInternal)vertx).getDeployment(id);
            VerticleMeta vm =  new VerticleMeta(id, deployment.verticleIdentifier(), deployment.deploymentOptions().toJson());
            return vm;
        }).collect(Collectors.toList());
        response.putHeader("content-type", "application/json; charset=utf-8").end(Json.encodePrettily(list));
    }
    
    private void metrics(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        List<MetricsMeta> metricsNames = metricsService.metricsNames().stream().map((name) -> {
            MetricsMeta meta = new MetricsMeta(name, null);
            return meta;
        }).collect(Collectors.toList());
        response.putHeader("content-type", "application/json; charset=utf-8").end(Json.encodePrettily(metricsNames));
    }

    private void receiveMetric(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        String metricsId = routingContext.request().getParam("id");
        JsonObject metricsSnapshot = metricsService.getMetricsSnapshot(metricsId).getJsonObject(metricsId);
        String type = metricsSnapshot.getString("type");
        if (type.equalsIgnoreCase("histogram")||type.equalsIgnoreCase("timer")) {
            metricsSnapshot.put("percentile75", metricsSnapshot.getValue("75%"));
            metricsSnapshot.put("percentile95", metricsSnapshot.getValue("95%"));
            metricsSnapshot.put("percentile98", metricsSnapshot.getValue("98%"));
            metricsSnapshot.put("percentile99", metricsSnapshot.getValue("99%"));
            metricsSnapshot.put("percentile999", metricsSnapshot.getValue("99.9%"));
        }
        MetricsMeta meta = new MetricsMeta(metricsId, metricsSnapshot);
        response.putHeader("content-type", "application/json; charset=utf-8").end(Json.encodePrettily(meta));
    }
    
    @Reference
    public void bindVertx(Vertx vertx) {
        this.vertx = vertx;
    }
    
    public void unbindVertx(Vertx vertx) {
        this.vertx = null;
    }
    
    @Reference
    public void bindMetricsService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    public void unbindMetricsService() {
        this.metricsService = null;
    }
}
