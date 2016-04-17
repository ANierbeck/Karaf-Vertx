package de.nierbeck.example.vertx.http;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

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

    @Activate
    public void activate(ServerConfig cfg) {
        LOGGER.info("Creating vertx HTTP server");
        this.cfg = cfg;
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
    public void start(Future<Void> startFuture) throws Exception {
        LOGGER.info("starting verticle");
        server = getVertx().createHttpServer(new HttpServerOptions().setPort(cfg.port())).requestHandler(req -> {
            req.response().end("Hello from OSGi !");
        });
     
        server.listen(status -> {
            if (status.succeeded()) {
                LOGGER.info("status suceeded");
                startFuture.complete();
                return;
            } else {
                LOGGER.info("status failed");
                startFuture.fail(status.cause());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.close();
        }
    }

}
