package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebtpEventType")
@WamEnum
public enum WebtpEventType {
    @WamEnumConstant(1) OPEN,
    @WamEnumConstant(2) CLOSE,
    @WamEnumConstant(3) DOWNLOAD_DOCUMENT_CLICK,
    @WamEnumConstant(4) EDIT_MENU_CLICK,
    @WamEnumConstant(5) ERROR,
    @WamEnumConstant(6) TELEMETRY,
    @WamEnumConstant(7) APP_PERF_DATA,
    @WamEnumConstant(8) PDF_SHARER_OPEN,
    @WamEnumConstant(9) PDF_SHARER_CANCEL,
    @WamEnumConstant(10) PDF_SHARER_CONTINUE,
    @WamEnumConstant(11) PDF_SHARER_SUCCESS,
    @WamEnumConstant(12) PDF_SHARER_ERROR,
    @WamEnumConstant(13) PDF_SHARER_UNSUPPORTED,
    @WamEnumConstant(14) PDF_RECEIVER_OPEN,
    @WamEnumConstant(15) PDF_RECEIVER_SUCCESS,
    @WamEnumConstant(16) PDF_RECEIVER_ERROR,
    @WamEnumConstant(17) PDF_RECEIVER_CANCEL,
    @WamEnumConstant(18) PDF_RECEIVER_CONTINUE,
    @WamEnumConstant(19) PDF_RECEIVER_FILE_FORWARDED,
    @WamEnumConstant(20) PDF_SHARER_AUTO_PROCEED_CHECKBOX,
    @WamEnumConstant(21) PDF_SHARER_CONTINUE_AUTO_PROCEED
}
