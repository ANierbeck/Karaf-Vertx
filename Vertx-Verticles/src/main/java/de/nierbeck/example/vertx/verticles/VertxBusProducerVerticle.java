package de.nierbeck.example.vertx.verticles;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.MessageProducer;

@Component(immediate=true, service=Verticle.class)
public class VertxBusProducerVerticle extends AbstractVerticle{

    private final static Logger LOGGER = Logger.getLogger("VertxBusProducerVerticle");
    
    @Override
    public void start() throws Exception {
        LOGGER.info("starting VertxBusProducerVerticle");
        MessageProducer<Object> publisher = getVertx().eventBus().publisher("localhost");
        
        int count = 0;
        while(true) {
            publisher.send("Message "+count++);
            Thread.sleep(2000);
        }
        
    }
}
