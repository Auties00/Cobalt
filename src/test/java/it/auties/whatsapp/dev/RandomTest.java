package it.auties.whatsapp.dev;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.protobuf.api.model.ProtobufSchema;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

public class RandomTest implements JacksonProvider {
    public static void main(String[] args) throws IOException {
        var test = new Test(ContactJid.of("393495089819@s.whatsapp.net"));
        var encoded = PROTOBUF.writeValueAsBytes(test);
        var decoded = PROTOBUF.reader()
                .with(ProtobufSchema.of(Test.class))
                .readValue(encoded, Test.class);
        System.out.println(decoded);
    }

    @AllArgsConstructor
    @Data
    @Builder
    @Jacksonized
    @Accessors(fluent = true)
    public static class Test implements ProtobufMessage {
        @ProtobufProperty(index = 1, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
        private ContactJid test;
    }
}
