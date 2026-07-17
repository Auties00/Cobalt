package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdChatAssignmentAssignmentType")
@WamEnum
public enum MdChatAssignmentAssignmentType {
    @WamEnumConstant(0) ASSIGNED,
    @WamEnumConstant(1) UNASSIGNED
}
