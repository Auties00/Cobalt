package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaAiModelTierType")
@WamEnum
public enum MetaAiModelTierType {
    @WamEnumConstant(0) BASE,
    @WamEnumConstant(1) PREMIUM
}
