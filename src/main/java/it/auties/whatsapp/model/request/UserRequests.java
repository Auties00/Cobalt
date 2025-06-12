package it.auties.whatsapp.model.request;

import com.alibaba.fastjson2.JSONWriter;

import java.io.IOException;
import java.io.StringWriter;

@SuppressWarnings("UnusedLabel")
public class UserRequests {
    public static String chosenName(String user) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startArray();
                    variable: {
                        writer.startObject();
                        writer.writeName("user_id");
                        writer.writeColon();
                        writer.writeString(user);
                        writer.endObject();
                    }
                    writer.endArray();
                }
                writer.endObject();
            }
            try(var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
