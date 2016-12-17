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
package de.nierbeck.example.vertx.http;

import java.lang.annotation.Annotation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.test.core.VertxTestBase;

@Ignore
public class VertxHttpServerTest extends VertxTestBase {
    @Before
    public void setUpTest() {
        ServerConfig cfg = new ServerConfig() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ServerConfig.class;
            }
            @Override
            public int port() {
                // TODO Auto-generated method stub
                return 8080;
            }
        };
        
        VertxHttpServer httpServer = new VertxHttpServer();
        httpServer.activate(cfg);
        vertx.deployVerticle(httpServer);
        waitUntil(() -> vertx.deploymentIDs().size() == 1);
    }

    @Test
    public void testGetHttpResponse() {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions());
        httpClient.request(HttpMethod.GET, 8080, "localhost", "/", response -> {
            response.bodyHandler(body -> {
                assertEquals("Hello from OSGi !", body.getString(0, body.length()));
                testComplete();
            });
        }).end();
        await();
    }
}
