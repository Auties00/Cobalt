package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CtwaDirectionFrom;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCtwa3pdConversionWamEvent")
@WamEvent(id = 5138)
public interface Ctwa3pdConversionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> ctwa3pdConversionMetadata();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> ctwa3pdConversionSubtype();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> ctwa3pdConversionType();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong ctwa3pdSchemaVersion();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> ctwa3pdSurfaceType();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong ctwaConversationDepth();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong ctwaConversationRepeat();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<CtwaDirectionFrom> ctwaDirectionFrom();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> ctwaSignals();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> ctwaTrackingPayload();
}
