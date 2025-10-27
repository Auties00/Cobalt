package com.github.auties00.cobalt.test;

import com.github.auties00.cobalt.version.WhatsAppAndroidClientInfo;
import com.github.auties00.cobalt.version.WhatsAppIosClientInfo;
import com.github.auties00.cobalt.version.WhatsAppWebClientInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ClientsTests {
    private static final Long PHONE_NUMBER_MOCK = 34751869223L;

    @Test
    public void testWebVersion() {
        assertDoesNotThrow(() -> WhatsAppWebClientInfo.of().version());
    }
    
    @Test
    public void testPersonalIosVersion() {
        assertDoesNotThrow(() -> WhatsAppIosClientInfo.ofPersonal().version());
    }
    
    @Test
    public void testBusinessIosVersion() {
        assertDoesNotThrow(() -> WhatsAppIosClientInfo.ofBusiness().version());
    }
    
    @Test
    public void testPersonalAndroidVersion() {
        assertDoesNotThrow(() -> WhatsAppAndroidClientInfo.ofPersonal().version());
    }
    
    @Test
    public void testBusinessAndroidVersion() {
        assertDoesNotThrow(() -> WhatsAppAndroidClientInfo.ofBusiness().version());
    }

    @Test
    public void testPersonalIosToken() {
        assertDoesNotThrow(() -> WhatsAppIosClientInfo.ofPersonal().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testBusinessIosToken() {
        assertDoesNotThrow(() -> WhatsAppIosClientInfo.ofBusiness().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testPersonalAndroidToken() {
        assertDoesNotThrow(() -> WhatsAppAndroidClientInfo.ofPersonal().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testBusinessAndroidToken() {
        assertDoesNotThrow(() -> WhatsAppAndroidClientInfo.ofBusiness().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }
}
