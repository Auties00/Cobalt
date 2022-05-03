package it.auties.whatsapp.dev;

import it.auties.bytes.Bytes;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.protobuf.api.model.ProtobufSchema;
import it.auties.whatsapp.crypto.AesCbc;
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
        var iv = Bytes.ofHex("320459788ac33e2c074b177d4c7c9568")
                .toByteArray();
        var data = Bytes.ofHex("32130a1153746f2074657374616e646f207369756d01")
                .toByteArray();
        var key = Bytes.ofHex("23bca7ae5d68e3f099d687e73a04e6f8c7bba0c5671430658075916cacbf4ff0")
                .toByteArray();
        var ciphered = AesCbc.encrypt(iv, data, key);
        System.out.println(Bytes.of(ciphered).toHex());
    }
}
