package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.AgentEngagementEnumType;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;
import com.github.auties00.cobalt.wire.wam.type.UiActionChatType;
import com.github.auties00.cobalt.wire.wam.type.UiActionType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebUiActionWamEvent")
@WamEvent(id = 472, betaWeight = 100, releaseWeight = 5000)
public interface UiActionEvent extends WamEventSpec {
    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<AgentEngagementEnumType> agentEngagementType();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<BotType> botType();

    @WamProperty(index = 17, type = WamType.TIMER)
    Optional<Instant> dbBgThreadReadsDurationT();

    @WamProperty(index = 18, type = WamType.TIMER)
    Optional<Instant> dbBgThreadWritesDurationT();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong dbMainThreadCount();

    @WamProperty(index = 19, type = WamType.TIMER)
    Optional<Instant> dbMainThreadReadsDurationT();

    @WamProperty(index = 20, type = WamType.TIMER)
    Optional<Instant> dbMainThreadWritesDurationT();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong dbReadsCount();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong dbWritesCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong deviceCount();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> isLowPowerMode();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<AddressingMode> localAddressingMode();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong participantCount();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> peripheralConnected();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<SizeBucket> sizeBucket();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<UiActionChatType> uiActionChatType();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> uiActionPreloaded();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> uiActionPresentationSource();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> uiActionT();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> uiActionTtrcSurfaceName();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<UiActionType> uiActionType();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
