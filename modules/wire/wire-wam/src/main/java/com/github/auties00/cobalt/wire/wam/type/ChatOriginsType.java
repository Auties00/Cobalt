package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatOriginsType")
@WamEnum
public enum ChatOriginsType {
    @WamEnumConstant(1) LID_USERNAME,
    @WamEnumConstant(2) LID_CTWA,
    @WamEnumConstant(3) OTHERS
}
