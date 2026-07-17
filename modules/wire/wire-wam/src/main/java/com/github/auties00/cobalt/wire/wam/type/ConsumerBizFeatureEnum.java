package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumConsumerBizFeatureEnum")
@WamEnum
public enum ConsumerBizFeatureEnum {
    @WamEnumConstant(0) BUSINESS_SEARCH,
    @WamEnumConstant(1) CONTACT_SHARING,
    @WamEnumConstant(2) BUSINESS_PROFILE,
    @WamEnumConstant(3) AUTHORIZED_AGENT
}
