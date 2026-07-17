package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLoginHostType")
@WamEnum
public enum LoginHostType {
    @WamEnumConstant(1) PUSH_OVERRIDES,
    @WamEnumConstant(2) G_WHATSAPP_NET,
    @WamEnumConstant(3) PUSH_FALLBACKS,
    @WamEnumConstant(4) G_FALLBACK_WHATSAPP_NET,
    @WamEnumConstant(5) HARDCODED_LIST,
    @WamEnumConstant(6) EX_WHATSAPP_NET,
    @WamEnumConstant(7) PROXY
}
