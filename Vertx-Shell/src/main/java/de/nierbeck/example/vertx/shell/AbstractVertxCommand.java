package de.nierbeck.example.vertx.shell;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import io.vertx.core.Vertx;

public abstract class AbstractVertxCommand implements Action {

    @Reference
    private Vertx vertxService;

    protected Vertx getVertxService() {
        return vertxService;
    }
    
}
