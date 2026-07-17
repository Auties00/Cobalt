package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageSendResultType")
@WamEnum
public enum MessageSendResultType {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(3) ERROR_UNKNOWN,
    @WamEnumConstant(4) ERROR_NETWORK,
    @WamEnumConstant(5) ERROR_EXPIRED,
    @WamEnumConstant(6) ERROR_CANCELLED,
    @WamEnumConstant(7) ERROR_UPLOAD,
    @WamEnumConstant(8) ERROR_BACKFILL_USYNC_FAILED,
    @WamEnumConstant(9) ERROR_PAYLOAD_TOO_BIG,
    @WamEnumConstant(10) ERROR_LOCATION,
    @WamEnumConstant(11) ERROR_INVALID_MESSAGE,
    @WamEnumConstant(12) ERROR_E2EE,
    @WamEnumConstant(13) ERROR_INVALID_PROTOBUF,
    @WamEnumConstant(14) SERVER_ERROR,
    @WamEnumConstant(15) EPHEMERALLY_EXPIRED,
    @WamEnumConstant(16) MEDIA_UPLOAD_FAILED,
    @WamEnumConstant(17) ERROR_CLIENT_OUT_OF_MEMORY,
    @WamEnumConstant(18) ERROR_UPLOAD_CANCELLED_MANUALLY,
    @WamEnumConstant(19) ERROR_UPLOAD_CANCELLED_AUTOMATIC,
    @WamEnumConstant(20) ERROR_BOUNDED_STANZA_TOO_LARGE,
    @WamEnumConstant(21) AEA_SEND_RECONCILATION_FAILURE
}
