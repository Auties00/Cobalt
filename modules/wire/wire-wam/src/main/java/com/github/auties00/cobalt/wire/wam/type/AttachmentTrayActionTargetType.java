package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAttachmentTrayActionTargetType")
@WamEnum
public enum AttachmentTrayActionTargetType {
    @WamEnumConstant(1) ATTACHMENT_TRAY,
    @WamEnumConstant(2) DOCUMENT,
    @WamEnumConstant(3) CAMERA,
    @WamEnumConstant(4) CAMERA_LIBRARY,
    @WamEnumConstant(5) GALLERY,
    @WamEnumConstant(6) PHOTO_AND_VIDEO_LIBRARY,
    @WamEnumConstant(7) AUDIO,
    @WamEnumConstant(8) LOCATION,
    @WamEnumConstant(9) CONTACT,
    @WamEnumConstant(10) POLL,
    @WamEnumConstant(11) PAYMENT,
    @WamEnumConstant(12) SHOP,
    @WamEnumConstant(13) ORDER,
    @WamEnumConstant(14) CATALOG,
    @WamEnumConstant(15) QUICK_REPLY,
    @WamEnumConstant(16) STICKER_MAKER,
    @WamEnumConstant(17) IMAGINE_EDIT,
    @WamEnumConstant(18) EVENT_CREATION,
    @WamEnumConstant(19) CALL_LINK_CREATION,
    @WamEnumConstant(20) SHARE_UPI_QR,
    @WamEnumConstant(21) MUSIC,
    @WamEnumConstant(22) DRAWING
}
