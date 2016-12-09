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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import de.nierbeck.example.vertx.encoder.BookEncoder;
import de.nierbeck.example.vertx.encoder.RecipeEncoder;
import de.nierbeck.example.vertx.entity.Book;
import de.nierbeck.example.vertx.entity.Recipe;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

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
        eventBus.registerDefaultCodec(Recipe.class, new RecipeEncoder());

        // Create a router object.
        Router router = Router.router(getVertx());

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 OSGi application</h1>");
        });

        
        router.route("/cookbook*").handler(BodyHandler.create());
        router.get("/cookbook/:id").handler(this::receiveCookBook);
        router.get("/cookbook/:book_id/recipe/:id").handler(this::receiveRecipe);
        router.post("/cookbook/:book_id/recipe").handler(this::addRecipe);
        router.get("/cookbook").handler(this::handleListBooks);
//        router.post("/cookbook/:book_id/recipe/:id").handler(this::updateRecipe);
        
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
    
    private void handleListBooks(RoutingContext routingContext) {
        eventBus.send("de.nierbeck.vertx.jdbc.read", new Book(), message -> {
            HttpServerResponse response = routingContext.response();
            if (!message.failed()) {
                @SuppressWarnings("unchecked")
                List<Book> customMessage = (List<Book>) message.result().body();
                System.out.println("Receiver ->>>>>>>> " + customMessage);
                if (customMessage != null) {
                    response.putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(customMessage));
                }
            } else {
                LOGGER.log(Level.SEVERE, "message failed");
            }
            response.closed();
        });
    }

    private void addRecipe(RoutingContext routingContext) {
        String bookId = routingContext.request().getParam("book_id");
        Recipe recipe = Json.decodeValue(routingContext.getBodyAsString(), Recipe.class);
        recipe.setBookId(Long.valueOf(bookId));

        HttpServerResponse response = routingContext.response();
        response.setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(recipe));
        eventBus.publish("de.nierbeck.vertx.jdbc.write.add.recipe", recipe);
    }
    
    private void receiveRecipe(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        String bookId = routingContext.request().getParam("book_id");
        Recipe recipe = new Recipe();
        recipe.setBookId(Long.valueOf(bookId));
        recipe.setId(Long.valueOf(id));


        eventBus.send("de.nierbeck.vertx.jdbc.read", recipe, message -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(message.result().body()));
            response.close();
        });
    }

    private void receiveCookBook(RoutingContext routingContext) {

        String id = routingContext.request().getParam("id");

        Book book = new Book();
        book.setId(Long.valueOf(id));

        eventBus.send("de.nierbeck.vertx.jdbc.read", book, message -> {
            HttpServerResponse response = routingContext.response();
            if (!message.failed()) {
                Book customMessage = (Book) message.result().body();
                System.out.println("Receiver ->>>>>>>> " + customMessage);
                if (customMessage != null) {
                    response.putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(customMessage));
                }
            } else {
                LOGGER.log(Level.SEVERE, "message failed");
            }
            response.closed();

        });
    }
}
