package com.example.examguard.utility;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;

public class OffsetDateTimeAdapter implements JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {

    @Override
    public JsonElement serialize(OffsetDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(src.toString());
    }

    @Override
    public OffsetDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        if (json == null || json.isJsonNull()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(json.getAsString());
        } catch (Exception e) {
            throw new JsonParseException("Invalid OffsetDateTime format: " + json.getAsString(), e);
        }
    }
}