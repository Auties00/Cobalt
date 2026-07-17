package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcQueryTriggerType")
@WamEnum
public enum WebcQueryTriggerType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) USER_SCROLL,
    @WamEnumConstant(2) NEW_MESSAGE_PREFETCH,
    @WamEnumConstant(3) SEARCH_RESULT_CLICK
}
