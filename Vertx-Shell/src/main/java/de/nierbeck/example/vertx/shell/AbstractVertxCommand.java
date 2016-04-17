package de.nierbeck.example.vertx.shell;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

public abstract class AbstractVertxCommand implements Action {

    @Reference
    private Vertx vertxService;
    
    @Reference
    private EventBus eventBusService;

    protected Vertx getVertxService() {
        return vertxService;
    }
    
    protected EventBus getEventBusService() {
        return eventBusService;
    }
}
