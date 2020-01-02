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

import java.util.ArrayList;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.nierbeck.example.vertx.encoder.BookEncoder;
import de.nierbeck.example.vertx.encoder.ListOfBookEncoder;
import de.nierbeck.example.vertx.encoder.RecipeEncoder;
import de.nierbeck.example.vertx.entity.Book;
import de.nierbeck.example.vertx.entity.Recipe;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;

@RunWith(VertxUnitRunner.class)
public class CookBookServiceVertclTest {
    
    private final static Logger LOGGER = Logger.getLogger(CookBookServiceVertcl.class.getName());

    @Rule
    public Timeout rule = Timeout.seconds(2);
    private Vertx vertx;
    private WebClient client;
    
    @Before
    public void setUp(final TestContext context) throws Exception {

        vertx = Vertx.vertx();

        vertx.eventBus().registerDefaultCodec(Book.class, new BookEncoder());
        vertx.eventBus().registerDefaultCodec(Recipe.class, new RecipeEncoder());
        vertx.eventBus().registerDefaultCodec((Class<ArrayList<Book>>) (Class<?>) ArrayList.class,
                new ListOfBookEncoder());

        LOGGER.info("setup of eventbus ...");
        vertx.eventBus().consumer("de.nierbeck.vertx.jdbc.read").handler(message -> {
            LOGGER.info("reply a book");
            final Book book = new Book(1l, "test", "me");
            final ArrayList<Book> arrayList = new ArrayList<>();
            arrayList.add(book);
            message.reply(arrayList);
        });

        LOGGER.info("deploying verticle ...");
        final CookBookServiceVertcl cookBookServiceVertcl = new CookBookServiceVertcl();
        cookBookServiceVertcl.bindEventBus(vertx.eventBus());
        final Router startWebRoutes = cookBookServiceVertcl.startWebRoutes();

        vertx.createHttpServer().requestHandler(startWebRoutes).listen(8080, context.asyncAssertSuccess());

        client = WebClient.create(vertx);

        LOGGER.info("deploying done ...");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test(final TestContext context) {
        LOGGER.info("testing ...");
        final Async async = context.async();
        client.get(8080, "localhost", "/").send(ar -> {
            context.assertTrue(ar.succeeded());
            final var response = ar.result();
            context.assertEquals(response.statusCode(), 200);
            async.complete();
        });
    }

    
    
}
