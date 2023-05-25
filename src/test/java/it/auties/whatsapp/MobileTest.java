package it.auties.whatsapp;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.DefaultControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class MobileTest {
    @Test
    public void run() {
        var uuid = DefaultControllerSerializer.instance()
                .listIds(ClientType.MOBILE)
                .peekLast();
        var protoKeys = Keys.of(uuid, null, ClientType.MOBILE, true);
        var keyPair = SignalKeyPair.random();
        var keys = Keys.of(protoKeys.uuid(), 17405281037L, protoKeys.noiseKeyPair().publicKey(), protoKeys.noiseKeyPair().privateKey(), keyPair.publicKey(), keyPair.privateKey(), null);
        var store = Store.random(uuid, 17405281037L, ClientType.MOBILE);
        Whatsapp.of(store, keys)
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(it.auties.whatsapp.MobileTest::onConnected)
                .connect()
                .join()
                .awaitDisconnection();
    }

    @SneakyThrows
    private static void onConnected(Whatsapp api) {
        System.out.println("Connected to mobile api");
        api.unlinkCompanions().join();
        api.linkCompanion("2@Mk6xEaYP7mXHVEDk9oL6CDbkW+RW8+uwPVaS2EqeoBc17CBW+n+RVeDmzsKIFSCD6FBkOidlBnGtIg==,g2Ka7Tp3d95JAlqtJdE2D+3iki8Cch42LTsJZJCE6j4=,yU9UjZGmSwlg0pLOp9bqtLr5wko2pzSHIRjXj/qJwxs=,6fXRi5HFnfFge95/G0Rn9gVrB/8Nhzb9v/WRFPf6ALY=")
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
