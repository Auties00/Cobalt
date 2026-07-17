package com.github.auties00.cobalt.registration.push.apns;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.exception.linked.mobile.WhatsAppRegistrationException;
import com.github.auties00.cobalt.Faker;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.registration.MobileClientRegistration;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration suite for {@link ApnsClient}, exercising the real Apple Push Notification courier
 * and the WhatsApp registration flow it feeds. Tests that talk to WhatsApp retry across several fresh
 * phone numbers to absorb transient server-side rejections, so a green run requires network access and
 * working APNS connectivity.
 */
@Timeout(value = 10, unit = TimeUnit.MINUTES)
class ApnsClientTest {

    @Test
    public void deliversPushCodeViaRegistration() throws Throwable {
        var maxAttempts = 5;
        var perAttemptTimeout = Duration.ofMinutes(2);
        var device = LinkedWhatsAppClientDevice.ios(false);
        Throwable lastFailure = null;
        for (var attempt = 1; attempt <= maxAttempts; attempt++) {
            var phoneNumber = Faker.randomItalianMobile();
            try (var pushClient = ApnsClient.newSession()) {
                pushClient.authenticate(device);

                var store = LinkedWhatsAppStoreFactory.temporary()
                        .create(LinkedWhatsAppClientType.MOBILE, phoneNumber);
                store.accountStore().setDevice(device);

                var verification = LinkedWhatsAppClientVerificationHandler.Mobile
                        .whatsapp(pushClient::getPushCode);

                var registrationFailure = new AtomicReference<Throwable>();
                var registrationDone = new CountDownLatch(1);
                var thread = Thread.ofVirtual().start(() -> {
                    try (var registration = MobileClientRegistration.newRegistration(
                            store, verification, null, pushClient)) {
                        registration.register();
                    } catch (Throwable t) {
                        registrationFailure.set(t);
                    } finally {
                        registrationDone.countDown();
                    }
                });

                if (!registrationDone.await(perAttemptTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    // WA never silent-pushed: getPushCode is parked on the holder's
                    // monitor inside the registration thread. Closing the push
                    // client wakes it and lets the registration thread unwind.
                    pushClient.close();
                    if (!registrationDone.await(15, TimeUnit.SECONDS)) {
                        thread.interrupt();
                        registrationDone.await();
                    }
                    lastFailure = new AssertionError(
                            "attempt " + attempt + " timed out waiting for push to " + phoneNumber);
                    System.out.printf("attempt %d/%d for %d timed out waiting for push%n",
                            attempt, maxAttempts, phoneNumber);
                    continue;
                }

                var failure = registrationFailure.get();
                if (failure == null) {
                    assertTrue(store.accountStore().registered(),
                            "store must report registered after a successful flow");
                    return;
                }
                if (failure instanceof WhatsAppRegistrationException reg) {
                    lastFailure = reg;
                    System.out.printf("attempt %d/%d for %d rejected by Whatsapp: %s%n",
                            attempt, maxAttempts, phoneNumber, reg.getMessage());
                    continue;
                }
                throw failure;
            }
        }
        fail("Registration via push did not succeed within " + maxAttempts + " attempts: "
             + lastFailure.getMessage(),
                lastFailure);
    }

    @Test
    void supportedPlatformsListsBothIosVariants() {
        try (var client = ApnsClient.newSession()) {
            assertEquals(
                    Set.of(ClientPlatformType.IOS, ClientPlatformType.IOS_BUSINESS),
                    client.supportedPlatforms());
        }
    }

    @Test
    void closeIsIdempotentBeforeAuthenticate() {
        var client = ApnsClient.newSession();
        client.close();
        assertDoesNotThrow(client::close);
    }

    @Test
    void accessorsRejectUnauthenticatedClient() {
        try (var client = ApnsClient.newSession()) {
            assertFalse(client.isAuthenticated());
            assertThrows(IllegalStateException.class, client::getSession);
            assertThrows(IllegalStateException.class, client::getPushToken);
            assertThrows(IllegalStateException.class, client::getPushCode);
        }
    }

    @Test
    void rejectsNonIosDevice() {
        try (var client = ApnsClient.newSession()) {
            assertThrows(IllegalArgumentException.class,
                    () -> client.authenticate(LinkedWhatsAppClientDevice.android(false)));
            assertThrows(IllegalArgumentException.class,
                    () -> client.authenticate(LinkedWhatsAppClientDevice.web()));
            assertFalse(client.isAuthenticated());
        }
    }

    @Test
    void authenticatesPersonalAndProducesPushToken() {
        try (var client = ApnsClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.ios(false));
            assertTrue(client.isAuthenticated());
            var token = client.getPushToken();
            assertNotNull(token);
            // APNS device tokens are 32 raw bytes returned hex-encoded, hence 64 characters
            assertEquals(64, token.length(), () -> "expected 64-char hex token, got: " + token);
        }
    }

    @Test
    void authenticatesBusinessAndProducesPushToken() {
        try (var client = ApnsClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.ios(true));
            assertTrue(client.isAuthenticated());
            assertEquals(64, client.getPushToken().length());
        }
    }

    @Test
    void rejectsDoubleAuthenticate() {
        try (var client = ApnsClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.ios(false));
            assertThrows(IllegalStateException.class,
                    () -> client.authenticate(LinkedWhatsAppClientDevice.ios(false)));
        }
    }

    @Test
    void sessionRoundTripsThroughLoadSession() throws Exception {
        ApnsSession saved;
        try (var client = ApnsClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.ios(false));
            assertEquals(64, client.getPushToken().length());
            saved = client.getSession();
            assertTrue(saved.privateKeyDer().length > 0);
            assertTrue(saved.publicKeyDer().length > 0);
            assertTrue(saved.deviceCertificate().length > 0);
        }

        try (var loaded = ApnsClient.loadSession(saved)) {
            assertTrue(loaded.isAuthenticated());
            // Apple rotates the device token across courier sessions, so assert shape not equality:
            // only the activation certificate is durable, the reloaded token need not match the saved one
            assertEquals(64, loaded.getPushToken().length());
        }
    }
}
