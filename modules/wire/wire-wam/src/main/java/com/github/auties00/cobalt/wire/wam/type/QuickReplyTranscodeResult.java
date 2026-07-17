package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuickReplyTranscodeResult")
@WamEnum
public enum QuickReplyTranscodeResult {
    @WamEnumConstant(1) QUICK_REPLY_TRANSCODE_RESULT_OK,
    @WamEnumConstant(2) QUICK_REPLY_TRANSCODE_RESULT_CANCELLED,
    @WamEnumConstant(3) QUICK_REPLY_TRANSCODE_RESULT_FAIL_IMAGE_UNKNOWN,
    @WamEnumConstant(4) QUICK_REPLY_TRANSCODE_RESULT_FAIL_IMAGE_ENCODING,
    @WamEnumConstant(5) QUICK_REPLY_TRANSCODE_RESULT_FAIL_IMAGE_FILE_COPY,
    @WamEnumConstant(6) QUICK_REPLY_TRANSCODE_RESULT_FAIL_VIDEO_UNKNOWN
}
