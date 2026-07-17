package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPromptTriggerPoint")
@WamEnum
public enum PromptTriggerPoint {
    @WamEnumConstant(1) USER_INPUT,
    @WamEnumConstant(2) CONVERSATION_STARTER
}
