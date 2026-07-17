package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusPostResult")
@WamEnum
public enum StatusPostResult {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(2) CANCELLED,
    @WamEnumConstant(3) ERROR_UNKNOWN,
    @WamEnumConstant(4) MEDIA_ERROR_REQUEST,
    @WamEnumConstant(5) MEDIA_ERROR_UPLOAD,
    @WamEnumConstant(6) MEDIA_ERROR_OOM,
    @WamEnumConstant(7) MEDIA_ERROR_IO,
    @WamEnumConstant(8) MEDIA_ERROR_NO_PERMISSIONS,
    @WamEnumConstant(9) MEDIA_ERROR_BAD_MEDIA,
    @WamEnumConstant(10) MEDIA_ERROR_INSUFFICIENT_SPACE,
    @WamEnumConstant(11) MEDIA_ERROR_FNF,
    @WamEnumConstant(12) MEDIA_ERROR_CANCEL,
    @WamEnumConstant(13) MEDIA_ERROR_SERVER,
    @WamEnumConstant(14) MEDIA_ERROR_REQUEST_TIMEOUT,
    @WamEnumConstant(15) MEDIA_ERROR_NOT_FINALIZED,
    @WamEnumConstant(16) MEDIA_ERROR_OPTIMISTIC_HASH,
    @WamEnumConstant(17) MEDIA_ERROR_MEDIA_CONN,
    @WamEnumConstant(18) MEDIA_ERROR_DNS,
    @WamEnumConstant(19) MEDIA_ERROR_THROTTLE,
    @WamEnumConstant(20) MEDIA_ERROR_SSL,
    @WamEnumConstant(21) MEDIA_ERROR_NO_CLIENT_NETWORK,
    @WamEnumConstant(22) MEDIA_DUPLICATE,
    @WamEnumConstant(23) MEDIA_SWITCH_REQUIRED,
    @WamEnumConstant(24) MEDIA_TOO_LARGE,
    @WamEnumConstant(25) MEDIA_ERROR_WAMSYS,
    @WamEnumConstant(26) MEDIA_ERROR_CONN,
    @WamEnumConstant(27) MEDIA_ERROR_URL,
    @WamEnumConstant(28) MEDIA_ERROR_NO_SUCH_ALGORITHM,
    @WamEnumConstant(29) MEDIA_ERROR_OPTIMISTIC_NETWORK_UNSAFE,
    @WamEnumConstant(30) MEDIA_ERROR_TRANSCODING_UKNOWN,
    @WamEnumConstant(31) MEDIA_ERROR_FILE_FORMAT_UNSUPPORTED,
    @WamEnumConstant(32) MEDIA_ERROR_TOO_LARG,
    @WamEnumConstant(33) MEDIA_ERROR_UNKNOWN,
    @WamEnumConstant(34) MEDIA_SKIPPED_EP_NO_PRIMARY_HOST,
    @WamEnumConstant(35) MEDIA_ERROR_CRONET,
    @WamEnumConstant(36) MEDIA_ERROR_NO_DIRECT_PATH,
    @WamEnumConstant(37) ERROR_NETWORK,
    @WamEnumConstant(38) ERROR_EXPIRED,
    @WamEnumConstant(39) ERROR_UPLOAD,
    @WamEnumConstant(40) ERROR_BACKFILL_USYNC_FAILED,
    @WamEnumConstant(41) ERROR_PAYLOAD_TOO_BIG,
    @WamEnumConstant(42) ERROR_LOCATION,
    @WamEnumConstant(43) ERROR_INVALID_MESSAGE,
    @WamEnumConstant(44) ERROR_E2EE,
    @WamEnumConstant(45) ERROR_INVALID_PROTOBUF,
    @WamEnumConstant(46) SERVER_ERROR,
    @WamEnumConstant(47) EPHEMERALLY_EXPIRED,
    @WamEnumConstant(48) USER_CANCELLED,
    @WamEnumConstant(49) USER_DELETED_UNSENT_MESSAGE,
    @WamEnumConstant(50) USER_MANUAL_RETRY,
    @WamEnumConstant(51) ERROR_CLIENT_OUT_OF_MEMORY
}
