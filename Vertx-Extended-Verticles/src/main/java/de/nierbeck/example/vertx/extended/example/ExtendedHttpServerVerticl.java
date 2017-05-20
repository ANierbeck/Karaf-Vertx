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
package de.nierbeck.example.vertx.extended.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class ExtendedHttpServerVerticl extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {
      vertx
          .createHttpServer()
          .requestHandler(r -> {
            r.response().end("<h1>Hello from my first " +
                "Vert.x 3 application</h1>");
          })
          .listen(9090, result -> {
            if (result.succeeded()) {
              fut.complete();
            } else {
              fut.fail(result.cause());
            }
          });
    }
}
