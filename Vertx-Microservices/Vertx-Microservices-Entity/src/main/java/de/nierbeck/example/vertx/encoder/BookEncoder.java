package de.nierbeck.example.vertx.encoder;

import de.nierbeck.example.vertx.entity.Book;
import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class BookEncoder implements MessageCodec<Book, Book>{

    @Override
    public void encodeToWire(Buffer buffer, Book book) {
     // Easiest ways is using JSON object
        String json = Json.encode(book);

        // Length of JSON: is NOT characters count
        int length = json.getBytes().length;

        // Write data into given buffer
        buffer.appendInt(length);
        buffer.appendString(json);
    }

    @Override
    public Book decodeFromWire(int pos, Buffer buffer) {
        // My custom message starting from this *position* of buffer
        int _pos = pos;

        // Length of JSON
        int length = buffer.getInt(_pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        String jsonStr = buffer.getString(_pos+=4, _pos+=length);
        
        return Json.decodeValue(jsonStr, Book.class);
        
    }

    @Override
    public Book transform(Book book) {
        return book;
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
