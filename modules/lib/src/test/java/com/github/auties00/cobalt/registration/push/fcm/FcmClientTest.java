package com.github.auties00.cobalt.registration.push.fcm;

import com.github.auties00.cobalt.Faker;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.exception.linked.mobile.WhatsAppRegistrationException;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.registration.MobileClientRegistration;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration suite for {@link FcmClient}: drives real authentication and registration flows against
 * Google FCM and WhatsApp, so the cases hit the network rather than a stub. A 45-second cool-down runs before
 * every method except the first (guarded by the {@code FIRST_TEST} flag rather than JUnit ordering, so it holds
 * under randomised method order) to stay under the C2DM anti-abuse threshold.
 */
@Timeout(value = 15, unit = TimeUnit.MINUTES)
class FcmClientTest {
    private static final AtomicBoolean FIRST_TEST = new AtomicBoolean(true);

    @Test
    public void deliversPushCodeViaRegistration() throws Throwable {
        var maxAttempts = 5;
        var perAttemptTimeout = Duration.ofMinutes(2);
        var device = LinkedWhatsAppClientDevice.ios(false);
        Throwable lastFailure = null;
        for (var attempt = 1; attempt <= maxAttempts; attempt++) {
            var phoneNumber = Faker.randomItalianMobile();
            try (var pushClient = FcmClient.newSession()) {
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

    @BeforeEach
    void throttleAgainstC2dmAntiAbuse() throws InterruptedException {
        if (FIRST_TEST.compareAndSet(true, false)) {
            return;
        }
        TimeUnit.SECONDS.sleep(45);
    }

    @Test
    void supportedPlatformsListsBothAndroidVariants() {
        try (var client = FcmClient.newSession()) {
            assertEquals(
                    Set.of(ClientPlatformType.ANDROID, ClientPlatformType.ANDROID_BUSINESS),
                    client.supportedPlatforms());
        }
    }

    @Test
    void closeIsIdempotentBeforeAuthenticate() {
        var client = FcmClient.newSession();
        client.close();
        assertDoesNotThrow(client::close);
    }

    @Test
    void accessorsRejectUnauthenticatedClient() {
        try (var client = FcmClient.newSession()) {
            assertFalse(client.isAuthenticated());
            assertThrows(IllegalStateException.class, client::getSession);
            assertThrows(IllegalStateException.class, client::getPushToken);
            assertThrows(IllegalStateException.class, client::getPushCode);
        }
    }

    @Test
    void rejectsNonAndroidDevice() {
        try (var client = FcmClient.newSession()) {
            assertThrows(IllegalArgumentException.class,
                    () -> client.authenticate(LinkedWhatsAppClientDevice.ios(false)));
            assertThrows(IllegalArgumentException.class,
                    () -> client.authenticate(LinkedWhatsAppClientDevice.web()));
            assertFalse(client.isAuthenticated());
        }
    }

    @Test
    void authenticatesPersonalAndProducesPushToken() {
        try (var client = FcmClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.android(false));
            assertTrue(client.isAuthenticated());
            var token = client.getPushToken();
            assertNotNull(token);
            assertFalse(token.isBlank(),
                    () -> "expected non-empty FCM registration token, got: " + token);
        }
    }

    @Test
    void authenticatesBusinessAndProducesPushToken() {
        try (var client = FcmClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.android(true));
            assertTrue(client.isAuthenticated());
            assertFalse(client.getPushToken().isBlank());
        }
    }

    @Test
    void rejectsDoubleAuthenticate() {
        try (var client = FcmClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.android(false));
            assertThrows(IllegalStateException.class,
                    () -> client.authenticate(LinkedWhatsAppClientDevice.android(false)));
        }
    }

    @Test
    void sessionRoundTripsThroughLoadSession() throws Exception {
        FcmSession saved;
        String firstToken;
        try (var client = FcmClient.newSession()) {
            client.authenticate(LinkedWhatsAppClientDevice.android(false));
            firstToken = client.getPushToken();
            saved = client.getSession();
            assertNotEquals(0L, saved.androidId());
            assertNotEquals(0L, saved.securityToken());
            assertFalse(saved.fcmToken().isBlank());
        }

        try (var loaded = FcmClient.loadSession(saved)) {
            assertTrue(loaded.isAuthenticated());
            // Exact-token equality, not just non-blank: the token is server-issued and cached in the
            // session, so a differing token would mean the round-trip silently dropped the cached value.
            assertEquals(firstToken, loaded.getPushToken());
        }
    }
}
