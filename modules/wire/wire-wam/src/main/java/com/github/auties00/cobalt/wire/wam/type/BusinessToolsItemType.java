package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessToolsItemType")
@WamEnum
public enum BusinessToolsItemType {
    @WamEnumConstant(0) OVERFLOW,
    @WamEnumConstant(1) PROFILE,
    @WamEnumConstant(2) CATALOG,
    @WamEnumConstant(3) AWAY_MESSAGE,
    @WamEnumConstant(4) GREETING_MESSAGE,
    @WamEnumConstant(5) QUICK_REPLIES,
    @WamEnumConstant(6) LABELS,
    @WamEnumConstant(7) LINKED_ACCOUNTS,
    @WamEnumConstant(8) SHORT_LINK,
    @WamEnumConstant(9) STATISTICS,
    @WamEnumConstant(10) CREATE_ACCOUNT_LINK,
    @WamEnumConstant(11) MANAGE_CTA,
    @WamEnumConstant(12) CREATE_AD,
    @WamEnumConstant(13) CONTINUE_PROMPT,
    @WamEnumConstant(14) ADVERTISE_LIST_ITEM,
    @WamEnumConstant(15) SHOPS,
    @WamEnumConstant(16) DISABLED_CATALOG,
    @WamEnumConstant(17) BANNED_SHOP,
    @WamEnumConstant(18) PREMIUM_TOOLS,
    @WamEnumConstant(19) BUSINESS_DIRECTORY,
    @WamEnumConstant(20) MANAGE_ADS,
    @WamEnumConstant(21) META_VERIFIED,
    @WamEnumConstant(22) AI_FROM_META,
    @WamEnumConstant(23) ORDERS,
    @WamEnumConstant(24) MARKETING_MESSAGES,
    @WamEnumConstant(25) BUSINESS_BROADCAST,
    @WamEnumConstant(26) PAYMENTS_HOME,
    @WamEnumConstant(27) MISSED_CALL_FOLLOWUP
}
