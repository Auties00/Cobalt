package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumProjectCode")
@WamEnum
public enum ProjectCode {
    @WamEnumConstant(1) DIT,
    @WamEnumConstant(2) DIRECTORY_SEARCH
}
