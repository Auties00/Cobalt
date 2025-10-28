package com.github.auties00.cobalt.client.version;

import com.github.auties00.cobalt.model.auth.UserAgent;

public sealed interface WhatsAppMobileClientVersion
        extends WhatsAppClientVersion
        permits WhatsAppAndroidClientVersion, WhatsAppIosClientVersion {
    static WhatsAppMobileClientVersion of(UserAgent.PlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsAppAndroidClientVersion.ofPersonal();
            case IOS -> WhatsAppIosClientVersion.ofPersonal();
            case ANDROID_BUSINESS -> WhatsAppAndroidClientVersion.ofBusiness();
            case IOS_BUSINESS -> WhatsAppIosClientVersion.ofBusiness();
            case WINDOWS, MACOS -> throw new IllegalArgumentException("Cannot create WhatsappClientInfo for web");
        };
    }

    boolean business();
    String computeRegistrationToken(long nationalPhoneNumber);
}
