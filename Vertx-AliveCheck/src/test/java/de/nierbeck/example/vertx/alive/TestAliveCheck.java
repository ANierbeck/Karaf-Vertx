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
package de.nierbeck.example.vertx.alive;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;

@RunWith(VertxUnitRunner.class)
public class TestAliveCheck {

    private final static Logger LOGGER = Logger.getLogger(TestAliveCheck.class.getName());

    @Rule
    public Timeout rule = Timeout.seconds(2);
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) throws IOException {
        LOGGER.info("Starting Vertx");
        vertx = Vertx.vertx();
        
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {

            // This handler will be called for every request
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");

            // Write to the response and end it
            response.end("Hello World from Vert.x-Web!");
        });

        HealthCheckHandler pingHandler = HealthCheckHandler.create(vertx);

        pingHandler.register("my-procedure", future -> future.complete(Status.OK()));
        
        router.route("/ping").handler(pingHandler);
        
        vertx.createHttpServer().requestHandler(router::accept).listen(8080, context.asyncAssertSuccess());
        
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void checkStatus(TestContext context) {
        LOGGER.info("testing ... ");
        given().port(8080).baseUri("http://localhost").when().get("/").then().assertThat().statusCode(200);
    }

    @Test
    public void checkThatTheIndexPageIsServed(TestContext context) {
        LOGGER.info("testing ... ");
        Async async = context.async();
        vertx.createHttpClient().getNow(8080, "localhost", "/", response -> {
            LOGGER.info("received response ... ");
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals(response.headers().get("content-type"), "text/plain");
            response.bodyHandler(body -> {
                context.assertTrue(body.toString().contains("Hello World from Vert.x-Web!"));
                async.complete();
            });
        });
    }
    
    @Test
    public void testPing(TestContext context) {
        LOGGER.info("testing ... ");
        Async async = context.async();
        vertx.createHttpClient().getNow(8080, "localhost", "/ping", response -> {
            LOGGER.info("received response ... ");
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals(response.headers().get("content-type"), "application/json;charset=UTF-8");
            response.bodyHandler(body -> {
                context.assertTrue(body.toString().contains("{\"checks\":[{\"id\":\"my-procedure\",\"status\":\"UP\"}],\"outcome\":\"UP\"}"));
                async.complete();
            });
        });
    }

}
