package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLidMigrationStageEnum")
@WamEnum
public enum LidMigrationStageEnum {
    @WamEnumConstant(1) NOT_MIGRATED,
    @WamEnumConstant(2) PEER_MIGRATED,
    @WamEnumConstant(3) HISTORY_MIGRATED
}
