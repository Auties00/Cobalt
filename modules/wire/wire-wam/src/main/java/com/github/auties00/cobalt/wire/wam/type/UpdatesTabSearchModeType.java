package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUpdatesTabSearchModeType")
@WamEnum
public enum UpdatesTabSearchModeType {
    @WamEnumConstant(0) QUERY,
    @WamEnumConstant(1) RECENT
}
