package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureAction")
@WamEnum
public enum DisclosureAction {
    @WamEnumConstant(0) SCREEN_VIEW,
    @WamEnumConstant(1) CLICK_ON_CONTINUE,
    @WamEnumConstant(2) CANCEL,
    @WamEnumConstant(3) BACK_BUTTON_TOOLBAR,
    @WamEnumConstant(4) BACK_BUTTON_SYSTEM,
    @WamEnumConstant(5) DISMISS,
    @WamEnumConstant(6) DISCLOSURE_INFO_VIEW
}
