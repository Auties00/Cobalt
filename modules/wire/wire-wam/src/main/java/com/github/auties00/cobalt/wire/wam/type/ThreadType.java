package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumThreadType")
@WamEnum
public enum ThreadType {
    @WamEnumConstant(1) GROUP,
    @WamEnumConstant(2) BROADCAST,
    @WamEnumConstant(3) INDIVIDUAL,
    @WamEnumConstant(4) STATUS,
    @WamEnumConstant(5) CHANNEL,
    @WamEnumConstant(6) SUB_GROUP,
    @WamEnumConstant(7) DEFAULT_SUB_GROUP,
    @WamEnumConstant(8) PARENT_GROUP,
    @WamEnumConstant(9) BOT
}
