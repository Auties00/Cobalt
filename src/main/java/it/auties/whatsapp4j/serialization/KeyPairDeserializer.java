package it.auties.whatsapp4j.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPrivateKeySpec;
import java.security.spec.XECPublicKeySpec;

public class KeyPairDeserializer extends JsonDeserializer<KeyPair> {
    @Override
    public KeyPair deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException{
        try {
            var node = readJsonNode(parser);
            var factory = KeyFactory.getInstance("XDH");
            var algorithm = new NamedParameterSpec("X25519");
            var publicKey = decodePublicKey(node, factory, algorithm);
            var privateKey = decodePrivateKey(node, factory, algorithm);
            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new RuntimeException("Cannot deserialize keypair: cryptographic exception", exception);
        }
    }

    private JsonNode readJsonNode(JsonParser parser) throws IOException {
        var codec = parser.getCodec();
        return codec.readTree(parser);
    }

    private PrivateKey decodePrivateKey(JsonNode node, KeyFactory factory, NamedParameterSpec algorithm) throws InvalidKeySpecException, IOException {
        var privateKey = node.get("privateKey").binaryValue();
        return factory.generatePrivate(new XECPrivateKeySpec(algorithm, privateKey));
    }

    private PublicKey decodePublicKey(JsonNode node, KeyFactory factory, NamedParameterSpec algorithm) throws InvalidKeySpecException {
        var publicKey = node.get("publicKey").bigIntegerValue();
        return factory.generatePublic(new XECPublicKeySpec(algorithm, publicKey));
    }
}
