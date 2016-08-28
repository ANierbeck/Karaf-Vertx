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
package de.nierbeck.example.vertx.encoder;

import de.nierbeck.example.vertx.entity.Recipe;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

public class RecipeEncoder implements MessageCodec<Recipe, Recipe>{

    @Override
    public void encodeToWire(Buffer buffer, Recipe recipe) {
     // Easiest ways is using JSON object
        String json = Json.encode(recipe);

        // Length of JSON: is NOT characters count
        int length = json.getBytes().length;

        // Write data into given buffer
        buffer.appendInt(length);
        buffer.appendString(json);
    }

    @Override
    public Recipe decodeFromWire(int pos, Buffer buffer) {
        // My custom message starting from this *position* of buffer
        int _pos = pos;

        // Length of JSON
        int length = buffer.getInt(_pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        String jsonStr = buffer.getString(_pos+=4, _pos+=length);
        
        return Json.decodeValue(jsonStr, Recipe.class);
        
    }

    @Override
    public Recipe transform(Recipe recipe) {
        return recipe;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

}
