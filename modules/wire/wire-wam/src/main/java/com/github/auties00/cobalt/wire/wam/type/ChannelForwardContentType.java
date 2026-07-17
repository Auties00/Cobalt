package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelForwardContentType")
@WamEnum
public enum ChannelForwardContentType {
    @WamEnumConstant(0) UPDATE,
    @WamEnumConstant(1) UPDATE_CARD
}
