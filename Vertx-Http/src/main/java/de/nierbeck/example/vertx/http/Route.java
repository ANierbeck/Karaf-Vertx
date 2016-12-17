package de.nierbeck.example.vertx.http;

import io.vertx.ext.web.Router;

/**
 * This is a Marker Interface for a Route Verticle to be collected by the HttpService. 
 * It is needed to define a "sub-route" for the HttpService
 *
 */
public interface Route {

    static final String CONTEXT_PATH = "ContextPath";
    
    Router getRoute();
    
}
