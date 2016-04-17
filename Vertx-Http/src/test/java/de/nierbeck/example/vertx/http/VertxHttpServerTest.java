package de.nierbeck.example.vertx.http;

import java.lang.annotation.Annotation;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.test.core.VertxTestBase;

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
