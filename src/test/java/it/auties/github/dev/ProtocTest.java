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

        // Ass way to benchmark
        var times = 1_000;
        var googleTimes = new long[times];
        var modernTimes = new long[times];
        for(var index = 0; index < times; index++){
            var googleStart = System.currentTimeMillis();
            Whatsapp.Message.parseFrom(data);
            googleTimes[index] = System.currentTimeMillis() - googleStart;

            var moderStart = System.currentTimeMillis();
            ProtobufDecoder.forType(MessageContainer.class)
                    .decode(data);
            modernTimes[index] = System.currentTimeMillis() - moderStart;
        }

        System.out.printf("Google took on average %s ms%n", Arrays.stream(googleTimes).average().getAsDouble());
        System.out.printf("ModernProtobuf took on average %s ms%n", Arrays.stream(modernTimes).average().getAsDouble());
    }
}
