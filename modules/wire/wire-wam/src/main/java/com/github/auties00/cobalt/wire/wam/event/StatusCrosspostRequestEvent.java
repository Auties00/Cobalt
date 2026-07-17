package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CrosspostDestinationType;
import com.github.auties00.cobalt.wire.wam.type.CrosspostOriginType;
import com.github.auties00.cobalt.wire.wam.type.CrosspostResultType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PrivacySettingsValueType;
import com.github.auties00.cobalt.wire.wam.type.StatusCrosspostShareTypeEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusCrosspostRequestWamEvent")
@WamEvent(id = 4994)
public interface StatusCrosspostRequestEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> cacSessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CrosspostDestinationType> crosspostDestination();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> crosspostErrorType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<CrosspostOriginType> crosspostOrigin();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> defaultStatusPrivacySettings();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isAutoCrosspostEnabledInSettings();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isAutoCrossposted();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> statusCrossPostPerPostStatusPrivacySetting();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> statusCrosspostEntryPoint();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> statusCrosspostEventType();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> statusCrosspostFlowTraceId();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<MediaType> statusCrosspostMediaType();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<CrosspostResultType> statusCrosspostResult();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<StatusCrosspostShareTypeEnum> statusCrosspostShareType();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong statusCrosspostTraceId();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();
}
