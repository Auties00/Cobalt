package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAudiencePredicateTypeEnum")
@WamEnum
public enum AudiencePredicateTypeEnum {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) EXPLICIT,
    @WamEnumConstant(2) CHATTED_RECENTLY,
    @WamEnumConstant(3) NOT_MESSAGED_RECENTLY,
    @WamEnumConstant(4) LARGEST_LIST,
    @WamEnumConstant(5) ALL_CONTACTS,
    @WamEnumConstant(6) LABEL
}
