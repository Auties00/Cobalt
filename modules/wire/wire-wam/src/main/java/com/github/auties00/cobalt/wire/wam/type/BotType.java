package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBotType")
@WamEnum
public enum BotType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) METABOT,
    @WamEnumConstant(2) BOT_1P_BIZ,
    @WamEnumConstant(3) BOT_3P_BIZ,
    @WamEnumConstant(4) UGC,
    @WamEnumConstant(5) META_CHARACTER,
    @WamEnumConstant(6) TEE_BOT,
    @WamEnumConstant(7) HATCH,
    @WamEnumConstant(8) MANUS
}
