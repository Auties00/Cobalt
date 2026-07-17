package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCompanionAddContactSource")
@WamEnum
public enum CompanionAddContactSource {
    @WamEnumConstant(0) CONTACT_INFO,
    @WamEnumConstant(1) VCARD,
    @WamEnumConstant(2) CONTACT_LIST,
    @WamEnumConstant(3) NEW_CHAT,
    @WamEnumConstant(4) PHONE_NUMBER_DIALER,
    @WamEnumConstant(5) FMX_CARD,
    @WamEnumConstant(6) NEW_CHAT_DRAWER,
    @WamEnumConstant(7) CHAT_HEADER,
    @WamEnumConstant(8) GROUP_MEMBER,
    @WamEnumConstant(9) CHAT_LIST_GLOBAL_SEARCH
}
