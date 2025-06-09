package it.auties.whatsapp.model.request;

import io.avaje.jsonb.Jsonb;

import java.io.IOException;
import java.io.StringWriter;

@SuppressWarnings("UnusedLabel")
public class UserRequests {
    public static String chosenName(String user) {
        try(
                var result = new StringWriter();
                var writer = Jsonb.builder()
                        .build()
                        .writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginArray();
                    variable: {
                        writer.beginObject();
                        writer.name("user_id");
                        writer.value(user);
                        writer.endObject();
                    }
                    writer.endArray();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
