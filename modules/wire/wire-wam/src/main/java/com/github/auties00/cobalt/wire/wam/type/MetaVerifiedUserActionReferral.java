package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedUserActionReferral")
@WamEnum
public enum MetaVerifiedUserActionReferral {
    @WamEnumConstant(1) SETTINGS,
    @WamEnumConstant(2) BUSINESS_TOOLS,
    @WamEnumConstant(3) NOTIFICATION,
    @WamEnumConstant(4) META_VERIFIED_HOME,
    @WamEnumConstant(5) CHANNEL_INFO,
    @WamEnumConstant(6) CHANNEL_ADMIN_CARD,
    @WamEnumConstant(7) LINKED_DEVICES,
    @WamEnumConstant(8) PRIVACY_INTERSTITIAL,
    @WamEnumConstant(9) BUSINESS_INFO,
    @WamEnumConstant(10) BUSINESS_CARD,
    @WamEnumConstant(11) LINKED_DEVICES_LIST,
    @WamEnumConstant(12) LINKED_DEVICES_EDU_SCREEN,
    @WamEnumConstant(13) PROTECTED_BUSINESS_ACCOUNTS_EMPTY_LIST,
    @WamEnumConstant(14) BUSINESS_CONTACTS_LIST,
    @WamEnumConstant(15) PROTECTED_BUSINESS_ACCOUNTS_LIST,
    @WamEnumConstant(16) DEEPLINK,
    @WamEnumConstant(17) QUICK_PROMOTION,
    @WamEnumConstant(18) BUSINESS_BROADCAST_HOME,
    @WamEnumConstant(19) BUSINESS_BROADCAST_THREAD,
    @WamEnumConstant(20) MESSAGE_CAPPING_NUX,
    @WamEnumConstant(21) MESSAGE_CAPPING_USAGE_STATS,
    @WamEnumConstant(22) CHAT_THREADS,
    @WamEnumConstant(23) NEW_CHAT_MESSAGE_HOME,
    @WamEnumConstant(24) BUSINESS_BROADCAST_REVIEW_SCREEN,
    @WamEnumConstant(25) BUSINESS_BROADCAST_MESSAGE_CREDITS_STORE
}
