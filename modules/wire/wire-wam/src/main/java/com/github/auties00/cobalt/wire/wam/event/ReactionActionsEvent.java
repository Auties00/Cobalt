package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.ReactionActionType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebReactionActionsWamEvent")
@WamEvent(id = 3184, betaWeight = 10, releaseWeight = 20)
public interface ReactionActionsEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ReactionActionType> reactionAction();
}
