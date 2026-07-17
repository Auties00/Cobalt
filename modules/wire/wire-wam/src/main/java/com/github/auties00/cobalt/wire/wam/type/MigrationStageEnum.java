package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMigrationStageEnum")
@WamEnum
public enum MigrationStageEnum {
    @WamEnumConstant(1) GOT_ABPROP,
    @WamEnumConstant(2) PRIMARY_LOCAL_MIGRATION_STARTED,
    @WamEnumConstant(3) PRIMARY_LOCAL_MIGRATION_ENDED,
    @WamEnumConstant(4) PRIMARY_LOCAL_MIGRATION_FAILED,
    @WamEnumConstant(5) PRIMARY_SENT_PEER_MESSAGE,
    @WamEnumConstant(6) COMPANION_RECEIVED_PEER_MESSAGE,
    @WamEnumConstant(7) COMPANION_LOCAL_MIGRATION_STARTED,
    @WamEnumConstant(8) COMPANION_LOCAL_MIGRATION_ENDED,
    @WamEnumConstant(9) COMPANION_LOCAL_MIGRATION_FAILED,
    @WamEnumConstant(10) COMPANION_MIGRATED_ON_NEW_PAIRING,
    @WamEnumConstant(11) COMPANION_RECEIVED_DEVICE_CAPABILITY,
    @WamEnumConstant(12) COMPANION_EXTRACTED_AND_SAVED_PEER_MESSAGE
}
