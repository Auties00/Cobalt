package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ActiveTimeAfterPairing;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapHistoryPayloadType;
import com.github.auties00.cobalt.wire.wam.type.MdHistorySyncStatusResult;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdBootstrapHistorySyncStatusAfterPairingWamEvent")
@WamEvent(id = 4652)
public interface MdBootstrapHistorySyncStatusAfterPairingEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ActiveTimeAfterPairing> activeTimeAfterPairing();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isLoopRunning();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong lastProcessedNotificationChunkOrder();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong lastProcessedNotificationChunkProgress();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MdBootstrapHistoryPayloadType> mdBootstrapHistoryPayloadType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MdHistorySyncStatusResult> mdHistorySyncStatusResult();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> mdSessionId();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong mdTimestamp();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong missingNotificationCount();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong nextNotificationChunkOrder();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong totalProcessedMessageCount();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong unprocessedNotificationCount();
}
