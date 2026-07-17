package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsGroupSafetyCheckUiInteractions")
@WamEnum
public enum PsGroupSafetyCheckUiInteractions {
    @WamEnumConstant(0) SEE_CHAT,
    @WamEnumConstant(1) EXIT_GROUP,
    @WamEnumConstant(2) EXIT_COMMUNITY,
    @WamEnumConstant(3) SEE_SCAM_EXAMPLES,
    @WamEnumConstant(4) PRIVACY_SETTINGS,
    @WamEnumConstant(5) HOW_TO_REPORT,
    @WamEnumConstant(6) DOUBLE_CHECK_LINKS,
    @WamEnumConstant(7) DISMISS,
    @WamEnumConstant(8) DRAG_DISMISS,
    @WamEnumConstant(9) X_BUTTON,
    @WamEnumConstant(10) BACK_BUTTON
}
