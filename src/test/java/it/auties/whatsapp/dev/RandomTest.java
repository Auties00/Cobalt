package it.auties.whatsapp.dev;

import it.auties.bytes.Bytes;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.BytesHelper;

import java.io.IOException;
import java.util.Arrays;

public class RandomTest {
    public static void main(String[] args) throws IOException {
        System.out.println(ContactJid.of("status@broadcast").user());
    }
}
