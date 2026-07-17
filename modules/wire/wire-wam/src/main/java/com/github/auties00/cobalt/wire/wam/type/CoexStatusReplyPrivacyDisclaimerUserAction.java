package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCoexStatusReplyPrivacyDisclaimerUserAction")
@WamEnum
public enum CoexStatusReplyPrivacyDisclaimerUserAction {
    @WamEnumConstant(1) DISPLAYED,
    @WamEnumConstant(2) TAPPED
}
