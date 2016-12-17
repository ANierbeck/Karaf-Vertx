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
