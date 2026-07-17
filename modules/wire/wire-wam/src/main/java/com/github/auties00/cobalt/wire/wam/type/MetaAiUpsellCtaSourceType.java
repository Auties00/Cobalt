package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaAiUpsellCtaSourceType")
@WamEnum
public enum MetaAiUpsellCtaSourceType {
    @WamEnumConstant(1) PERSISTENT_CHAT_BANNER,
    @WamEnumConstant(2) PROMOTIONAL_MESSAGE,
    @WamEnumConstant(3) MUSE_SPARK_PROACTIVE_MESSAGE
}
