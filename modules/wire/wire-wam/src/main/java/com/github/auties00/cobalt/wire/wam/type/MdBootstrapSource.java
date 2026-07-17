package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdBootstrapSource")
@WamEnum
public enum MdBootstrapSource {
    @WamEnumConstant(1) APP_STATE,
    @WamEnumConstant(2) HISTORY
}
