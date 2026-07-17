package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallStanzaType")
@WamEnum
public enum CallStanzaType {
    @WamEnumConstant(0) OFFER,
    @WamEnumConstant(1) ACCEPT,
    @WamEnumConstant(2) REJECT,
    @WamEnumConstant(3) VIDEO,
    @WamEnumConstant(4) TERMINATE,
    @WamEnumConstant(5) ENC_REKEY,
    @WamEnumConstant(6) RELAYLATENCY,
    @WamEnumConstant(7) TRANSPORT,
    @WamEnumConstant(8) PREACCEPT,
    @WamEnumConstant(9) GROUP_UPDATE,
    @WamEnumConstant(10) MUTE_V2,
    @WamEnumConstant(11) INTERRUPTION,
    @WamEnumConstant(12) FLOWCONTROL,
    @WamEnumConstant(13) NOTIFY,
    @WamEnumConstant(14) OFFER_NOTICE,
    @WamEnumConstant(15) CALL_RELAY,
    @WamEnumConstant(16) MUTE,
    @WamEnumConstant(17) SCREEN_SHARE,
    @WamEnumConstant(18) UNKNOWN
}
