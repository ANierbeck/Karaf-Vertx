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

import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

@Command(scope = "vertx", name = "local-map-get", description = "Get values from a local map")
@Service
public class VertxMapRm extends AbstractVertxCommand {

    @Argument(index = 0, name = "map", description = "the name of the map to get from", required = true)
    private String map;

    @Argument(index = 1, multiValued = true, description = "the keys to get", name = "keys", required = true)
    private List<String> keys;

    @Override
    public Object execute() throws Exception {
        SharedData sharedData = getVertxService().sharedData();
        LocalMap<Object, Object> map = sharedData.getLocalMap(this.map);

        if (keys != null) {
            keys.forEach(map::remove);
        }
        
        return "removed keys from map";
    }

}
