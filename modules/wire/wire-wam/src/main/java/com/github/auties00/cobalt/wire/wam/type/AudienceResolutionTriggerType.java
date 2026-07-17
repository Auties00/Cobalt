package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAudienceResolutionTriggerType")
@WamEnum
public enum AudienceResolutionTriggerType {
    @WamEnumConstant(0) USER_VIEW,
    @WamEnumConstant(1) PERIODIC_REFRESH
}
