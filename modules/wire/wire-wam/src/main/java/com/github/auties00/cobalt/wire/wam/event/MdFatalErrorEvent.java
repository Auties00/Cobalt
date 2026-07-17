package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.Collection;
import com.github.auties00.cobalt.wire.wam.type.IsPureSyncdSessionEnum;
import com.github.auties00.cobalt.wire.wam.type.LidMigrationStageEnum;
import com.github.auties00.cobalt.wire.wam.type.MdCompanionDeviceAccountType;
import com.github.auties00.cobalt.wire.wam.type.MdSyncdFatalErrorCode;
import com.github.auties00.cobalt.wire.wam.type.MdSyncdFatalErrorSource;
import com.github.auties00.cobalt.wire.wam.type.RecoveryStatusEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdFatalErrorWamEvent")
@WamEvent(id = 2304)
public interface MdFatalErrorEvent extends WamEventSpec {
    @WamProperty(index = 46, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 47, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 41, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<Collection> collection();

    @WamProperty(index = 42, type = WamType.STRING)
    Optional<String> companionSessionIds();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> currentPrimaryAppVersion();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong daysSinceLastPeriodicSync();

    @WamProperty(index = 38, type = WamType.BOOLEAN)
    Optional<Boolean> didBootstrapFromSnapshot();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong hoursSinceFirstFiniteFailure();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isBootstrap();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> isFatal();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> isPatchSenderPrimary();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> isPreviousPatchAnIncomingPatch();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> isPreviousPatchSenderPrimary();

    @WamProperty(index = 48, type = WamType.ENUM)
    Optional<IsPureSyncdSessionEnum> isPureSyncdSession();

    @WamProperty(index = 39, type = WamType.BOOLEAN)
    Optional<Boolean> isSenderIndexSameAsPreviousSenderIndex();

    @WamProperty(index = 40, type = WamType.BOOLEAN)
    Optional<Boolean> isThereAnotherSyncdCompanion();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> isWebLthashConsistent();

    @WamProperty(index = 49, type = WamType.ENUM)
    Optional<LidMigrationStageEnum> lidMigrationStage();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalCollectionNameMismatch();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalCurrentLthashMismatch();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalDidPreviousPatchFailPostSaveValidation();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalDidUseMacFetchFallback();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalFirstTwoBytesFromAHashOfSnapshotMacKeyMismatch();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalHasMissingRemove();

    @WamProperty(index = 21, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalNewLthashMismatch();

    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalNewLthashSubtractMismatch();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalNumberAddMismatch();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalNumberHasOverrideMutation();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong macFatalNumberNumAddMutation();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong macFatalNumberNumRemoveMutation();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalNumberOverrideMismatch();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalNumberRemoveMismatch();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalPatchVersionMismatch();

    @WamProperty(index = 34, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalPreviousPatchNewLthashToCurrentPatchCurrentLthash();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> macFatalSenderCurrentLthashToLocalCalculatedCurrentLthashMismatch();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong mailboxAgeDays();

    @WamProperty(index = 43, type = WamType.ENUM)
    Optional<MdCompanionDeviceAccountType> mdCompanionDeviceAccountType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MdSyncdFatalErrorCode> mdFatalErrorCode();

    @WamProperty(index = 44, type = WamType.STRING)
    Optional<String> mdRegAttemptId();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong patchSnapshotMutationCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong patchVersion();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong recoveryRequestDurationMs();

    @WamProperty(index = 51, type = WamType.ENUM)
    Optional<RecoveryStatusEnum> recoveryStatus();

    @WamProperty(index = 45, type = WamType.INTEGER)
    OptionalLong seqNumber();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> sessionStartPrimaryAppVersion();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<MdSyncdFatalErrorSource> sourceType();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong timeSincePairingMs();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong timeSinceRefreshMs();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong timeSinceTabTakeoverMs();
}
