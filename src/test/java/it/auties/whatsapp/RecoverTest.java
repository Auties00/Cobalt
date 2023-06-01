package it.auties.whatsapp;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

public class RecoverTest {
    @Test
    public void test(){
        var phoneNumber = 923231246561L;
        var publicKey = Base64.getDecoder().decode("zENAcy7lPCP60TK8/l1Cm8sUEGI23/eLVwP+sVgKhAI=");
        var privateKey = Base64.getDecoder().decode("QE0ihoaNYykKBymn6UYe+20vtt9bifaqaoWQ/sKI7nQ=");
        var messagePublicKey = Base64.getDecoder().decode("ly3YX83y9bcx2QBWtkhLMg0YTuyyjn6NyWS1hbPqM3s=");
        var messagePrivateKey = Base64.getDecoder().decode("GHUrLl6OYlmah5h4YaYAL2CH684lTrixWMuT0+L7y0k=");
        var registrationId = Base64.getDecoder().decode("OTIzMjMxMjQ2NTYxIwaB8Ug2LKOWPsP3tl4AqQnIEJGG");
        var uuid = UUID.randomUUID();
        var store = Store.of(uuid, phoneNumber, ClientType.MOBILE, false).get();
        var keys = Keys.of(uuid, phoneNumber, messagePublicKey, messagePrivateKey, publicKey, privateKey, registrationId);
        Whatsapp.of(store, keys)
                .connect()
                .join()
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .connect()
                .join()
                .awaitDisconnection();
    }
}
