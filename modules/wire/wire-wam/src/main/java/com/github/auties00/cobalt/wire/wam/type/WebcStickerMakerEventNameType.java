package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcStickerMakerEventNameType")
@WamEnum
public enum WebcStickerMakerEventNameType {
    @WamEnumConstant(0) STICKER_MAKER_BUTTON_TAP,
    @WamEnumConstant(1) IMAGE_UPLOADED,
    @WamEnumConstant(2) IMAGE_CROPPED,
    @WamEnumConstant(3) EMOJI_ADDED,
    @WamEnumConstant(4) STICKER_ADDED,
    @WamEnumConstant(5) TEXT_ADDED,
    @WamEnumConstant(6) IMAGE_OUTLINED,
    @WamEnumConstant(7) SEND_STICKER
}
