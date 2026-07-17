package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedInteractionReferral")
@WamEnum
public enum MetaVerifiedInteractionReferral {
    @WamEnumConstant(1) CHAT_PROFILE,
    @WamEnumConstant(2) CONTACT_CARD,
    @WamEnumConstant(3) SETTINGS,
    @WamEnumConstant(4) BUSINESS_TOOLS,
    @WamEnumConstant(5) NOTIFICATION,
    @WamEnumConstant(6) FMX_CONTACT_CARD,
    @WamEnumConstant(7) INCOMING_CALL_NOTIFICATION,
    @WamEnumConstant(8) INCOMING_CALL_SCREEN
}
