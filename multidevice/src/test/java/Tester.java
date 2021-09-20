import it.auties.whatsapp4j.beta.handshake.MultiDeviceSocket;
import it.auties.whatsapp4j.beta.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;

import java.util.concurrent.CountDownLatch;

public class Tester {
    public static void main(String[] args) throws InterruptedException {
        var latch = new CountDownLatch(1);
        new MultiDeviceSocket(WhatsappConfiguration.defaultOptions(), new MultiDeviceKeysManager()).connect();
        latch.await();
    }
}
