package com.ambrosia.backup;

import apple.utilities.json.gson.GsonBuilderDynamic;
import apple.utilities.json.gson.serialize.JsonSerializing;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import java.time.Instant;

public class InstantGsonSerializing implements JsonSerializing<Instant> {

    public static GsonBuilder registerGson(GsonBuilder gson) {
        return gson.registerTypeAdapter(Instant.class, new InstantGsonSerializing());
    }

    public static GsonBuilderDynamic registerGson(GsonBuilderDynamic gson) {
        return gson.registerTypeAdapter(Instant.class, new InstantGsonSerializing());
    }

    @Override
    public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Instant.parse(json.getAsJsonPrimitive().getAsString());
    }

    @Override
    public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
