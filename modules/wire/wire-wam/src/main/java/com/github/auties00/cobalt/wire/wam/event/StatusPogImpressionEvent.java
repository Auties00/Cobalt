package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusRowSection;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusPogImpressionWamEvent")
@WamEvent(id = 8302)
public interface StatusPogImpressionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong pogIndex();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> statusGroupId();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> statusPosterHashId();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> statusPosterId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<StatusRowSection> statusViewEntrypoint();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<TsSurface> tsSurface();
}
