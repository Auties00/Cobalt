package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBotPromptType")
@WamEnum
public enum BotPromptType {
    @WamEnumConstant(1) TEXT,
    @WamEnumConstant(2) VOICE,
    @WamEnumConstant(3) VOICE_BACKGROUND,
    @WamEnumConstant(4) VOICE_CONVERSATION_STARTER,
    @WamEnumConstant(5) TEXT_CONVERSATION_STARTER,
    @WamEnumConstant(6) TEXT_FROM_VOICE,
    @WamEnumConstant(8) IMAGE,
    @WamEnumConstant(9) PTT,
    @WamEnumConstant(10) COLLECTION,
    @WamEnumConstant(11) DOCUMENT
}
