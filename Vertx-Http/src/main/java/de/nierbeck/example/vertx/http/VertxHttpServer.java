package de.nierbeck.example.vertx.http;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

@ObjectClassDefinition(name = "Server Configuration")
@interface ServerConfig {
  int port() default 8080;
}

@Component(immediate = true)
@Designate(ocd = ServerConfig.class)
public class VertxHttpServer {
    
    private final static Logger LOGGER = Logger.getLogger("VertxHttpServer");

    @Reference
    private Vertx vertx;

    private HttpServer server;

    @Activate
    public void activate(ServerConfig cfg) {
        LOGGER.info("Creating vertx HTTP server");
        server = vertx.createHttpServer().requestHandler((r) -> {
            r.response().end("Hello from OSGi !");
        }).listen(cfg.port());
    }

    @Deactivate
    public void deActivate() {
        if (server != null) {
            server.close();
        }
    }

}
