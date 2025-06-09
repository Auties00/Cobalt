package it.auties.whatsapp.util;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.AdapterFactory;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;

@CustomAdapter
public class ConcurrentLinkedSetJsonAdapter<T> implements JsonAdapter<ConcurrentLinkedSet<T>> {
    private final JsonAdapter<T> adapter;

    public static final AdapterFactory FACTORY = (type, jsonb) -> {
        if (Types.isGenericTypeOf(type, ConcurrentLinkedSet.class)) {
            return new ConcurrentLinkedSetJsonAdapter<>(jsonb, Types.typeArguments(type));
        }

        return null;
    };

    private ConcurrentLinkedSetJsonAdapter(Jsonb jsonb, java.lang.reflect.Type[] types) {
        this.adapter = jsonb.adapter(types[0]);
    }

    @Override
    public void toJson(JsonWriter jsonWriter, ConcurrentLinkedSet<T> entries) {
        jsonWriter.beginArray();
        for (var entry : entries) {
            adapter.toJson(jsonWriter, entry);
        }
        jsonWriter.endArray();
    }

    @Override
    public ConcurrentLinkedSet<T> fromJson(JsonReader jsonReader) {
        var result = new ConcurrentLinkedSet<T>();
        jsonReader.beginArray();
        while (jsonReader.hasNextElement()) {
            var entry = adapter.fromJson(jsonReader);
            result.add(entry);
        }
        jsonReader.endArray();
        return result;
    }
}
