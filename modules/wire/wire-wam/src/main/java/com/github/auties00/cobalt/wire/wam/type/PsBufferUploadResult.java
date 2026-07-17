package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsBufferUploadResult")
@WamEnum
public enum PsBufferUploadResult {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) ERROR_PARSING,
    @WamEnumConstant(3) ERROR_DECODING,
    @WamEnumConstant(4) ERROR_CREDENTIAL,
    @WamEnumConstant(5) ERROR_OTHER,
    @WamEnumConstant(6) ERROR_CLIENT_NETWORK,
    @WamEnumConstant(7) ERROR_WAIT_FOR_TOKEN,
    @WamEnumConstant(8) ERROR_SERVER_OTHER,
    @WamEnumConstant(9) SKIPPED_NO_NETWORK,
    @WamEnumConstant(10) SKIPPED_NO_DATA,
    @WamEnumConstant(11) ERROR_ACCESS_TOKEN
}
