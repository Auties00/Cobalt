package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcRmrReasonCode")
@WamEnum
public enum WebcRmrReasonCode {
    @WamEnumConstant(0) OTHER,
    @WamEnumConstant(1) MSG_CLICK,
    @WamEnumConstant(2) STICKER_PANEL_ICON,
    @WamEnumConstant(3) MEDIA_VIEWER,
    @WamEnumConstant(4) VIDEO_STREAMING,
    @WamEnumConstant(5) STATUS_V3,
    @WamEnumConstant(6) MULTI_SELECT_DOWNLOAD,
    @WamEnumConstant(7) DOCUMENT_DOWNLOAD,
    @WamEnumConstant(8) PIP,
    @WamEnumConstant(9) STICKER_PANEL_STICKER,
    @WamEnumConstant(10) SEND_TO_CHAT,
    @WamEnumConstant(11) UPLOAD,
    @WamEnumConstant(12) MSG_INIT,
    @WamEnumConstant(13) MSG_UPDATE,
    @WamEnumConstant(14) MSG_DELETE,
    @WamEnumConstant(15) MSG_RENDER
}
