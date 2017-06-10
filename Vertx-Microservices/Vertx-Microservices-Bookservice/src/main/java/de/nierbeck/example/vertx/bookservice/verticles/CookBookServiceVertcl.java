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

package de.nierbeck.example.vertx.bookservice.verticles;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.nierbeck.example.vertx.entity.Book;
import de.nierbeck.example.vertx.entity.Recipe;
import de.nierbeck.example.vertx.http.Route;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Component(immediate = true, service = Verticle.class)
public class CookBookServiceVertcl extends AbstractVerticle {

    private final static Logger LOGGER = Logger.getLogger(CookBookServiceVertcl.class.getName());

    private EventBus eventBus;

    private BundleContext bc;

    private ServiceRegistration<Route> serviceRegistration;

    @Activate
    public void activate(BundleContext context) {
        LOGGER.info("Creating route to register in HttpServer");
        this.bc = context;
    }

    @Deactivate
    public void deActivate() {
        try {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Caught exception", e);
        }
    }

    @Override
    public void start() throws Exception {
        completeStartup(startWebRoutes());
    }
    
    protected Router startWebRoutes() {
        LOGGER.info("starting rest router");
        Router router = Router.router(getVertx());

        router.route("/*").handler(BodyHandler.create());
        router.get("/").handler(this::handleListBooks);
        router.post("/").handler(this::addBook);
        router.get("/:id").handler(this::receiveCookBook);
        router.get("/:book_id/recipe").handler(this::listRecipes);
        router.post("/:book_id/recipe").handler(this::addRecipe);
        router.get("/:book_id/recipe/:id").handler(this::receiveRecipe);
        router.put("/:book_id/recipe/:id").handler(this::updateRecipe);
        router.delete("/:book_id/recipe/:id").handler(this::deleteRecipe);
        
        return router;
    }
    
    private void completeStartup(Router router) {
        Route route = new Route() {
            @Override
            public Router getRoute() {
                return router;
            }
        };
        
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Route.CONTEXT_PATH, "/cookbook-service");
        serviceRegistration = bc.registerService(Route.class, route, properties);
    }
    
    private void handleListBooks(RoutingContext routingContext) {
        eventBus.send("de.nierbeck.vertx.jdbc.read", new Book(), message -> {
            HttpServerResponse response = routingContext.response();
            if (!message.failed()) {
                @SuppressWarnings("unchecked")
                List<Book> customMessage = (List<Book>) message.result().body();
                LOGGER.log(Level.INFO, "Receiver ->>>>>>>> " + customMessage);
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

    private void addBook(RoutingContext routingContext) {
        Book book = Json.decodeValue(routingContext.getBodyAsString(), Book.class);
        
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(book));
        eventBus.publish("de.nierbeck.vertx.jdbc.write.add", book);
    }
    
    private void addRecipe(RoutingContext routingContext) {
        String bookId = routingContext.request().getParam("book_id");
        Recipe recipe = Json.decodeValue(routingContext.getBodyAsString(), Recipe.class);
        recipe.setBookId(Long.valueOf(bookId));

        HttpServerResponse response = routingContext.response();
        response.setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(recipe));
        eventBus.publish("de.nierbeck.vertx.jdbc.write.add", recipe);
    }
    
    private void updateRecipe(RoutingContext routingContext) {
        String bookId = routingContext.request().getParam("book_id");
        Recipe recipe = Json.decodeValue(routingContext.getBodyAsString(), Recipe.class);
        
        if (recipe.getBookId() != Long.parseLong(bookId)) {
            LOGGER.log(Level.INFO, "something wrong recipe of wrong book id");
        } 
        eventBus.publish("de.nierbeck.vertx.jdbc.write.update", recipe);
    }
    
    private void receiveRecipe(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        String bookId = routingContext.request().getParam("book_id");
        Recipe recipe = new Recipe();
        recipe.setBookId(Long.valueOf(bookId));
        recipe.setId(Long.valueOf(id));


        eventBus.send("de.nierbeck.vertx.jdbc.read", recipe, message -> {
            HttpServerResponse response = routingContext.response();
            if (!message.failed()) {
                Recipe customMessage = (Recipe) message.result().body();
                if (customMessage != null) {
                    response.putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(message.result().body()));
                }
            } else {
                LOGGER.log(Level.SEVERE, "message failed to retrieve recipe");
            }
            response.close();
        });
    }
    
    private void listRecipes(RoutingContext routingContext) {
        String bookId = routingContext.request().getParam("book_id");
        
        eventBus.send("de.nierbeck.vertx.jdbc.read", new Recipe(null, null, null, Long.valueOf(bookId)), message -> {
            HttpServerResponse response = routingContext.response();
            if (!message.failed()) {
                @SuppressWarnings("unchecked")
                List<Recipe> customMessage = (List<Recipe>) message.result().body();
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

    private void deleteRecipe(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        String bookId = routingContext.request().getParam("book_id");
        Recipe recipe = new Recipe();
        recipe.setBookId(Long.valueOf(bookId));
        recipe.setId(Long.valueOf(id));
        
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(202)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(recipe));
        
        eventBus.send("de.nierbeck.vertx.jdbc.delete", recipe);
    }
    
    private void receiveCookBook(RoutingContext routingContext) {

        String id = routingContext.request().getParam("id");

        Book book = new Book();
        book.setId(Long.valueOf(id));

        eventBus.send("de.nierbeck.vertx.jdbc.read", book, message -> {
            HttpServerResponse response = routingContext.response();
            if (!message.failed()) {
                Book customMessage = (Book) message.result().body();
                LOGGER.log(Level.INFO,"Receiver ->>>>>>>> " + customMessage);
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
    
    @Reference(unbind="unbindEventBus")
    public void bindEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    private void unbindEventBus(EventBus eventBus) {
        this.eventBus = null;
    }
    
}
