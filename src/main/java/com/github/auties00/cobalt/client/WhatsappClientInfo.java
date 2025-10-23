package com.github.auties00.cobalt.client;

import com.github.auties00.cobalt.client.mobile.WhatsappMobileClientInfo;
import com.github.auties00.cobalt.client.mobile.android.WhatsappAndroidClientInfo;
import com.github.auties00.cobalt.client.mobile.ios.WhatsappIosClientInfo;
import com.github.auties00.cobalt.client.web.WhatsappWebClientInfo;
import com.github.auties00.cobalt.model.proto.auth.UserAgent;
import com.github.auties00.cobalt.model.proto.auth.Version;

public sealed interface WhatsappClientInfo
        permits WhatsappWebClientInfo, WhatsappMobileClientInfo {
    static WhatsappClientInfo of(UserAgent.PlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsappAndroidClientInfo.ofPersonal();
            case IOS -> WhatsappIosClientInfo.ofPersonal();
            case ANDROID_BUSINESS -> WhatsappAndroidClientInfo.ofBusiness();
            case IOS_BUSINESS -> WhatsappIosClientInfo.ofBusiness();
            case WINDOWS, MACOS -> WhatsappWebClientInfo.of();
        };
    }

    Version version();
}
