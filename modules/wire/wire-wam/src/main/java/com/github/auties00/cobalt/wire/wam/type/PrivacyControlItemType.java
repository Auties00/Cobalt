package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPrivacyControlItemType")
@WamEnum
public enum PrivacyControlItemType {
    @WamEnumConstant(0) LAST_SEEN_AND_ONLINE,
    @WamEnumConstant(1) PROFILE_PHOTO,
    @WamEnumConstant(2) ABOUT,
    @WamEnumConstant(3) GROUPS,
    @WamEnumConstant(4) STATUS,
    @WamEnumConstant(5) READ_RECEIPT,
    @WamEnumConstant(6) BLOCKED,
    @WamEnumConstant(7) LIVE_LOCATION,
    @WamEnumConstant(8) SCREEN_LOCK,
    @WamEnumConstant(9) DDM_TIMER,
    @WamEnumConstant(10) CALLS,
    @WamEnumConstant(11) FINGERPRINT_LOCK,
    @WamEnumConstant(12) DISAPPEARING_MESSAGES,
    @WamEnumConstant(13) UNKNOWN,
    @WamEnumConstant(14) CHECKUP,
    @WamEnumConstant(15) SHOW_PREVIEW,
    @WamEnumConstant(16) E2EE_BACKUPS,
    @WamEnumConstant(17) TWO_STEP_VERIFICATION,
    @WamEnumConstant(18) FACE_AND_HAND_EFFECTS,
    @WamEnumConstant(19) ADVANCED,
    @WamEnumConstant(20) CHAT_LOCK,
    @WamEnumConstant(21) AVATAR,
    @WamEnumConstant(22) CONTACTS,
    @WamEnumConstant(23) PRIVACY_CHECKUP,
    @WamEnumConstant(24) PIX,
    @WamEnumConstant(25) DEFENSE_MODE,
    @WamEnumConstant(26) PROFILE_LINKS,
    @WamEnumConstant(27) PASSKEY,
    @WamEnumConstant(28) EMAIL,
    @WamEnumConstant(29) CHANNELS,
    @WamEnumConstant(30) COVER_PHOTO,
    @WamEnumConstant(31) GROUP_CREATION
}
