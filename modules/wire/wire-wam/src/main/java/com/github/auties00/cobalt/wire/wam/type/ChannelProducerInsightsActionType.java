package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelProducerInsightsActionType")
@WamEnum
public enum ChannelProducerInsightsActionType {
    @WamEnumConstant(0) WIDGET_IMPRESSION,
    @WamEnumConstant(1) OPEN,
    @WamEnumConstant(2) CLOSE,
    @WamEnumConstant(3) NAVIGATION_TAP,
    @WamEnumConstant(4) INFO_ICON_TAP,
    @WamEnumConstant(5) LINK_CLICK
}
