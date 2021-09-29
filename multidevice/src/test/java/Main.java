import it.auties.whatsapp4j.beta.handshake.MultiDeviceSocket;
import it.auties.whatsapp4j.beta.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;

import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final HexFormat HEX = HexFormat.of();
    public static void main(String[] args) throws Exception {
        var latch = new CountDownLatch(1);
        new MultiDeviceSocket(WhatsappConfiguration.defaultOptions())
                .connect();
        latch.await();
    }
}
