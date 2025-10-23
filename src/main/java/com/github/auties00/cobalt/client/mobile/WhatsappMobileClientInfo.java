package com.github.auties00.cobalt.client.mobile;

import com.github.auties00.cobalt.client.WhatsappClientInfo;
import com.github.auties00.cobalt.client.mobile.android.WhatsappAndroidClientInfo;
import com.github.auties00.cobalt.client.mobile.ios.WhatsappIosClientInfo;
import com.github.auties00.cobalt.model.proto.auth.UserAgent;

public sealed interface WhatsappMobileClientInfo
        extends WhatsappClientInfo
        permits WhatsappAndroidClientInfo, WhatsappIosClientInfo {
    static WhatsappMobileClientInfo of(UserAgent.PlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsappAndroidClientInfo.ofPersonal();
            case IOS -> WhatsappIosClientInfo.ofPersonal();
            case ANDROID_BUSINESS -> WhatsappAndroidClientInfo.ofBusiness();
            case IOS_BUSINESS -> WhatsappIosClientInfo.ofBusiness();
            case WINDOWS, MACOS -> throw new IllegalArgumentException("Cannot create WhatsappClientInfo for web");
        };
    }

    boolean business();
    String computeRegistrationToken(long nationalPhoneNumber);
}
