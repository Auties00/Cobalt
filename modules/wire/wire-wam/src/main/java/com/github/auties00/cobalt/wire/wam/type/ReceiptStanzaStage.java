package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReceiptStanzaStage")
@WamEnum
public enum ReceiptStanzaStage {
    @WamEnumConstant(0) OVERALL,
    @WamEnumConstant(6) WAITING_IN_E2E_QUEUE,
    @WamEnumConstant(7) WAITING_IN_UNORDERED_QUEUE,
    @WamEnumConstant(1) PARSE,
    @WamEnumConstant(2) WAITING_TO_PROCESS,
    @WamEnumConstant(3) PROCESS,
    @WamEnumConstant(5) WAITING_TO_ACK,
    @WamEnumConstant(4) ACK
}
