package de.nierbeck.example.vertx.shell;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;

@Command(scope = "vertx", name = "bus-send", description = "Send a message to the event bus")
@Service
public class VertxBusSend extends AbstractVertxCommand {

    @Argument(index = 0, required = true)
    private String address;

    @Argument(index = 1, required = true)
    private String body;

    @Option(name = "timout", description = "the send timeout")
    private long timeout;

    @Option(name = "reply", description = "wait for a reply and print it on the console")
    private boolean reply;

    @Option(name = "verbose")
    private boolean verbose;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public Object execute() throws Exception {
        if (reply) {
            getEventBusService().send(address, body, new DeliveryOptions(), ar -> {
                if (ar.succeeded()) {
                    Message<Object> reply = ar.result();
                    if (verbose) {
                        System.out.println("Reply address: " + reply.replyAddress() + "\n");
                        MultiMap headers = reply.headers();
                        for (String header : headers.names()) {
                            System.out.println("Reply header " + header + ":" + headers.getAll(header) + "\n");
                        }
                    }
                    System.out.println("Reply: <");
                    System.out.println(reply.body().toString() + ">\n");
                } else {
                    System.out.println("Error: " + ar.cause().getMessage() + "\n");
                }
            });
        } else {
            getEventBusService().send(address, body, new DeliveryOptions());
        }
        return null;
    }

}
