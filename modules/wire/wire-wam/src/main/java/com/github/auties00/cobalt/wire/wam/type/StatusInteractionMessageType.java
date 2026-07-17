package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusInteractionMessageType")
@WamEnum
public enum StatusInteractionMessageType {
    @WamEnumConstant(1) LIKE,
    @WamEnumConstant(2) EMOJI,
    @WamEnumConstant(3) TEXT,
    @WamEnumConstant(4) IMAGE,
    @WamEnumConstant(5) VOICE,
    @WamEnumConstant(6) STICKER,
    @WamEnumConstant(7) LOCATION,
    @WamEnumConstant(8) CONTACT,
    @WamEnumConstant(9) CAMERA_CAPTURE_IMAGE
}
