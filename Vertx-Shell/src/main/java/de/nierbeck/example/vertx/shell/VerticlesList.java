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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;

import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;

@Command(scope = "verticles", name = "list", description = "Lists deployed Verticles")
@Service
public class VerticlesList extends AbstractVertxCommand {

    @Override
    public Object execute() throws Exception {
        
        ShellTable table = new ShellTable();
        
        table.column("ID");
        table.column("Identifier");
        table.column("Options");
        
        getVertxService().deploymentIDs().forEach(id -> { 
                Deployment deployment = ((VertxInternal)getVertxService()).getDeployment(id);
                Row row = table.addRow();
                row.addContent(id, deployment.verticleIdentifier(), deployment.deploymentOptions().toJson());
            }
        );
        
        try {
            table.print(System.out);
        } catch (Throwable t)  {
            System.err.println("FAILED to write table");
        }
        
        return null;
    }

}
