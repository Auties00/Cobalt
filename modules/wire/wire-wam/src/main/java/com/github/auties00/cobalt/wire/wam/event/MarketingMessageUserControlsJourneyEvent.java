package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MmUserControlsAction;
import com.github.auties00.cobalt.wire.wam.type.MmUserControlsEntryPoint;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMarketingMessageUserControlsJourneyWamEvent")
@WamEvent(id = 6070, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface MarketingMessageUserControlsJourneyEvent extends WamEventSpec {
    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong businessPhoneNumber();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> isSuccess();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MmUserControlsAction> mmUserControlsAction();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MmUserControlsEntryPoint> mmUserControlsEntryPoint();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> mmUserControlsErrorType();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong mmUserControlsRolloutVariant();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong sequenceNumber();

    @WamProperty(index = 10, type = WamType.TIMER)
    Optional<Instant> stopDuration();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> templateId();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
