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
package de.nierbeck.example.vertx.http;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

@ObjectClassDefinition(name = "Server Configuration")
@interface ServerConfig {
    int port() default 8080;
}

@Component(immediate = true, service = Verticle.class)
@Designate(ocd = ServerConfig.class)
public class VertxHttpServer extends AbstractVerticle {

    private final static Logger LOGGER = Logger.getLogger("VertxHttpServer");

    private HttpServer server;

    private ServerConfig cfg;

    private Map<String, Route> routes;

    private Router router;

    @Activate
    public void activate(ServerConfig cfg) {
        LOGGER.info("Creating vertx HTTP server");
        this.cfg = cfg;
        routes = new HashMap<>();
    }

    @Deactivate
    public void deActivate() {
        try {
            stop();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Caught exception", e);
        }
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("starting rest router");
        server = getVertx().createHttpServer();
        router = Router.router(getVertx());
        server.requestHandler(router::accept).listen(cfg.port());

        if (!routes.isEmpty()) {
            update();
        }
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    synchronized private void update() {
        if (routes.isEmpty() || router == null)
            return;
        routes.entrySet().forEach(entry -> {
            LOGGER.info("adding routes for subroute: " + entry.getKey());
            router.mountSubRouter(entry.getKey(), entry.getValue().getRoute());
        });
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeRoute")
    public void addRoute(Route routeToAdd, Map<String, Object> properties) {
        this.routes.put((String) properties.get(Route.CONTEXT_PATH), routeToAdd);
        update();
    }

    public void removeRoute(Route routToRemove, Map<String, Object> properties) {
        String key = (String) properties.get("ContextPath");
        this.routes.remove(key);
        router.clear();
        update();
    }

}
