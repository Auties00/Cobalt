package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupBulkRemovalEntryPoint")
@WamEnum
public enum GroupBulkRemovalEntryPoint {
    @WamEnumConstant(1) FLOOD_SYSTEM_MESSAGE
}
