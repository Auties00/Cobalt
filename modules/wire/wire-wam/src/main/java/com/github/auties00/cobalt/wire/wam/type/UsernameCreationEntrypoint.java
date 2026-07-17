package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameCreationEntrypoint")
@WamEnum
public enum UsernameCreationEntrypoint {
    @WamEnumConstant(1) USERNAME_UPSELL,
    @WamEnumConstant(2) PROFILE_SETTING,
    @WamEnumConstant(3) PIN_UPSELL_INTEGRITY_BANNER,
    @WamEnumConstant(4) USERNAME_ACTIVATION_BANNER,
    @WamEnumConstant(5) USERNAME_UPSELL_CREATION_BANNER_WA,
    @WamEnumConstant(6) USERNAME_UPSELL_RESERVATION_BANNER_WA,
    @WamEnumConstant(7) SMB_FB_USERNAME_RESERVATION,
    @WamEnumConstant(8) ACTIVATION_APP_FOREGROUND,
    @WamEnumConstant(9) DEEP_LINK,
    @WamEnumConstant(10) FB_PAGE_WA_DEEPLINK,
    @WamEnumConstant(11) IG_WA_DEEPLINK,
    @WamEnumConstant(12) USERNAME_UPSELL_SYS_MSG
}
