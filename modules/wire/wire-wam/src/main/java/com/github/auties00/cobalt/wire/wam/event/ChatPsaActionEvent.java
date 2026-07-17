package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PsaMessageActionType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebChatPsaActionWamEvent")
@WamEvent(id = 3572)
public interface ChatPsaActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> psaCampaignId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PsaMessageActionType> psaMessageActionType();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> psaMsgId();
}
