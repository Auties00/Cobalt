package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumProfileEntryPoint")
@WamEnum
public enum ProfileEntryPoint {
    @WamEnumConstant(1) CONTACT_CARD,
    @WamEnumConstant(2) CHAT_HEADER,
    @WamEnumConstant(3) CHAT_MENU,
    @WamEnumConstant(4) GROUP_MEMBERSHIP_SCREEN,
    @WamEnumConstant(5) STATUS,
    @WamEnumConstant(6) CALLS,
    @WamEnumConstant(7) BROADCAST_LIST,
    @WamEnumConstant(8) PRODUCT,
    @WamEnumConstant(9) CATALOG,
    @WamEnumConstant(10) SETTINGS,
    @WamEnumConstant(11) SPAM_BLOCK,
    @WamEnumConstant(12) CHATS_HOME,
    @WamEnumConstant(13) SHOPS,
    @WamEnumConstant(14) MENTION,
    @WamEnumConstant(15) EPHEMERAL_SETTINGS_MESSAGE,
    @WamEnumConstant(16) MAP,
    @WamEnumConstant(17) SEARCH,
    @WamEnumConstant(18) PAYMENT_TRANSACTION_DETAILS,
    @WamEnumConstant(19) CUSTOM_URL_LINK,
    @WamEnumConstant(20) CUSTOM_URL_QR_CODE,
    @WamEnumConstant(21) NOTIFICATION_BLOCK_ACTION,
    @WamEnumConstant(22) REPORT_TO_ADMIN_PARTICIPANTS_SCREEN,
    @WamEnumConstant(23) MISSED_CALL_NOTIFICATION_BLOCK_ACTION,
    @WamEnumConstant(24) INTEROP,
    @WamEnumConstant(25) FORWARDED_BIZ_MSG_DIRECT_TAP,
    @WamEnumConstant(26) FORWARDED_BIZ_MSG_CHAT_HEADER,
    @WamEnumConstant(27) ADD_MSG_TO_NOTE_TOAST,
    @WamEnumConstant(28) SENDER_PROFILE_PICTURE_IN_GROUP_CHAT,
    @WamEnumConstant(29) IMMERSIVE_CHAT_HEADER,
    @WamEnumConstant(30) FORWARD_ATTRIBUTION,
    @WamEnumConstant(31) CUSTOM_PHONE_NUMBER_LINK,
    @WamEnumConstant(32) META_AI_THREAD_LIST
}
