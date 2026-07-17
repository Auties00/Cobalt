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

@WhatsAppWebModule(moduleName = "WAWebEphemeralSyncResponseReceiveWamEvent")
@WamEvent(id = 4780)
public interface EphemeralSyncResponseReceiveEvent extends WamEventSpec {
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

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<EsrSendResultType> esrResolveResult();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> threadId();
}
