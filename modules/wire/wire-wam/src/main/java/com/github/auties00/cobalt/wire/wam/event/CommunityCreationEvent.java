package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CommunityCreationActionTakenType;
import com.github.auties00.cobalt.wire.wam.type.CommunityCreationCurrentScreenType;
import com.github.auties00.cobalt.wire.wam.type.CommunityCreationEntrypointType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCommunityCreationWamEvent")
@WamEvent(id = 3492)
public interface CommunityCreationEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong communityCreationActionCount();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CommunityCreationActionTakenType> communityCreationActionTaken();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<CommunityCreationCurrentScreenType> communityCreationCurrentScreen();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<CommunityCreationEntrypointType> communityCreationEntrypoint();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> communityCreationSessionId();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> communityId();
}
