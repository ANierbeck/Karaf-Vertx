package de.nierbeck.example.vertx.encoder;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import de.nierbeck.example.vertx.entity.Book;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class ListOfBookEncoder implements MessageCodec<List<Book>, List<Book>> {

    @Override
    public void encodeToWire(Buffer buffer, List<Book> listOfBooks) {
        String json = Json.encode(listOfBooks);
        
        int length = json.getBytes().length;
        
        buffer.appendInt(length);
        buffer.appendString(json);
        
    }

    @Override
    public List<Book> decodeFromWire(int pos, Buffer buffer) {
     // My custom message starting from this *position* of buffer
        int _pos = pos;

        // Length of JSON
        int length = buffer.getInt(_pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        String jsonStr = buffer.getString(_pos+=4, _pos+=length);
        
        JsonObject jsonObject = new JsonObject(jsonStr);
        return Json.mapper.convertValue(jsonObject, new TypeReference<List<Book>>(){});
    }

    @Override
    public List<Book> transform(List<Book> listOfBooks) {
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
