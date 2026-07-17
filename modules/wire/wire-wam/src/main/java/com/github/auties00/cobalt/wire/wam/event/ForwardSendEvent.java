package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingChatInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.E2eCiphertextType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityTriggerActionType;
import com.github.auties00.cobalt.wire.wam.type.ForwardOrigin;
import com.github.auties00.cobalt.wire.wam.type.ForwardPickerOrigin;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageBizType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebForwardSendWamEvent")
@WamEvent(id = 1728)
public interface ForwardSendEvent extends WamEventSpec {
    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<DisappearingChatInitiatorType> disappearingChatInitiator();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<E2eCiphertextType> e2eCiphertextType();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong e2eCiphertextVersion();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong ephemeralityDuration();

    @WamProperty(index = 24, type = WamType.ENUM)
    Optional<EphemeralityInitiatorType> ephemeralityInitiator();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<EphemeralityTriggerActionType> ephemeralityTriggerAction();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> fastForwardEnabled();

    @WamProperty(index = 28, type = WamType.ENUM)
    Optional<ForwardOrigin> forwardOrigin();

    @WamProperty(index = 27, type = WamType.ENUM)
    Optional<ForwardPickerOrigin> forwardPickerOrigin();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> isForwardedForward();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isFrequentlyForwarded();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> mediaCaptionPresent();

    @WamProperty(index = 26, type = WamType.ENUM)
    Optional<MessageBizType> messageBizType();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> messageForwardAgeT();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsFanout();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsFastForward();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsInternational();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 13, type = WamType.TIMER)
    Optional<Instant> messageSendT();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong receiverDefaultDisappearingDuration();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong resendCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong senderDefaultDisappearingDuration();

    @WamProperty(index = 23, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> wouldBeFrequentlyForwardedAt3();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> wouldBeFrequentlyForwardedAt4();
}
