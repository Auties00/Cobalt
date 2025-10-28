package com.github.auties00.cobalt.client.version;

import com.github.auties00.cobalt.model.auth.UserAgent;
import com.github.auties00.cobalt.model.auth.Version;

public sealed interface WhatsAppClientVersion
        permits WhatsAppWebClientVersion, WhatsAppMobileClientVersion {
    static WhatsAppClientVersion of(UserAgent.PlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsAppAndroidClientVersion.ofPersonal();
            case IOS -> WhatsAppIosClientVersion.ofPersonal();
            case ANDROID_BUSINESS -> WhatsAppAndroidClientVersion.ofBusiness();
            case IOS_BUSINESS -> WhatsAppIosClientVersion.ofBusiness();
            case WINDOWS, MACOS -> WhatsAppWebClientVersion.of();
        };
    }

    Version latest();
}
