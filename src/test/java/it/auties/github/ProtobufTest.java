package it.auties.github;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.message.server.HandshakeMessage;
import it.auties.whatsapp.protobuf.model.client.ClientHello;
import it.auties.whatsapp.protobuf.model.companion.CompanionRegData;
import it.auties.whatsapp.socket.Proto;
import it.auties.whatsapp.utils.CypherUtils;
import it.auties.whatsapp.utils.MultiDeviceCypher;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;

public class ProtobufTest {
    public static void main(String[] args) throws Exception {
        tryDecode(0);
    }

    private static void tryDecode(int tag) {
        try {
            var custom = CompanionRegData.builder()
                    .buildHash(Base64.getDecoder().decode("S9Kdc4pc4EJryo21snc5cg=="))
                    .eRegid(BinaryArray.of(10, 4).data())
                    .eKeytype(BinaryArray.of(5, 1).data())
                    .build();

            var customEncoded = ProtobufEncoder.encode(custom);
            System.out.println(Proto.HandshakeMessage.parseFrom(customEncoded));
            System.out.println("tag: " + tag);
        }catch (Exception exception){
            tryDecode(tag + 1);
        }
    }
}
