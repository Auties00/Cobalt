package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureEventType")
@WamEnum
public enum DisclosureEventType {
    @WamEnumConstant(0) CTA_URL_CLICK,
    @WamEnumConstant(1) BODY_URL_CLICK,
    @WamEnumConstant(2) BODY_URL_LONG_PRESS,
    @WamEnumConstant(3) DISCLOSURE_INFO_CLICK,
    @WamEnumConstant(4) OPEN_CHAT_WITH_UNREAD_MM,
    @WamEnumConstant(5) CTA_APP_CLICK
}
