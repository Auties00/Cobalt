package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CallResultType;
import com.github.auties00.cobalt.wire.wam.type.CallSide;
import com.github.auties00.cobalt.wire.wam.type.LobbyEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.LobbyExitType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebJoinableCallWamEvent")
@WamEvent(id = 2572)
public interface JoinableCallEvent extends WamEventSpec {
    @WamProperty(index = 23, type = WamType.TIMER)
    Optional<Instant> acceptAckLatencyMs();

    @WamProperty(index = 51, type = WamType.STRING)
    Optional<String> callLinkRandomId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> callRandomId();

    @WamProperty(index = 31, type = WamType.STRING)
    Optional<String> callReplayerId();

    @WamProperty(index = 41, type = WamType.ENUM)
    Optional<CallSide> callSide();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> groupAcceptNoCriticalGroupUpdate();

    @WamProperty(index = 38, type = WamType.TIMER)
    Optional<Instant> groupAcceptToCriticalGroupUpdateMs();

    @WamProperty(index = 42, type = WamType.BOOLEAN)
    Optional<Boolean> hasScheduleExactAlarmPermission();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> hasSpamDialog();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> isCallFull();

    @WamProperty(index = 55, type = WamType.BOOLEAN)
    Optional<Boolean> isDeviceSwitch();

    @WamProperty(index = 50, type = WamType.BOOLEAN)
    Optional<Boolean> isEventsLink();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> isFromCallLink();

    @WamProperty(index = 45, type = WamType.BOOLEAN)
    Optional<Boolean> isLidCall();

    @WamProperty(index = 39, type = WamType.BOOLEAN)
    Optional<Boolean> isLinkCreator();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> isLinkJoin();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> isLinkedGroupCall();

    @WamProperty(index = 54, type = WamType.BOOLEAN)
    Optional<Boolean> isOneOnOneCall();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isPendingCall();

    @WamProperty(index = 46, type = WamType.BOOLEAN)
    Optional<Boolean> isPhashBased();

    @WamProperty(index = 48, type = WamType.BOOLEAN)
    Optional<Boolean> isPhashMismatch();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isRejoin();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isRering();

    @WamProperty(index = 40, type = WamType.BOOLEAN)
    Optional<Boolean> isScheduledCall();

    @WamProperty(index = 56, type = WamType.BOOLEAN)
    Optional<Boolean> isTransferRejoin();

    @WamProperty(index = 47, type = WamType.BOOLEAN)
    Optional<Boolean> isUpgradedGroupCallBeforeConnected();

    @WamProperty(index = 43, type = WamType.BOOLEAN)
    Optional<Boolean> isVoiceChat();

    @WamProperty(index = 52, type = WamType.BOOLEAN)
    Optional<Boolean> isWaitingRoomEnabled();

    @WamProperty(index = 34, type = WamType.TIMER)
    Optional<Instant> joinAckLatencyMs();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> joinableAcceptBeforeLobbyAck();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> joinableDuringCall();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> joinableEndCallBeforeLobbyAck();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<CallResultType> legacyCallResult();

    @WamProperty(index = 19, type = WamType.TIMER)
    Optional<Instant> lobbyAckLatencyMs();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<LobbyEntryPointType> lobbyEntryPoint();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<LobbyExitType> lobbyExit();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong lobbyExitNackCode();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong lobbyOpenDurationMs();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> lobbyQueryWhileConnected();

    @WamProperty(index = 7, type = WamType.TIMER)
    Optional<Instant> lobbyVisibleT();

    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> nseEnabled();

    @WamProperty(index = 28, type = WamType.TIMER)
    Optional<Instant> nseOfflineQueueMs();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong numConnectedPeers();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong numInvitedParticipants();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong numOutgoingRingingPeers();

    @WamProperty(index = 35, type = WamType.TIMER)
    Optional<Instant> queryAckLatencyMs();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong randomScheduledId();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> receivedByNse();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> rejoinMissingDbMapping();

    @WamProperty(index = 53, type = WamType.INTEGER)
    OptionalLong timeInWaitingRoomMs();

    @WamProperty(index = 36, type = WamType.TIMER)
    Optional<Instant> timeSinceAcceptMs();

    @WamProperty(index = 21, type = WamType.TIMER)
    Optional<Instant> timeSinceLastClientPollMinutes();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> videoEnabled();
}
