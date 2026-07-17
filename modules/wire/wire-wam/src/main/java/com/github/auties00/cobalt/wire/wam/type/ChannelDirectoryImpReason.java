package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelDirectoryImpReason")
@WamEnum
public enum ChannelDirectoryImpReason {
    @WamEnumConstant(1) PILL_SELECTION,
    @WamEnumConstant(2) COUNTRY_SELECTION,
    @WamEnumConstant(3) NOT_APPLICABLE
}
