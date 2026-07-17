package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelLinkShareDirection")
@WamEnum
public enum ChannelLinkShareDirection {
    @WamEnumConstant(1) WHATSAPP,
    @WamEnumConstant(2) STATUS,
    @WamEnumConstant(3) EXTERNAL,
    @WamEnumConstant(4) QR_CODE
}
