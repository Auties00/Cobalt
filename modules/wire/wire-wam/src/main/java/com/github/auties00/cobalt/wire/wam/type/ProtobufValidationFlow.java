package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumProtobufValidationFlow")
@WamEnum
public enum ProtobufValidationFlow {
    @WamEnumConstant(0) STANZA_MESSAGE_RECEIVE,
    @WamEnumConstant(1) STANZA_MESSAGE_SEND,
    @WamEnumConstant(2) NOTIFICATION_EXTENSION_RECEIVE,
    @WamEnumConstant(3) HISTORY_SYNC_RECEIVE,
    @WamEnumConstant(4) STANZA_PSA_MESSAGE_RECEIVE,
    @WamEnumConstant(5) FUTUREPROOF_PROCESSING,
    @WamEnumConstant(6) CROSS_PLATFORM_MIGRATION_RECEIVE,
    @WamEnumConstant(7) HISTORY_SYNC_SEND
}
