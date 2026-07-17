package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaymentsVerifyCardResultType")
@WamEnum
public enum PaymentsVerifyCardResultType {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(2) ERROR_GENERIC,
    @WamEnumConstant(3) DEBIT_CARD_INVALID,
    @WamEnumConstant(4) EXPIRATION_IN_PAST,
    @WamEnumConstant(5) EXPIRATION_EMPTY,
    @WamEnumConstant(6) EXPIRATION_INVALID,
    @WamEnumConstant(7) MONTH_INVALID,
    @WamEnumConstant(8) YEAR_INVALID
}
