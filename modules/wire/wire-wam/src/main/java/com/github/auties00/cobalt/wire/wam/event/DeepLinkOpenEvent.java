package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkOpenFrom;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkType;
import com.github.auties00.cobalt.wire.wam.type.OwnerType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebDeepLinkOpenWamEvent")
@WamEvent(id = 2136)
public interface DeepLinkOpenEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> campaign();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DeepLinkOpenFrom> deepLinkOpenFrom();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> deepLinkSessionId();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<DeepLinkType> deepLinkType();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isContact();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<OwnerType> linkOwnerType();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong sourceSurface();
}
