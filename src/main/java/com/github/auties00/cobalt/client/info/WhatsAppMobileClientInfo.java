package com.github.auties00.cobalt.client.info;

import com.github.auties00.cobalt.model.auth.UserAgent;

public sealed interface WhatsAppMobileClientInfo
        extends WhatsAppClientInfo
        permits WhatsAppAndroidClientInfo, WhatsAppIosClientInfo {
    static WhatsAppMobileClientInfo of(UserAgent.PlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsAppAndroidClientInfo.ofPersonal();
            case IOS -> WhatsAppIosClientInfo.ofPersonal();
            case ANDROID_BUSINESS -> WhatsAppAndroidClientInfo.ofBusiness();
            case IOS_BUSINESS -> WhatsAppIosClientInfo.ofBusiness();
            case WINDOWS, MACOS -> throw new IllegalArgumentException("Cannot create WhatsappClientInfo for web");
        };
    }

    boolean business();
    String computeRegistrationToken(long nationalPhoneNumber);
}
