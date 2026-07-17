package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGcInitiationType")
@WamEnum
public enum GcInitiationType {
    @WamEnumConstant(0) ONE_ON_ONE_TO_GC_UPGRADE,
    @WamEnumConstant(1) ADHOC,
    @WamEnumConstant(2) LINKED,
    @WamEnumConstant(3) CALL_LINK,
    @WamEnumConstant(4) VOICE_CHAT
}
