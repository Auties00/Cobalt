package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelEntryPointMetadata")
@WamEnum
public enum ChannelEntryPointMetadata {
    @WamEnumConstant(1) STATUS_HEADER,
    @WamEnumConstant(2) LINK_TOOLTIP,
    @WamEnumConstant(3) LINK_BUTTON,
    @WamEnumConstant(4) POST_TOOLTIP
}
