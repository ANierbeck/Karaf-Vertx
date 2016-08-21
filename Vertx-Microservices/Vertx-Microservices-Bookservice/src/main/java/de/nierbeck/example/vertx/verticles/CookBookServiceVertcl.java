package de.nierbeck.example.vertx.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import de.nierbeck.example.vertx.encoder.BookEncoder;
import de.nierbeck.example.vertx.entity.Book;
import de.nierbeck.example.vertx.entity.Recipe;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ObjectClassDefinition(name = "Server Configuration")
@interface ServerConfig {
    int port() default 8000;
}

@Component(immediate = true, service = Verticle.class)
@Designate(ocd = ServerConfig.class)
public class CookBookServiceVertcl extends AbstractVerticle {

    private final static Logger LOGGER = Logger.getLogger(CookBookServiceVertcl.class.getName());

    private ServerConfig cfg;

    private HttpServer server;
    
    @Reference
    private EventBus eventBus;

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
    public void start(Future<Void> future) throws Exception {
        LOGGER.info("starting rest router");
        
        eventBus.registerDefaultCodec(Book.class, new BookEncoder());

        // Create a router object.
        Router router = Router.router(getVertx());

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 OSGi application</h1>");
        });
        
        router.post("/cookbook/:id/recipe").handler(this::publishRecipeToEventBus);
        
        router.get("/cookbook/:id").handler(this::receiveCookBook);
        

        getVertx().createHttpServer().requestHandler(router::accept).listen(cfg.port(), result -> {
            if (result.succeeded()) {
                future.complete();
            } else {
                future.fail(result.cause());
            }
        });

    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.close();
        }
    }
    
    private void publishRecipeToEventBus(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        Recipe recipe = Json.decodeValue(routingContext.getBodyAsString(), Recipe.class);
        
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(recipe));

        eventBus.publish("de.nierbeck.vertx.jdbc.write", recipe);
    }
    
    private void receiveCookBook(RoutingContext routingContext) {
        
        String id = routingContext.request().getParam("id");
        
        Book book = new Book();
        book.setId(Long.valueOf(id));
        
        eventBus.send("de.nierbeck.vertx.jdbc.read", book, message -> {
            Book customMessage = (Book) message.result().body();
            HttpServerResponse response = routingContext.response();
            System.out.println("Receiver ->>>>>>>> " + customMessage);
            if (customMessage != null) {
                response.putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(customMessage));
            }
            response.closed();

        });
    }
}
