package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumContactSyncSource")
@WamEnum
public enum ContactSyncSource {
    @WamEnumConstant(1) CONTACT_FORM,
    @WamEnumConstant(2) CTWA_OPEN_DEEP_LINK,
    @WamEnumConstant(3) CTWA_POST_DEEP_LINK
}
