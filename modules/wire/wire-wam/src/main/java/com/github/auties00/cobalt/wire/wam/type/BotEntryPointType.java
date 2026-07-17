package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBotEntryPointType")
@WamEnum
public enum BotEntryPointType {
    @WamEnumConstant(1) WA_CHAT,
    @WamEnumConstant(2) STATUS,
    @WamEnumConstant(3) NEW_CHAT,
    @WamEnumConstant(4) INVOKE,
    @WamEnumConstant(5) SHARED_AI,
    @WamEnumConstant(6) NOTIFICATION,
    @WamEnumConstant(7) BANNER,
    @WamEnumConstant(8) AI_CHATS_LIST_BUTTON,
    @WamEnumConstant(9) AI_CONTACT_ON_WA,
    @WamEnumConstant(10) DEEPLINK_USER_SHARED,
    @WamEnumConstant(11) DEEPLINK_QP,
    @WamEnumConstant(12) AI_SEARCH_CHATS_LIST,
    @WamEnumConstant(13) CHAT_INFO_PAGE,
    @WamEnumConstant(14) AI_VOICE,
    @WamEnumConstant(15) AI_CHAT_SHORTCUT,
    @WamEnumConstant(16) AI_IMAGINE_BOTTOM_SHEET,
    @WamEnumConstant(17) AI_HOME_PREVIEW,
    @WamEnumConstant(18) AI_SEARCH_CHATS_LIST_VOICE,
    @WamEnumConstant(19) AI_CHAT_THREAD_VOICE,
    @WamEnumConstant(20) AI_FAB_VOICE,
    @WamEnumConstant(21) CHAT_INFO_SETTINGS,
    @WamEnumConstant(22) CHAT_THREAD,
    @WamEnumConstant(23) AI_MEMORY_BOTTOM_SHEET,
    @WamEnumConstant(24) FORWARD,
    @WamEnumConstant(25) AI_MEMORY_SYSTEM_MESSAGE,
    @WamEnumConstant(26) AI_WIDGET,
    @WamEnumConstant(27) SHARE,
    @WamEnumConstant(28) AI_NEW_FAB_VOICE,
    @WamEnumConstant(29) AI_TAB_DISCOVERY,
    @WamEnumConstant(30) CHAT_LIST,
    @WamEnumConstant(31) AI_TAB,
    @WamEnumConstant(32) AI_HOME_IN_TAB,
    @WamEnumConstant(33) AI_NULL_STATE,
    @WamEnumConstant(34) AI_STUDIO_CREATION,
    @WamEnumConstant(35) AI_STUDIO_PROFILE_EDIT,
    @WamEnumConstant(36) BOT_SETTINGS,
    @WamEnumConstant(37) META_AI_LONG_PRESS_CONTEXT_MENU,
    @WamEnumConstant(38) FAB,
    @WamEnumConstant(39) AI_NEW_FAB_VOICE_CALL_HISTORY,
    @WamEnumConstant(40) AI_STUDIO_CREATION_FAB,
    @WamEnumConstant(41) INVOKE_META_AI_1ON1,
    @WamEnumConstant(42) INVOKE_META_AI_GROUP,
    @WamEnumConstant(43) AI_STUDIO_YOUR_AI,
    @WamEnumConstant(44) ASK_META_AI_CONTEXT_MENU,
    @WamEnumConstant(45) META_AI_SETTINGS,
    @WamEnumConstant(46) AI_STICKERS_BOTTOM_SHEET,
    @WamEnumConstant(47) AI_WEB_NAVIGATION_BAR,
    @WamEnumConstant(48) AI_WEB_INTRO_PANEL
}
