package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBotBizType")
@WamEnum
public enum BotBizType {
    @WamEnumConstant(1) BOT_BIZ_3P,
    @WamEnumConstant(2) BOT_BIZ_1P
}
