package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAiChatOriginsType")
@WamEnum
public enum AiChatOriginsType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) AI_HOME,
    @WamEnumConstant(2) AI_TAB_TEXT,
    @WamEnumConstant(3) AI_TAB_VOICE
}
