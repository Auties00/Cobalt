package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUiActionChatType")
@WamEnum
public enum UiActionChatType {
    @WamEnumConstant(1) INDIVIDUAL,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) SUBGROUP,
    @WamEnumConstant(4) DEFAULT_SUBGROUP,
    @WamEnumConstant(5) CHANNEL,
    @WamEnumConstant(6) META_AI,
    @WamEnumConstant(7) AI_CHARACTERS
}
