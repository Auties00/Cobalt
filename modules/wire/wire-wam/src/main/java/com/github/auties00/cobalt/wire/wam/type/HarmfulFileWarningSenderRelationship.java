package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumHarmfulFileWarningSenderRelationship")
@WamEnum
public enum HarmfulFileWarningSenderRelationship {
    @WamEnumConstant(0) NON_CONTACT,
    @WamEnumConstant(1) CONTACT,
    @WamEnumConstant(2) SIDE_CONTACT
}
