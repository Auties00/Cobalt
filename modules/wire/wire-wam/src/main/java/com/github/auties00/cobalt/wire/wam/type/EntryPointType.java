package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEntryPointType")
@WamEnum
public enum EntryPointType {
    @WamEnumConstant(1) MAIN_SCREEN,
    @WamEnumConstant(2) CONTACT_INFO
}
