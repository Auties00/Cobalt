package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelEntryPoint")
@WamEnum
public enum ChannelEntryPoint {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) UPDATES_TAB,
    @WamEnumConstant(2) DIRECTORY,
    @WamEnumConstant(3) LINK,
    @WamEnumConstant(4) DEEPLINK,
    @WamEnumConstant(5) FORWARDED_MESSAGE,
    @WamEnumConstant(6) DIRECTORY_SEARCH,
    @WamEnumConstant(7) RECOMMENDED_LIST,
    @WamEnumConstant(8) NOTIFICATION,
    @WamEnumConstant(9) UPDATES_TAB_SEARCH,
    @WamEnumConstant(10) STATUS,
    @WamEnumConstant(11) ADMIN_INVITE_MESSAGE,
    @WamEnumConstant(12) MEDIA_BROWSER,
    @WamEnumConstant(13) SIMILAR_CHANNEL,
    @WamEnumConstant(14) DIRECTORY_CATEGORIES,
    @WamEnumConstant(15) DIRECTORY_CATEGORIES_SEARCH,
    @WamEnumConstant(16) NEWSLETTER_MEDIA_GALLERY_MEDIA,
    @WamEnumConstant(17) NEWSLETTER_MEDIA_GALLERY_LINKS,
    @WamEnumConstant(18) THREAD_CHAIN_PILL,
    @WamEnumConstant(19) THREAD_CHAIN_SWIPE_UP,
    @WamEnumConstant(20) RECENT_SEARCHES,
    @WamEnumConstant(21) NEWSLETTER_CREATION_UPDATES_TAB,
    @WamEnumConstant(22) NEWSLETTER_CREATION_DIRECTORY,
    @WamEnumConstant(23) NEWSLETTER_CREATION_DIRECTORY_CATEGORIES,
    @WamEnumConstant(24) INVITE_CONTACTS_TO_FOLLOW_MESSAGE,
    @WamEnumConstant(25) MUSIC_ATTRIBUTION_BOTTOM_SHEET_FROM_CHAT,
    @WamEnumConstant(26) CHANNEL_NOTIFICATIONS_SETTINGS
}
