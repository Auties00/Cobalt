package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPnhActionType")
@WamEnum
public enum PnhActionType {
    @WamEnumConstant(1) SEE_MASKED_PN_AT_CHAT_CREATION,
    @WamEnumConstant(2) REQUEST_DIALOG_APPEAR,
    @WamEnumConstant(3) SEND_REQUEST,
    @WamEnumConstant(4) SHARE_PN_SHEET_APPEAR,
    @WamEnumConstant(5) DISMISS,
    @WamEnumConstant(6) SHARE_NUMBER
}
