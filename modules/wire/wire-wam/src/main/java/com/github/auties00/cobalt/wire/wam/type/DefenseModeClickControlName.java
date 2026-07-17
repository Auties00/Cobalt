package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDefenseModeClickControlName")
@WamEnum
public enum DefenseModeClickControlName {
    @WamEnumConstant(0) MAIN_CONTROL,
    @WamEnumConstant(1) LAST_SEEN,
    @WamEnumConstant(2) PROFILE_PHOTO,
    @WamEnumConstant(3) ABOUT,
    @WamEnumConstant(4) GROUPS,
    @WamEnumConstant(5) TWO_STEP_VERIFICATION,
    @WamEnumConstant(6) SILENCE_UNKNOWN_CALLERS,
    @WamEnumConstant(7) DISABLE_LINK_PREVIEWS,
    @WamEnumConstant(8) PROTECT_IP_ADDRESS_IN_CALLS,
    @WamEnumConstant(9) SHOW_SECURITY_NOTIFICATIONS,
    @WamEnumConstant(10) ONLINE,
    @WamEnumConstant(11) COVER_PHOTO,
    @WamEnumConstant(12) LINKED_PROFILES,
    @WamEnumConstant(13) BLOCK_UNKNOWN_ACCOUNT_MESSAGES
}
