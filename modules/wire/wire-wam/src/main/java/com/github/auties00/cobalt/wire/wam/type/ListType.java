package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumListType")
@WamEnum
public enum ListType {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) UNREAD,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) FAVORITE,
    @WamEnumConstant(4) PREDEFINED,
    @WamEnumConstant(5) CUSTOM,
    @WamEnumConstant(6) COMMUNITY,
    @WamEnumConstant(7) BUSINESS_AI,
    @WamEnumConstant(8) DRAFTS,
    @WamEnumConstant(9) CAMPAIGN_REPLIES,
    @WamEnumConstant(10) SERVER_ASSIGNED
}
