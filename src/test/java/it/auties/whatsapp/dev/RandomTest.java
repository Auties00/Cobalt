package it.auties.whatsapp.dev;

import it.auties.bytes.Bytes;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.BytesHelper;

import java.io.IOException;
import java.util.Arrays;

public class RandomTest {
    public static void main(String[] args) throws IOException {
        var iv = Bytes.ofRandom(16).toByteArray();
        var plain = Bytes.ofRandom(10_240).toByteArray();
        var key = Bytes.ofRandom(32).toByteArray();
        var enc = AesCbc.encrypt(iv, plain, key);
        var dec = AesCbc.decrypt(iv, enc, key);
        System.out.println(Arrays.toString(plain) + ": " + plain.length);
        System.out.println(Arrays.toString(dec) + ": " + dec.length);
    }
}
