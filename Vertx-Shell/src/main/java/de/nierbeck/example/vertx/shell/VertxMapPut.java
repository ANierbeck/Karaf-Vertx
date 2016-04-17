package de.nierbeck.example.vertx.shell;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

@Command(scope = "vertx", name = "local-map-put", description = "Put key/value in a local map")
@Service
public class VertxMapPut extends AbstractVertxCommand {

    @Argument(index = 0, name = "map", description = "the local shared map name", required = true)
    private String map;

    @Argument(index = 1, multiValued = false, description = "the key to put", name = "key", required = true)
    private String key;
    
    @Argument(index = 2, multiValued = false, description = "the value to put", name = "value", required = true)
    private String value; 

    @Override
    public Object execute() throws Exception {
        SharedData sharedData = getVertxService().sharedData();
        LocalMap<Object, Object> localMap = sharedData.getLocalMap(map);
        localMap.put(key, value);

        return "key and value added to map";
    }

}
