package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchUjItemType")
@WamEnum
public enum SearchUjItemType {
    @WamEnumConstant(1) AI_SUGGESTION,
    @WamEnumConstant(2) CHAT,
    @WamEnumConstant(3) CONTACT,
    @WamEnumConstant(4) INVITE,
    @WamEnumConstant(5) MESSAGE,
    @WamEnumConstant(6) GROUP_IN_COMMON,
    @WamEnumConstant(7) MEDIA,
    @WamEnumConstant(8) BUSINESS,
    @WamEnumConstant(9) RECENT_SEARCH_INDIVIDUAL_SUGGESTION,
    @WamEnumConstant(10) RECENT_SEARCH_GROUP_SUGGESTION,
    @WamEnumConstant(11) CONTACT_FROM_COMMON_GROUP,
    @WamEnumConstant(12) UNKNOWN_CONTACT,
    @WamEnumConstant(13) COMMUNITY,
    @WamEnumConstant(14) PUSHNAME
}
