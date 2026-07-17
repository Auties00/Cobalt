package com.github.auties00.cobalt.client.linked.info;

import com.github.auties00.cobalt.Faker;
import org.junit.jupiter.api.Test;


import static com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Network-backed smoke suite for the {@link LinkedWhatsAppClientInfo} hierarchy.
 *
 * <p>Each {@code version()} test resolves a live upstream version for one platform flavour, so the suite reaches the
 * network: web and Windows hit {@code web.whatsapp.com}, iOS hits the Apple App Store lookup endpoint, and Android
 * downloads the current Play Store APK. Because the upstream version numbers shift with every WhatsApp release the
 * tests assert behaviour rather than exact output, namely that no exception escapes the resolution pipeline or the
 * {@link WhatsAppMobileClientInfo#computeRegistrationToken(long)} algorithm.
 */
public class LinkedWhatsAppClientInfoTest {
    // Re-randomised per run so digit-dependent branches in the token algorithm are exercised over time.
    private static final Long PHONE_NUMBER_MOCK = Faker.randomItalianMobile();

    @Test
    public void testWebVersion() {
        assertDoesNotThrow(() -> LinkedWhatsAppClientInfo.of(MACOS).version());
        assertDoesNotThrow(() -> LinkedWhatsAppClientInfo.of(WINDOWS).version());
    }

    @Test
    public void testPersonalIosVersion() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(IOS).version());
    }

    @Test
    public void testBusinessIosVersion() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(IOS_BUSINESS).version());
    }

    @Test
    public void testPersonalAndroidVersion() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(ANDROID).version());
    }

    @Test
    public void testBusinessAndroidVersion() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(ANDROID_BUSINESS).version());
    }

    @Test
    public void testPersonalIosToken() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(IOS).computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testBusinessIosToken() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(IOS_BUSINESS).computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testPersonalAndroidToken() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(ANDROID).computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testBusinessAndroidToken() {
        assertDoesNotThrow(() -> WhatsAppMobileClientInfo.of(ANDROID_BUSINESS).computeRegistrationToken(PHONE_NUMBER_MOCK));
    }
}
