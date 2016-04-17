package de.nierbeck.example.vertx.verticles;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.MessageProducer;

@Component(immediate=true, service=Verticle.class)
public class VertxBusProducerVerticle extends AbstractVerticle{

    private final static Logger LOGGER = Logger.getLogger("VertxBusProducerVerticle");
    private long timerId;
    
    @Override
    public void start() throws Exception {
        LOGGER.info("starting VertxBusProducerVerticle");
        MessageProducer<Object> publisher = getVertx().eventBus().publisher("localhost");
        
        timerId = getVertx().setPeriodic(2000, new Handler<Long>() {
            int count = 0;
            
            @Override
            public void handle(Long event) {
                publisher.send("Message "+count++);
            }
        });
    }
    
    @Override
    public void stop() throws Exception {
        getVertx().cancelTimer(timerId);
    }
}
