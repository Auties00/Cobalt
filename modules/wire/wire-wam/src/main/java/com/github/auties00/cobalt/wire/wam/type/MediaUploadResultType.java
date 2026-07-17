package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMediaUploadResultType")
@WamEnum
public enum MediaUploadResultType {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(3) DUPLICATE,
    @WamEnumConstant(2) ERROR_UNKNOWN,
    @WamEnumConstant(4) ERROR_REQUEST,
    @WamEnumConstant(5) ERROR_UPLOAD,
    @WamEnumConstant(6) ERROR_OOM,
    @WamEnumConstant(7) ERROR_IO,
    @WamEnumConstant(8) ERROR_NO_PERMISSIONS,
    @WamEnumConstant(9) ERROR_BAD_MEDIA,
    @WamEnumConstant(10) ERROR_INSUFFICIENT_SPACE,
    @WamEnumConstant(11) ERROR_FNF,
    @WamEnumConstant(12) ERROR_CANCEL,
    @WamEnumConstant(13) ERROR_SERVER,
    @WamEnumConstant(14) ERROR_REQUEST_TIMEOUT,
    @WamEnumConstant(15) ERROR_NOT_FINALIZED,
    @WamEnumConstant(16) ERROR_OPTIMISTIC_HASH,
    @WamEnumConstant(17) ERROR_MEDIA_CONN,
    @WamEnumConstant(18) ERROR_DNS,
    @WamEnumConstant(19) ERROR_THROTTLE,
    @WamEnumConstant(20) ERROR_SSL,
    @WamEnumConstant(21) ERROR_NO_CLIENT_NETWORK,
    @WamEnumConstant(22) SKIPPED_EP_NOT_ONLINE,
    @WamEnumConstant(23) SKIPPED_EP_NOT_1TO1CHAT,
    @WamEnumConstant(24) SKIPPED_EP_UPLOAD_FAILED,
    @WamEnumConstant(25) SKIPPED_EP_MULTI_CHAT,
    @WamEnumConstant(26) SKIPPED_EP_NO_PRIMARY_HOST,
    @WamEnumConstant(27) ERROR_CRONET,
    @WamEnumConstant(28) ERROR_INCOMPLETE_SERVER_RESPONSE,
    @WamEnumConstant(29) ERROR_TRANSCODING,
    @WamEnumConstant(30) ERROR_CANCEL_PROGRAMMATIC,
    @WamEnumConstant(31) ERROR_NO_ROUTE,
    @WamEnumConstant(32) ERROR_TOO_LARGE,
    @WamEnumConstant(33) ERROR_CANNOT_TRANSCODE,
    @WamEnumConstant(34) ERROR_UNKNOWN_MIMETYPE,
    @WamEnumConstant(35) ERROR_UNSUPPORTED_MIMETYPE,
    @WamEnumConstant(36) ERROR_SERVER_REJECTED_MEDIA,
    @WamEnumConstant(37) ERROR_IO_ENCRYPTION,
    @WamEnumConstant(38) ERROR_NO_ENCRYPTION_ALGORITHM,
    @WamEnumConstant(39) ERROR_HOST_SWITCH_REQUIRED,
    @WamEnumConstant(40) ERROR_WAMSYS,
    @WamEnumConstant(41) ERROR_INVALID_URL,
    @WamEnumConstant(42) INTEGRITY_CHECK_FAILURE
}
