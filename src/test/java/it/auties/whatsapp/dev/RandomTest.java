package it.auties.whatsapp.dev;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.protobuf.api.model.ProtobufSchema;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.HexFormat;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

public class RandomTest implements JacksonProvider {
    public static void main(String[] args) throws IOException {
        var container = MessageContainer.of("Hello");
        System.out.println(HexFormat.of().formatHex(PROTOBUF.writeValueAsBytes(container)));
    }
}
