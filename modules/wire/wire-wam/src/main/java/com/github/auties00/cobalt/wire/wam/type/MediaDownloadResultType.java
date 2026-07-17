package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMediaDownloadResultType")
@WamEnum
public enum MediaDownloadResultType {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(2) ERROR_UNKNOWN,
    @WamEnumConstant(3) ERROR_TIMEOUT,
    @WamEnumConstant(4) ERROR_DNS,
    @WamEnumConstant(5) ERROR_INSUFFICIENT_SPACE,
    @WamEnumConstant(6) ERROR_TOO_OLD,
    @WamEnumConstant(7) ERROR_CANNOT_RESUME,
    @WamEnumConstant(8) ERROR_HASH_MISMATCH,
    @WamEnumConstant(9) ERROR_INVALID_URL,
    @WamEnumConstant(10) ERROR_OUTPUT_STREAM,
    @WamEnumConstant(11) ERROR_CANCEL,
    @WamEnumConstant(12) DEDUPED,
    @WamEnumConstant(14) ERROR_ENC_HASH_MISMATCH,
    @WamEnumConstant(15) PREFETCH_END,
    @WamEnumConstant(16) ERROR_CANCEL_PROGRAMMATIC,
    @WamEnumConstant(17) ERROR_MEDIA_CONN,
    @WamEnumConstant(18) ERROR_THROTTLE,
    @WamEnumConstant(19) ERROR_SSL,
    @WamEnumConstant(20) ERROR_NETWORK,
    @WamEnumConstant(21) ERROR_CONNECT,
    @WamEnumConstant(22) ERROR_EP_NOTIFY_DECRYPTION_FAILURE,
    @WamEnumConstant(23) SKIPPED_EP_DIFFERENT_POP,
    @WamEnumConstant(24) SKIPPED_EP_AUTODOWNLOAD_DISABLED,
    @WamEnumConstant(25) ERROR_SERVER,
    @WamEnumConstant(26) ERROR_WATLS,
    @WamEnumConstant(27) ERROR_INVALID_CODE,
    @WamEnumConstant(28) ERROR_WAMSYS,
    @WamEnumConstant(29) ERROR_GENERIC,
    @WamEnumConstant(30) ERROR_CRONET,
    @WamEnumConstant(31) ERROR_NO_CLIENT_NETWORK,
    @WamEnumConstant(32) ERROR_HOST_SWITCH_REQUIRED,
    @WamEnumConstant(33) ERROR_SUSPICIOUS_CONTENT,
    @WamEnumConstant(34) ERROR_NO_ENCRYPTION_ALGORITHM,
    @WamEnumConstant(35) ERROR_NO_ENCRYPTED_HASH,
    @WamEnumConstant(36) ERROR_NO_MEDIA_HASH,
    @WamEnumConstant(37) ERROR_NO_MEDIA_KEY,
    @WamEnumConstant(38) ERROR_NO_SIDECAR,
    @WamEnumConstant(39) ERROR_HASH_VERIFICATION_FAILURE,
    @WamEnumConstant(40) INTEGRITY_CHECK_FAILURE
}
