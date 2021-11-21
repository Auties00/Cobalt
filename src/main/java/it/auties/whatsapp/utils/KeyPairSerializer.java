package it.auties.whatsapp.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import it.auties.whatsapp.utils.CypherUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.XECPublicKey;

public class KeyPairSerializer extends JsonSerializer<KeyPair> {
    @Override
    public void serialize(KeyPair keyPair, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeObjectField("publicKey", encodePublicKey(keyPair));
        generator.writeObjectField("privateKey", encodePrivateKey(keyPair));
        generator.writeEndObject();
    }

    private BigInteger encodePublicKey(KeyPair keyPair) {
        return ((XECPublicKey) keyPair.getPublic()).getU();
    }

    private byte[] encodePrivateKey(KeyPair keyPair) {
        return CypherUtils.raw(keyPair.getPrivate());
    }
}
