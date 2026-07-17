package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumImageSearchFailedErrorType")
@WamEnum
public enum ImageSearchFailedErrorType {
    @WamEnumConstant(0) NON_200_RESPONSE,
    @WamEnumConstant(1) NON_302_RESPONSE,
    @WamEnumConstant(2) NULL_RESPONSE_BODY,
    @WamEnumConstant(3) DECODE_OR_PARSE_ERROR,
    @WamEnumConstant(4) NO_REDIRECT_URL,
    @WamEnumConstant(5) INVALID_URL,
    @WamEnumConstant(6) CONSENT_FORM_IN_URL,
    @WamEnumConstant(7) NETWORK_ERROR,
    @WamEnumConstant(8) UNKNOWN_ERROR
}
