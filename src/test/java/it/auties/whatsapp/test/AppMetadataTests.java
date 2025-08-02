package it.auties.whatsapp.test;

import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.AppMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AppMetadataTests {
    private static final Long PHONE_NUMBER_MOCK = 3934751869223L;
    @Test
    public void testWebVersion() {
        assertDoesNotThrow(() -> AppMetadata.getVersion(PlatformType.WINDOWS));
        assertDoesNotThrow(() -> AppMetadata.getVersion(PlatformType.MACOS));
    }
    
    @Test
    public void testPersonalIosVersion() {
        assertDoesNotThrow(() -> getVersion(PlatformType.IOS));
    }
    
    @Test
    public void testBusinessIosVersion() {
        assertDoesNotThrow(() -> getVersion(PlatformType.IOS_BUSINESS));
    }
    
    @Test
    public void testPersonalAndroidVersion() {
        assertDoesNotThrow(() -> getVersion(PlatformType.ANDROID));
    }
    
    @Test
    public void testBusinessAndroidVersion() {
        assertDoesNotThrow(() -> getVersion(PlatformType.ANDROID_BUSINESS));
    }

    @Test
    public void testPersonalIosToken() {
        assertDoesNotThrow(() -> getToken(PlatformType.IOS));
    }

    @Test
    public void testBusinessIosToken() {
        assertDoesNotThrow(() -> getToken(PlatformType.IOS_BUSINESS));
    }

    @Test
    public void testPersonalAndroidToken() {
        assertDoesNotThrow(() -> getToken(PlatformType.ANDROID));
    }

    @Test
    public void testBusinessAndroidToken() {
        assertDoesNotThrow(() -> getToken(PlatformType.ANDROID_BUSINESS));
    }

    private static Version getVersion(PlatformType platformType) {
        return AppMetadata.getVersion(platformType);
    }

    private static String getToken(PlatformType platformType) {
        return AppMetadata.getToken(
                PHONE_NUMBER_MOCK,
                platformType,
                AppMetadata.getVersion(platformType)
        );
    }
}
