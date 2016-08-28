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
