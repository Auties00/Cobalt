package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLobbyEntryPointType")
@WamEnum
public enum LobbyEntryPointType {
    @WamEnumConstant(0) ERROR,
    @WamEnumConstant(1) NOTIFICATION_MESSAGE,
    @WamEnumConstant(2) NOTIFICATION_BUTTON,
    @WamEnumConstant(3) CALL_LOG,
    @WamEnumConstant(4) NOTIFICATION_ONGOING,
    @WamEnumConstant(5) NOT_OPENED,
    @WamEnumConstant(6) OPEN_FOR_RINGING,
    @WamEnumConstant(7) IN_APP_NOTIFICATION,
    @WamEnumConstant(8) LINKED_GROUP_CALL_SYSTEM_MESSAGE,
    @WamEnumConstant(9) CHAT_HEADER,
    @WamEnumConstant(10) QUICK_CONTACT_DIALOG,
    @WamEnumConstant(11) GROUP_CHAT_INFO,
    @WamEnumConstant(12) CALL_LINK_CALL_LOG,
    @WamEnumConstant(13) CALL_LINK_LOG_INFO,
    @WamEnumConstant(14) CALL_LINK_INTERNAL,
    @WamEnumConstant(15) CALL_LINK_EXTERNAL,
    @WamEnumConstant(16) CALL_LINK_CREATE,
    @WamEnumConstant(17) CALL_LINK_INDIVIDUAL_CHAT,
    @WamEnumConstant(18) CALL_LINK_GROUP_CHAT,
    @WamEnumConstant(19) UPCOMING_CALL_LOG,
    @WamEnumConstant(20) LOBBY_SWITCH,
    @WamEnumConstant(21) SCHEDULED_CREATION_MESSAGE,
    @WamEnumConstant(22) CALL_LOG_MESSAGE_ONGOING,
    @WamEnumConstant(23) APP_TILE_CONTEXT_MENU,
    @WamEnumConstant(24) EVENT_INDIVIDUAL_CHAT,
    @WamEnumConstant(25) EVENT_GROUP_CHAT,
    @WamEnumConstant(26) VOICE_CHAT_MINI_PLAYER,
    @WamEnumConstant(27) SECOND_NOTIFICATION,
    @WamEnumConstant(28) START_CALL,
    @WamEnumConstant(29) CALL_LINK_CREATOR_PUSH_NOTIFICATION,
    @WamEnumConstant(30) LINK_CREATOR_CALL_STARTED_PUSH_NOTIFICATION,
    @WamEnumConstant(31) LINK_CREATOR_CALL_CONNECTED_PUSH_NOTIFICATION,
    @WamEnumConstant(32) UPCOMING_SCHEDULE_CALL,
    @WamEnumConstant(33) UPCOMING_SCHEDULE_CALL_LIST,
    @WamEnumConstant(34) EVENT_MESSAGE,
    @WamEnumConstant(35) VOICE_CHAT_WAVE_IN_APP_NOTIFICATION,
    @WamEnumConstant(36) VOICE_CHAT_WAVE_IN_APP_NOTIFICATION_BUTTON,
    @WamEnumConstant(37) UPCOMING_EVENT_BANNER,
    @WamEnumConstant(38) WAITING_ROOM_JOINED_PUSH_NOTIFICATION,
    @WamEnumConstant(39) WAITING_ROOM_MULTIPLE_JOINED_PUSH_NOTIFICATION,
    @WamEnumConstant(40) CALL_TRANSFER_PUSH_NOTIFICATION
}
