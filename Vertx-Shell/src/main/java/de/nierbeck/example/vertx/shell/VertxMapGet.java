package de.nierbeck.example.vertx.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

@Command(scope = "vertx", name = "local-map-get", description = "Get values from a local map")
@Service
public class VertxMapGet extends AbstractVertxCommand {

    @Argument(index = 0, name = "map", description = "the name of the map to get from", required = true)
    private String map;

    @Argument(index = 1, multiValued = true, description = "the keys to get", name = "keys", required = true)
    private List<String> keys;

    @Override
    public Object execute() throws Exception {
        SharedData sharedData = getVertxService().sharedData();
        LocalMap<Object, Object> map = sharedData.getLocalMap(this.map);

        ShellTable table = new ShellTable();

        table.column("Map[" + this.map + "]\nKey");
        table.column("\nValue");

        if (keys != null) {
            for (String key : keys) {
                Object value = map.get(key);
                Row row = table.addRow();
                row.addContent(key, value);
            }
        }
        
        table.print(System.out);

        return null;
    }

}
