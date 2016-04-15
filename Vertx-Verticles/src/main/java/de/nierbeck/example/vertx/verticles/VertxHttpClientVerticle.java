package de.nierbeck.example.vertx.verticles;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;

@Component(service=Verticle.class)
public class VertxHttpClientVerticle extends AbstractVerticle {

    private final static Logger LOGGER = Logger.getLogger("VertxHttpClientVerticle");

    @Override
    public void start() throws Exception {
        LOGGER.info("starting VertxHttpClientVerticle");
        getVertx().createHttpClient().getNow("perdu.com", "/", response -> {
            response.bodyHandler(buffer -> LOGGER.info(buffer.toString("UTF-8")));
        });
    }
}
