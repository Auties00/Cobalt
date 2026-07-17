package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatAssignmentChatType")
@WamEnum
public enum ChatAssignmentChatType {
    @WamEnumConstant(0) INDIVIDUAL,
    @WamEnumConstant(1) GROUP,
    @WamEnumConstant(2) COMMUNITY,
    @WamEnumConstant(3) CHANNEL
}
