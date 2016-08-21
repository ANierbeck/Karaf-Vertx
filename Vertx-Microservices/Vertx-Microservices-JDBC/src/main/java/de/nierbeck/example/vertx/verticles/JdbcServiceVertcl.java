package de.nierbeck.example.vertx.verticles;

import java.util.logging.Logger;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.jdbc.JDBCClient;

@Component(immediate=true, service=Verticle.class)
public class JdbcServiceVertcl extends AbstractVerticle {

    private final static Logger LOGGER = Logger.getLogger("JdbcServiceVertcl");
    
    @Reference
    private DataSource dataSource;
    
    @Reference
    private EventBus eventBus;
    
    private JDBCClient client;
    
    @Override
    public void start() throws Exception {
        super.start();
        client = JDBCClient.create(vertx, dataSource);
        MessageConsumer<Object> read = eventBus.consumer("de.nierbeck.vertx.jdbc.read");
        MessageConsumer<Object> write = eventBus.consumer("de.nierbeck.vertx.jdbc.write");
        read.handler(message -> {
            LOGGER.info("received read message: "+ message);
            message.reply(message.body());
        });
        write.handler(message -> {
            LOGGER.info("received write message: "+ message);
            message.reply(message.body());
        });
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        client.close();
    }
    
}
