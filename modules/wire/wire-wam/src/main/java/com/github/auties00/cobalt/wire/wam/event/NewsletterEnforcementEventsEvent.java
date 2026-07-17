package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.EnforcementInteractionEventType;
import com.github.auties00.cobalt.wire.wam.type.EnforcementType;
import com.github.auties00.cobalt.wire.wam.type.InteractionEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.InteractionSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebNewsletterEnforcementEventsWamEvent")
@WamEvent(id = 7112)
public interface NewsletterEnforcementEventsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<EnforcementInteractionEventType> enforcementInteractionEvent();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<EnforcementType> enforcementType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<InteractionEntryPoint> interactionEntryPoint();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<InteractionSurface> interactionSurface();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong newsletterEnforcementSessionId();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> violationCategory();
}
