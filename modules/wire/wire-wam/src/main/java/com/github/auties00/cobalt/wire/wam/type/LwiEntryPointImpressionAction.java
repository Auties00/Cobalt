package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiEntryPointImpressionAction")
@WamEnum
public enum LwiEntryPointImpressionAction {
    @WamEnumConstant(1) LWI_ACTION_DISMISS,
    @WamEnumConstant(2) LWI_ACTION_RECOMMENDATION_FETCH_START,
    @WamEnumConstant(3) LWI_ACTION_RECOMMENDATION_FETCH_RESPONSE,
    @WamEnumConstant(4) LWI_ACTION_RECOMMENDATION_FETCH_ERROR,
    @WamEnumConstant(5) LWI_ACTION_IMPRESSION
}
