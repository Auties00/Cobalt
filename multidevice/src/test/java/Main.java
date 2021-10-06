import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.listener.IWhatsappListener;
import it.auties.whatsapp4j.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.socket.MultiDeviceSocket;

import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws Exception {
        var latch = new CountDownLatch(1);
        var api = new MultiDeviceSocket(WhatsappConfiguration.defaultOptions(), new MultiDeviceKeysManager());
        api.manager().listeners().add(new IWhatsappListener() {});
        api.connect();
        latch.await();
    }
}
