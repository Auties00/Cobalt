package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelDyiEventType")
@WamEnum
public enum ChannelDyiEventType {
    @WamEnumConstant(1) CHANNEL_REPORT_REQUEST,
    @WamEnumConstant(2) CHANNEL_REPORT_DOWNLOAD,
    @WamEnumConstant(3) CHANNEL_REPORT_EXPORT
}
