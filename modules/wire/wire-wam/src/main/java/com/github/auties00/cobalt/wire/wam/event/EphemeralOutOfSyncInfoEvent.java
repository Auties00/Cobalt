package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ClientGroupSizeBucket;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebEphemeralOutOfSyncInfoWamEvent")
@WamEvent(id = 3892)
public interface EphemeralOutOfSyncInfoEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ClientGroupSizeBucket> groupSizeBucket();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong incomingMessageEphemeralityDuration();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isNewThreadForUser();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong otherDefaultModeDuration();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong threadEphemeralityDuration();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong userDefaultModeDuration();
}
