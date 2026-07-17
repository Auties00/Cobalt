package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupJoinRequestEntrypointType")
@WamEnum
public enum GroupJoinRequestEntrypointType {
    @WamEnumConstant(1) SYSTEM_MESSAGE,
    @WamEnumConstant(2) CONVERSATION_BANNER,
    @WamEnumConstant(3) GROUP_INFO,
    @WamEnumConstant(4) NOTIFICATION
}
