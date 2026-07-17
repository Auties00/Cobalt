package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAudienceManagementActionType")
@WamEnum
public enum AudienceManagementActionType {
    @WamEnumConstant(0) CREATED,
    @WamEnumConstant(1) RESOLVED,
    @WamEnumConstant(2) UPDATED,
    @WamEnumConstant(3) DELETED,
    @WamEnumConstant(4) SET_DYNAMIC,
    @WamEnumConstant(5) SET_EXPLICIT,
    @WamEnumConstant(6) SYNCED
}
