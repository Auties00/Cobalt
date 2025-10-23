package com.github.auties00.cobalt.test;

import com.github.auties00.cobalt.client.mobile.android.WhatsappAndroidClientInfo;
import com.github.auties00.cobalt.client.mobile.ios.WhatsappIosClientInfo;
import com.github.auties00.cobalt.client.web.WhatsappWebClientInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ClientsTests {
    private static final Long PHONE_NUMBER_MOCK = 34751869223L;

    @Test
    public void testWebVersion() {
        assertDoesNotThrow(() -> WhatsappWebClientInfo.of().version());
    }
    
    @Test
    public void testPersonalIosVersion() {
        assertDoesNotThrow(() -> WhatsappIosClientInfo.ofPersonal().version());
    }
    
    @Test
    public void testBusinessIosVersion() {
        assertDoesNotThrow(() -> WhatsappIosClientInfo.ofBusiness().version());
    }
    
    @Test
    public void testPersonalAndroidVersion() {
        assertDoesNotThrow(() -> WhatsappAndroidClientInfo.ofPersonal().version());
    }
    
    @Test
    public void testBusinessAndroidVersion() {
        assertDoesNotThrow(() -> WhatsappAndroidClientInfo.ofBusiness().version());
    }

    @Test
    public void testPersonalIosToken() {
        assertDoesNotThrow(() -> WhatsappIosClientInfo.ofPersonal().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testBusinessIosToken() {
        assertDoesNotThrow(() -> WhatsappIosClientInfo.ofBusiness().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testPersonalAndroidToken() {
        assertDoesNotThrow(() -> WhatsappAndroidClientInfo.ofPersonal().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }

    @Test
    public void testBusinessAndroidToken() {
        assertDoesNotThrow(() -> WhatsappAndroidClientInfo.ofBusiness().computeRegistrationToken(PHONE_NUMBER_MOCK));
    }
}
