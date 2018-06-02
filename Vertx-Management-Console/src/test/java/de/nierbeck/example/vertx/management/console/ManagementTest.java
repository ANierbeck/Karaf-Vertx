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

package de.nierbeck.example.vertx.management.console;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.nierbeck.example.vertx.management.console.internal.VerticleMeta;
import de.nierbeck.example.vertx.verticles.VertxHttpClientVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;


@RunWith(VertxUnitRunner.class)
public class ManagementTest {

    @Rule
    public Timeout rule = Timeout.seconds(2);
    private Vertx vertx;
    private int port;
    
    @Before
    public void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();
        
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
        
        Management management = new Management();
        management.bindVertx(vertx);
        Router startWebRoutes = management.startWebRoutes();
        vertx.createHttpServer().requestHandler(startWebRoutes::accept).listen(port, context.asyncAssertSuccess());
    }
    
    @After
    public void tearDown(TestContext context) throws Exception {
        vertx = null;
    }

    @Test
    public void testAccessibleOverview(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/overview/", response -> {
            context.assertEquals(response.statusCode(), 200);
            async.complete();
        });
    }

    @Test
    public void testPayloadEmptyOverview(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/overview/", response -> {
            context.assertEquals(response.statusCode(), 200);
            response.bodyHandler(body -> {
                ArrayList<VerticleMeta> list = Json.decodeValue(body.toString(), (Class<ArrayList<VerticleMeta>>) (Class<?>) ArrayList.class);
                context.assertNotNull(list);
                context.assertTrue(list.isEmpty());
                async.complete();
          });
        });
    }
    
    @Test
    public void testPayloadOverview(TestContext context) throws Exception {

        Async async = context.async();
        
        final List<String> ids = new ArrayList<>();
        
        
        vertx.deployVerticle(VertxHttpClientVerticle.class.getName(), res -> {
            context.assertTrue(res.succeeded());
            ids.add(res.result());
            async.complete();
        });
        
        Thread.sleep(1000);
        
        vertx.createHttpClient().getNow(port, "localhost", "/overview/", response -> {
            context.assertEquals(response.statusCode(), 200);
            response.bodyHandler(body -> {
                ArrayList<VerticleMeta> list = Json.decodeValue(body.toString(), (Class<ArrayList<VerticleMeta>>) (Class<?>) ArrayList.class);
                context.assertNotNull(list);
                context.assertFalse(list.isEmpty());
                context.assertEquals(list.size(), 1);
                context.assertTrue(list.get(0).getIdentifier().equalsIgnoreCase(ids.get(0)));
                async.complete();
          });
        });
    }
}
