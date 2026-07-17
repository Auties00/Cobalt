package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingChatInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityTriggerActionType;
import com.github.auties00.cobalt.wire.wam.type.EsrFailureReasonType;
import com.github.auties00.cobalt.wire.wam.type.EsrSendResultType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebEphemeralSyncResponseSendWamEvent")
@WamEvent(id = 4778)
public interface EphemeralSyncResponseSendEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DisappearingChatInitiatorType> clientDisappearingModeInitiator();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong clientEphemeralityDuration();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<EphemeralityInitiatorType> clientEphemeralityInitiator();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong clientEphemeralitySettingTimestamp();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<EphemeralityTriggerActionType> clientEphemeralityTriggerAction();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<DisappearingChatInitiatorType> esrDisappearingModeInitiator();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong esrEphemeralityDuration();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<EphemeralityInitiatorType> esrEphemeralityInitiator();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong esrEphemeralitySettingTimestamp();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<EphemeralityTriggerActionType> esrEphemeralityTriggerAction();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<EsrFailureReasonType> esrFailureReason();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong esrSendAttempt();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<EsrSendResultType> esrSendResult();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 15, type = WamType.ENUM)
    Optional<DisappearingChatInitiatorType> messageDisappearingModeInitiator();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong messageEphemeralityDuration();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<EphemeralityInitiatorType> messageEphemeralityInitiator();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong messageEphemeralitySettingTimestamp();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<EphemeralityTriggerActionType> messageEphemeralityTriggerAction();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> threadId();
}
