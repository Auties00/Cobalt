package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MigrationStageEnum;
import com.github.auties00.cobalt.wire.wam.type.StageFailureReasonEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLid11MigrationLifecycleWamEvent")
@WamEvent(id = 6154)
public interface Lid11MigrationLifecycleEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong chatNotInMappingCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong companionHasADifferentMappingCount();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong currentLocalTimeSeconds();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong fakeLidCount();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isLocally1x1MigratedFromDb();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isStageInOfflineResume();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isSyncdLidSession();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong latestMappingCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mappingCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong migratedThreadCount();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<MigrationStageEnum> migrationStage();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong peerMappingBytesLength();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong primaryMigrationTimeSeconds();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<StageFailureReasonEnum> stageFailureReason();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> webClientDidPairingStanzaIndicated1x1MigrationThisSession();
}
