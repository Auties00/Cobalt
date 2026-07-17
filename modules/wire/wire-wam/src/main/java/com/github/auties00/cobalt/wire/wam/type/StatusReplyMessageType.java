package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusReplyMessageType")
@WamEnum
public enum StatusReplyMessageType {
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) TEXT,
    @WamEnumConstant(3) IMAGE,
    @WamEnumConstant(4) VOICE,
    @WamEnumConstant(5) DOCUMENT,
    @WamEnumConstant(6) AUDIO,
    @WamEnumConstant(7) STICKER,
    @WamEnumConstant(8) LOCATION,
    @WamEnumConstant(9) PRODUCT,
    @WamEnumConstant(10) CONTACT,
    @WamEnumConstant(11) CONTACT_ARRAY,
    @WamEnumConstant(12) CAMERA_CAPTURE_IMAGE,
    @WamEnumConstant(13) MEDIA_GALLERY,
    @WamEnumConstant(14) GIF_VIDEO,
    @WamEnumConstant(15) QUICK_REPLY,
    @WamEnumConstant(16) POLL,
    @WamEnumConstant(17) AVATAR_QUICK_REPLY,
    @WamEnumConstant(18) STICKER_QUICK_REPLY
}
