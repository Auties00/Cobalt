package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumContactUsExitState")
@WamEnum
public enum ContactUsExitState {
    @WamEnumConstant(1) PROBLEM_DESCRIPTION,
    @WamEnumConstant(2) SUGGESTED_FAQ,
    @WamEnumConstant(3) EMAIL_SEND,
    @WamEnumConstant(4) IN_APP_FAQ,
    @WamEnumConstant(5) CANCELLED,
    @WamEnumConstant(6) FAQ
}
