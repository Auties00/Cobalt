package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStickerErrorType")
@WamEnum
public enum StickerErrorType {
    @WamEnumConstant(2) DECOMPRESSION,
    @WamEnumConstant(3) SENDER_VALIDATION,
    @WamEnumConstant(4) RECEIVER_VALIDATION
}
