package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSubfunnelType")
@WamEnum
public enum SubfunnelType {
    @WamEnumConstant(1) TEXT,
    @WamEnumConstant(2) PTT,
    @WamEnumConstant(3) REACTION,
    @WamEnumConstant(4) SHARE_CONTENT,
    @WamEnumConstant(5) EXPRESSION_TRAY,
    @WamEnumConstant(6) MEDIA_SHARING,
    @WamEnumConstant(7) QUOTED_MESSAGE,
    @WamEnumConstant(8) FORWARD_ACTION
}
