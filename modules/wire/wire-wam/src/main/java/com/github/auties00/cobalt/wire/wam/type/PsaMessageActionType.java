package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsaMessageActionType")
@WamEnum
public enum PsaMessageActionType {
    @WamEnumConstant(1) SAVE,
    @WamEnumConstant(2) FORWARD,
    @WamEnumConstant(3) REACT,
    @WamEnumConstant(4) LINK_CLICK,
    @WamEnumConstant(5) MEDIA_PLAY,
    @WamEnumConstant(6) DELETE,
    @WamEnumConstant(7) PUSH_NOTIFICATION_CLICK,
    @WamEnumConstant(8) PUSH_NOTIFICATION_RENDER
}
