package com.github.auties00.cobalt.client.info;

import com.github.auties00.cobalt.model.auth.UserAgent;
import com.github.auties00.cobalt.model.auth.Version;

public sealed interface WhatsAppClientInfo
        permits WhatsAppWebClientInfo, WhatsAppMobileClientInfo {
    static WhatsAppClientInfo of(UserAgent.PlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsAppAndroidClientInfo.ofPersonal();
            case IOS -> WhatsAppIosClientInfo.ofPersonal();
            case ANDROID_BUSINESS -> WhatsAppAndroidClientInfo.ofBusiness();
            case IOS_BUSINESS -> WhatsAppIosClientInfo.ofBusiness();
            case WINDOWS, MACOS -> WhatsAppWebClientInfo.of();
        };
    }

    Version latest();
}
