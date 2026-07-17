package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallVideoState")
@WamEnum
public enum CallVideoState {
    @WamEnumConstant(0) DISABLED,
    @WamEnumConstant(1) ENABLED,
    @WamEnumConstant(2) PAUSED,
    @WamEnumConstant(3) UPGRADE_REQUEST,
    @WamEnumConstant(6) MUTED,
    @WamEnumConstant(10) UNKNOWN_PEER,
    @WamEnumConstant(12) XR_2D_CODEC_AVATAR_ENABLED,
    @WamEnumConstant(20) ERROR
}
