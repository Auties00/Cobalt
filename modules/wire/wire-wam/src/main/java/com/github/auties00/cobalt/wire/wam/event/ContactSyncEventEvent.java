package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ContactSyncSource;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebContactSyncEventWamEvent")
@WamEvent(id = 1006, betaWeight = 20, releaseWeight = 100)
public interface ContactSyncEventEvent extends WamEventSpec {
    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong contactSyncBusinessResponseNew();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong contactSyncChangedVersionRowCount();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong contactSyncConsecutiveCount();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong contactSyncDeviceResponseNew();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong contactSyncDisappearingModeResponseNew();

    @WamProperty(index = 23, type = WamType.TIMER)
    Optional<Instant> contactSyncEndTimestamp();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong contactSyncErrorCode();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong contactSyncFailureProtocol();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncIsMultiIq();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong contactSyncLatency();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong contactSyncMultiIqCompletedPages();

    @WamProperty(index = 35, type = WamType.INTEGER)
    OptionalLong contactSyncMultiIqFailedPageIndex();

    @WamProperty(index = 36, type = WamType.INTEGER)
    OptionalLong contactSyncMultiIqSessionDurationMs();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong contactSyncMultiIqTotalPages();

    @WamProperty(index = 38, type = WamType.INTEGER)
    OptionalLong contactSyncNewLidToPnMappings();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong contactSyncNewLidToUsernameMappings();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncNoop();

    @WamProperty(index = 40, type = WamType.INTEGER)
    OptionalLong contactSyncOsImportedContactsToRemove();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong contactSyncPayResponseNew();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncRequestClearWaSyncData();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncRequestIsUrgent();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong contactSyncRequestOrigin();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong contactSyncRequestPreparationLatency();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong contactSyncRequestProtocol();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong contactSyncRequestRetryCount();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncRequestShouldRetry();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong contactSyncRequestedCount();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong contactSyncResponseCount();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong contactSyncSidelistRequestedCount();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong contactSyncSidelistResponseCount();

    @WamProperty(index = 24, type = WamType.ENUM)
    Optional<ContactSyncSource> contactSyncSource();

    @WamProperty(index = 25, type = WamType.TIMER)
    Optional<Instant> contactSyncStartTimestamp();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong contactSyncStatusResponseNew();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncSuccess();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> contactSyncType();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong contactSyncTypeCode();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncTypeIsBackground();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncTypeIsFull();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncTypeIsMetadata();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> contactSyncTypeIsSnapshot();
}
