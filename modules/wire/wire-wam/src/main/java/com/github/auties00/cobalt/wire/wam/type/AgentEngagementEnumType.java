package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAgentEngagementEnumType")
@WamEnum
public enum AgentEngagementEnumType {
    @WamEnumConstant(0) DIRECT_CHAT,
    @WamEnumConstant(1) INVOKED,
    @WamEnumConstant(2) MEMBER
}
