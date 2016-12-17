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

import java.util.ArrayList;

import com.fasterxml.jackson.core.type.TypeReference;

import de.nierbeck.example.vertx.entity.Book;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class ListOfBookEncoder implements MessageCodec<ArrayList<Book>, ArrayList<Book>> {

    @Override
    public void encodeToWire(Buffer buffer, ArrayList<Book> listOfBooks) {
        String json = Json.encode(listOfBooks);
        
        int length = json.getBytes().length;
        
        buffer.appendInt(length);
        buffer.appendString(json);
        
    }

    @Override
    public ArrayList<Book> decodeFromWire(int pos, Buffer buffer) {
     // My custom message starting from this *position* of buffer
        int _pos = pos;

        // Length of JSON
        int length = buffer.getInt(_pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        String jsonStr = buffer.getString(_pos+=4, _pos+=length);
        
        JsonObject jsonObject = new JsonObject(jsonStr);
        return Json.mapper.convertValue(jsonObject, new TypeReference<ArrayList<Book>>(){});
    }

    @Override
    public ArrayList<Book> transform(ArrayList<Book> listOfBooks) {
        return listOfBooks;
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
