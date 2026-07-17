package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusRowSection")
@WamEnum
public enum StatusRowSection {
    @WamEnumConstant(1) RECENT_STORIES,
    @WamEnumConstant(2) PREVIOUS_STORIES,
    @WamEnumConstant(3) MUTED_STORIES,
    @WamEnumConstant(4) MY_STATUS,
    @WamEnumConstant(5) CHAT_LIST,
    @WamEnumConstant(6) GROUP_PARTICIPANT,
    @WamEnumConstant(7) CONTACT_CARD,
    @WamEnumConstant(8) PROFILE_PAGE,
    @WamEnumConstant(9) CHAT_LIST_SEARCH,
    @WamEnumConstant(10) UPDATES_TAB_SEARCH,
    @WamEnumConstant(11) CHAT_TOP_BAR,
    @WamEnumConstant(12) SEE_ALL_RECENT,
    @WamEnumConstant(13) SEE_ALL_VIEWED,
    @WamEnumConstant(14) SEE_ALL_MUTED,
    @WamEnumConstant(15) SEE_ALL_SEARCH,
    @WamEnumConstant(16) NOTIFICATION,
    @WamEnumConstant(17) STATUS_VIEWER_SHEET,
    @WamEnumConstant(18) WAMO_PREVIEW,
    @WamEnumConstant(19) GROUP_CHAT_THREAD,
    @WamEnumConstant(20) GROUP_INFO_PAGE,
    @WamEnumConstant(21) GROUP_INFO_PROFILE_RING,
    @WamEnumConstant(22) GROUP_INFO_GROUP_STATUS_ROW,
    @WamEnumConstant(23) UNKNOWN,
    @WamEnumConstant(24) QUOTED_MESSAGE_IN_CHAT,
    @WamEnumConstant(25) GROUP_STATUS_TAB_SELF_POG,
    @WamEnumConstant(26) WIDGET,
    @WamEnumConstant(27) CHANNEL_INFO_PAGE,
    @WamEnumConstant(28) CHANNEL_THREAD_PAGE,
    @WamEnumConstant(29) CHANNEL_SUBSCRIBER_LIST,
    @WamEnumConstant(30) CHANNEL_RECOMMENDED_LIST,
    @WamEnumConstant(31) CHANNEL_DIRECTORY_LIST,
    @WamEnumConstant(34) CHANNEL_STATUS_MY_LIST,
    @WamEnumConstant(35) CHATS_TAB_STATUS_TRAY,
    @WamEnumConstant(36) CHANNEL_INFO_SCREEN,
    @WamEnumConstant(37) CHANNEL_THREAD_SCREEN,
    @WamEnumConstant(38) ARCHIVE_STATUS_VIEWER,
    @WamEnumConstant(39) ARCHIVE_STORAGE,
    @WamEnumConstant(40) ME_TAB
}
