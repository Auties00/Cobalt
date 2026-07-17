package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVoMessageType")
@WamEnum
public enum VoMessageType {
    @WamEnumConstant(1) PHOTO,
    @WamEnumConstant(2) VIDEO,
    @WamEnumConstant(3) PTT
}
