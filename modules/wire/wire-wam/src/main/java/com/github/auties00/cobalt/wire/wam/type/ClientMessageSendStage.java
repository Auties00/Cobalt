package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumClientMessageSendStage")
@WamEnum
public enum ClientMessageSendStage {
    @WamEnumConstant(1) CLIENT_RENDERED,
    @WamEnumConstant(2) CLIENT_SAVED,
    @WamEnumConstant(3) CLIENT_WRITTEN_WIRE,
    @WamEnumConstant(4) CLIENT_QUEUED,
    @WamEnumConstant(5) CLIENT_WAITING_TO_ENCRYPT,
    @WamEnumConstant(6) CLIENT_READY_TO_SEND,
    @WamEnumConstant(7) CLIENT_ENCRYPT,
    @WamEnumConstant(8) CLIENT_PREKEYS_FETCH
}
