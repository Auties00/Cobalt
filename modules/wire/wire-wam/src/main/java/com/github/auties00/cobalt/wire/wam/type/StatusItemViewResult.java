package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusItemViewResult")
@WamEnum
public enum StatusItemViewResult {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(2) CANCELLED,
    @WamEnumConstant(3) ERROR_UNKNOWN,
    @WamEnumConstant(4) MEDIA_ERROR_UNKNOWN,
    @WamEnumConstant(5) MEDIA_ERROR_DNS,
    @WamEnumConstant(6) MEDIA_ERROR_TIMEOUT,
    @WamEnumConstant(7) MEDIA_ERROR_INSUFFICIENT_SPACE,
    @WamEnumConstant(8) MEDIA_ERROR_TOO_OLD,
    @WamEnumConstant(9) MEDIA_ERROR_CANNOT_RESUME,
    @WamEnumConstant(10) MEDIA_ERROR_HASH_MISMATCH,
    @WamEnumConstant(11) MEDIA_ERROR_INVALID_URL,
    @WamEnumConstant(12) MEDIA_ERROR_OUTPUT_STREAM,
    @WamEnumConstant(13) MEDIA_ERROR_MEDIA_CONN,
    @WamEnumConstant(14) MEDIA_ERROR_THROTTLE,
    @WamEnumConstant(15) MEDIA_DOWNLOAD_CANCEL,
    @WamEnumConstant(16) MEDIA_PREFETCH_END,
    @WamEnumConstant(17) MEDIA_ERROR_WATLS,
    @WamEnumConstant(18) MEDIA_ERROR_SERVER,
    @WamEnumConstant(19) MEDIA_ERROR_WAMSYS,
    @WamEnumConstant(20) MEDIA_ERROR_NETWORK,
    @WamEnumConstant(21) MEDIA_ERROR_CONNECT,
    @WamEnumConstant(22) MEDIE_HOST_SWTICH_REQUIRED,
    @WamEnumConstant(23) MEDIA_INVALID_CODE,
    @WamEnumConstant(24) MEDIA_SUSPICIOUS_CONTENT,
    @WamEnumConstant(25) MEDIA_ERROR_CRONET,
    @WamEnumConstant(26) PARTIAL_IMAGE_DOWNLOAD
}
