package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbPerCustomerDataSharingControlAction")
@WamEnum
public enum SmbPerCustomerDataSharingControlAction {
    @WamEnumConstant(0) SYSTEM_MESSAGE_INSERTED,
    @WamEnumConstant(1) CONSENT_SCREEN_VIEW,
    @WamEnumConstant(2) CONSENT_SCREEN_CONFIRM,
    @WamEnumConstant(3) CONSENT_SCREEN_CANCEL,
    @WamEnumConstant(4) CONTACT_TOGGLE_CHANGED
}
