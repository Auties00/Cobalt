package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdChatAssignmentChatType")
@WamEnum
public enum MdChatAssignmentChatType {
    @WamEnumConstant(0) GROUP,
    @WamEnumConstant(1) BROADCAST_LIST,
    @WamEnumConstant(2) INDIVIDUAL,
    @WamEnumConstant(3) COMMUNITY,
    @WamEnumConstant(4) CHANNEL,
    @WamEnumConstant(5) INTEROP
}
