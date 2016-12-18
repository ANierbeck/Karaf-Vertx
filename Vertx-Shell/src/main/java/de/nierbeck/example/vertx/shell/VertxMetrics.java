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
package de.nierbeck.example.vertx.shell;

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
    
    @Reference
    private MetricsService metricsService;

    @Argument
    private String metricsBaseName;

    @Override
    public Object execute() throws Exception {

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
