package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPrivateAiFeatureName")
@WamEnum
public enum PrivateAiFeatureName {
    @WamEnumConstant(0) SUMMARIZATION,
    @WamEnumConstant(1) WRITE_WITH_AI,
    @WamEnumConstant(2) PSI,
    @WamEnumConstant(3) QUICK_RECAP,
    @WamEnumConstant(4) INCOGNITO,
    @WamEnumConstant(5) SIDE_CHAT,
    @WamEnumConstant(6) GROUP_AI,
    @WamEnumConstant(7) PSI_SEARCH
}
