package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelEntryPointApp")
@WamEnum
public enum ChannelEntryPointApp {
    @WamEnumConstant(1) EXTERNAL_UNKNOWN,
    @WamEnumConstant(2) WHATSAPP
}
