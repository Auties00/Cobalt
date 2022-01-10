package it.auties.github.dev;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.protobuf.message.model.MessageContainer;
import it.auties.whatsapp.protobuf.message.server.ProtocolMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HexFormat;

public class ProtocTest {
    @Test
    public void test() throws Exception {
        var data = HexFormat.of()
                .parseHex("624910063a450a430a080a0600000000e32512370a2049ea2bcae40210a8b532ec46901921a6aca69cfe7c01ebcd77920f2342e21fee120c0883ddd3ce0610011a020001189cbfadd5e32f");
        var googleResult = Whatsapp.Message.parseFrom(data);
        for(var key : googleResult.getProtocolMessage().getAppStateSyncKeyShare().getKeysList()){
            System.out.println(Arrays.toString(key.getKeyId().getKeyId().toByteArray()));
            System.out.println(Arrays.toString(key.getKeyData().getKeyData().toByteArray()));
            System.out.println();
        }

        var modernResult = ProtobufDecoder.forType(MessageContainer.class)
                .decode(data);
        for(var key : ((ProtocolMessage) modernResult.content()).appStateSyncKeyShare().keys()){
            System.out.println(Arrays.toString(key.keyId().keyId()));
            System.out.println(Arrays.toString(key.keyData().keyData()));
            System.out.println();
        }
    }
}
