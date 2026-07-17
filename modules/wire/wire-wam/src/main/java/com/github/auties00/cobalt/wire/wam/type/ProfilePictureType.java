package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumProfilePictureType")
@WamEnum
public enum ProfilePictureType {
    @WamEnumConstant(1) PHOTO_CAMERA,
    @WamEnumConstant(2) PHOTO_UPLOAD,
    @WamEnumConstant(3) WEB_SEARCH,
    @WamEnumConstant(4) EMOJI,
    @WamEnumConstant(5) STICKER,
    @WamEnumConstant(6) REMOVE_PHOTO
}
