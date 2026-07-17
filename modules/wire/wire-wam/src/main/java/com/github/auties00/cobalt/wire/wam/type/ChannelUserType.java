package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelUserType")
@WamEnum
public enum ChannelUserType {
    @WamEnumConstant(1) OWNER,
    @WamEnumConstant(2) ADMIN,
    @WamEnumConstant(3) FOLLOWER,
    @WamEnumConstant(4) GUEST
}
