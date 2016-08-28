package de.nierbeck.example.vertx.shell;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;

import com.codahale.metrics.MetricRegistry;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;

@Command(scope = "vertx", name = "metrics", description = "shows metrics for the given details")
@Service
public class VertxMetrics extends AbstractVertxCommand {

    @Reference
    private MetricRegistry metricsRegistry;

    @Argument
    private String metricsBaseName;

    @Override
    public Object execute() throws Exception {

        MetricsService metricsService = MetricsService.create(getVertxService());

        JsonObject metrics = (metricsBaseName != null) ? metricsService.getMetricsSnapshot(metricsBaseName)
                : metricsService.getMetricsSnapshot(getVertxService());

        ShellTable table = new ShellTable();

        table.noHeaders().column(new Col("key")).column(new Col("value")).emptyTableText("nothing found");
        
        metrics.forEach(mapEntry -> {
            if (mapEntry.getValue() instanceof String)
                table.addRow().addContent(mapEntry.getKey(), mapEntry.getValue());
            else {
                JsonObject subMap = (JsonObject) mapEntry.getValue();
                subMap.forEach(subMapEntry -> {
                    table.addRow().addContent(mapEntry.getKey()+":"+subMapEntry.getKey(), subMapEntry.getValue());
                });
            }
        });

        try {
            table.print(System.out);
        } catch (Throwable t) {
            System.err.println("FAILED to write table");
        }

        return null;
    }

}
