package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ApplicationState;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapHistoryPayloadType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapPayloadType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapStepResult;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdBootstrapAppStateDataDownloadedWamEvent")
@WamEvent(id = 2294)
public interface MdBootstrapAppStateDataDownloadedEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<ApplicationState> applicationState();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> historySyncRetryRequestId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong mdBootstrapContactsCount();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<MdBootstrapHistoryPayloadType> mdBootstrapHistoryPayloadType();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong mdBootstrapPayloadSize();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MdBootstrapPayloadType> mdBootstrapPayloadType();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mdBootstrapStepDuration();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<MdBootstrapStepResult> mdBootstrapStepResult();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> mdRegAttemptId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> mdSessionId();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong mdStorageQuotaBytes();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong mdStorageQuotaUsedBytes();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> mdSyncFailureReason();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong mdTimestamp();
}
