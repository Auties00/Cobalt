package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchUjCriteriaType")
@WamEnum
public enum SearchUjCriteriaType {
    @WamEnumConstant(1) TEXT_MATCH_FILTER,
    @WamEnumConstant(2) UNREAD_FILTER,
    @WamEnumConstant(3) MEDIA_TYPE_FILTER,
    @WamEnumConstant(4) CONTACT_FILTER,
    @WamEnumConstant(5) NON_CONTACTS_FILTER,
    @WamEnumConstant(6) BUSINESS_FILTER,
    @WamEnumConstant(7) OTHER
}
