package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessInteractionEntryPointSource")
@WamEnum
public enum BusinessInteractionEntryPointSource {
    @WamEnumConstant(1) CLICK_TO_CHAT_LINK,
    @WamEnumConstant(2) MESSAGE_SHORT_LINK,
    @WamEnumConstant(3) QR_CODE,
    @WamEnumConstant(4) CUSTOM_LINK,
    @WamEnumConstant(5) CUSTOM_QR_CODE_LINK
}
