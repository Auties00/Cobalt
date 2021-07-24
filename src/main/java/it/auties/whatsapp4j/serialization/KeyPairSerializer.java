package it.auties.whatsapp4j.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.XECPrivateKey;
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
        return ((XECPrivateKey) keyPair.getPrivate())
                .getScalar()
                .orElseThrow(() -> new IllegalArgumentException("Cannot serialize a private key with no scalar value"));
    }
}
