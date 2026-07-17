package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReportingTokenValidationFailureReason")
@WamEnum
public enum ReportingTokenValidationFailureReason {
    @WamEnumConstant(0) MISSING_MESSAGE_SECRET,
    @WamEnumConstant(1) EMPTY_REPORTING_TOKEN_CONTENT,
    @WamEnumConstant(2) MISMATCH_REPORTING_TOKEN,
    @WamEnumConstant(3) UNSUPPORTED_VERSION,
    @WamEnumConstant(4) GROUP_HISTORY_MESSAGE_MISSING_FROM_PUBLIC_STANZA
}
