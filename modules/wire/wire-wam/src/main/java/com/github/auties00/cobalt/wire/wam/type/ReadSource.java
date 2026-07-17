package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReadSource")
@WamEnum
public enum ReadSource {
    @WamEnumConstant(0) OTHER,
    @WamEnumConstant(1) CHAT,
    @WamEnumConstant(2) NOTIFICATION,
    @WamEnumConstant(3) MARK_AS_READ,
    @WamEnumConstant(4) MULTIDEVICE_SYNC
}
