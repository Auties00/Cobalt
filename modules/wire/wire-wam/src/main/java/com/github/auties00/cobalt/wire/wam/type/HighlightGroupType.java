package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumHighlightGroupType")
@WamEnum
public enum HighlightGroupType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) SINGLE,
    @WamEnumConstant(2) CREATOR,
    @WamEnumConstant(3) ADMIN,
    @WamEnumConstant(4) SAVED_CONTACTS,
    @WamEnumConstant(5) PARTICIPANTS,
    @WamEnumConstant(6) MORE
}
