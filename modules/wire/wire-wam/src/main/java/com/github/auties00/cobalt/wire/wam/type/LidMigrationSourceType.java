package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLidMigrationSourceType")
@WamEnum
public enum LidMigrationSourceType {
    @WamEnumConstant(1) PEER,
    @WamEnumConstant(2) HISTORY
}
