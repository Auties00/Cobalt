package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusInteractionActors")
@WamEnum
public enum StatusInteractionActors {
    @WamEnumConstant(1) SELF_INTERACTION,
    @WamEnumConstant(2) POSTER_VIEWER,
    @WamEnumConstant(3) OTHER_TO_OTHER
}
