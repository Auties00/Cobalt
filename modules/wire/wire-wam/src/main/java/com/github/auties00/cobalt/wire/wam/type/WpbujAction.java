package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWpbujAction")
@WamEnum
public enum WpbujAction {
    @WamEnumConstant(1) VIEW,
    @WamEnumConstant(2) DOWNLOAD,
    @WamEnumConstant(3) APPLY,
    @WamEnumConstant(4) SELECT
}
