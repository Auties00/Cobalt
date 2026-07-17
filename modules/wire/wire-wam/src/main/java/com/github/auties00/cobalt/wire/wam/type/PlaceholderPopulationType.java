package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPlaceholderPopulationType")
@WamEnum
public enum PlaceholderPopulationType {
    @WamEnumConstant(0) OTHER,
    @WamEnumConstant(1) RETRY,
    @WamEnumConstant(2) PEER_MESSAGE,
    @WamEnumConstant(3) RESEND
}
