package it.auties.whatsapp4j.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPrivateKeySpec;
import java.security.spec.XECPublicKeySpec;

public class KeyPairDeserializer extends JsonDeserializer<KeyPair> {
    @Override
    @SneakyThrows
    public KeyPair deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        var codec = parser.getCodec();
        var node = (JsonNode) codec.readTree(parser);
        var factory = KeyFactory.getInstance("XDH");
        var algorithm = new NamedParameterSpec("X25519");
        var publicKey = factory.generatePublic(new XECPublicKeySpec(algorithm, node.get("publicKey").bigIntegerValue()));
        var privateKey = factory.generatePrivate(new XECPrivateKeySpec(algorithm, node.get("privateKey").binaryValue()));
        return new KeyPair(publicKey, privateKey);
    }
}
