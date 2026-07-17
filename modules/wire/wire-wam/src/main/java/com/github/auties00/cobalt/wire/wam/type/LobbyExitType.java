package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLobbyExitType")
@WamEnum
public enum LobbyExitType {
    @WamEnumConstant(0) ERROR,
    @WamEnumConstant(1) CONNECTED,
    @WamEnumConstant(2) REJECTED_BY_USER,
    @WamEnumConstant(3) MISSED,
    @WamEnumConstant(4) LOBBY_NACK,
    @WamEnumConstant(5) ACCEPT_NACK,
    @WamEnumConstant(6) MISSED_NO_RING,
    @WamEnumConstant(7) LINK_QUERY_NACK,
    @WamEnumConstant(8) LINK_JOIN_NACK,
    @WamEnumConstant(9) TIMEOUT,
    @WamEnumConstant(10) LOBBY_SWITCH,
    @WamEnumConstant(11) OFFER_NACK,
    @WamEnumConstant(12) WAITING_ROOM_DENIED,
    @WamEnumConstant(13) WAITING_ROOM_LEAVE,
    @WamEnumConstant(14) WAITING_ROOM_TIMEOUT
}
