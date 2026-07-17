package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebThreadInteractionDataVoipWamEvent")
@WamEvent(id = 6362)
public interface ThreadInteractionDataVoipEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong callOffersReceived();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong callOffersSent();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong callsResultBusy();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong callsResultCancelled();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong callsResultConnected();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong callsResultError();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong callsResultMissed();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong callsResultRejected();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> threadDs();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> threadIdByLid();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong totalCallDuration();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong videoCallsOffered();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong voiceCallsOffered();
}
