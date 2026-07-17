package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelDirectorySurface")
@WamEnum
public enum ChannelDirectorySurface {
    @WamEnumConstant(1) CHANNEL_DIRECTORY_CATEGORIES,
    @WamEnumConstant(2) CHANNEL_DIRECTORY
}
