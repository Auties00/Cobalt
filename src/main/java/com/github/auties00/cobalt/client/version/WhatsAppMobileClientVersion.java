package com.github.auties00.cobalt.client.version;

import com.github.auties00.cobalt.version.WhatsAppAndroidClientInfo;
import com.github.auties00.cobalt.version.WhatsAppIosClientInfo;
import com.github.auties00.cobalt.model.proto.auth.UserAgent;

public sealed interface WhatsAppMobileClientVersion
        extends WhatsAppClientVersion
        permits WhatsAppAndroidClientInfo, WhatsAppIosClientInfo {
    static WhatsAppMobileClientVersion of(UserAgent.PlatformType platform) {
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
