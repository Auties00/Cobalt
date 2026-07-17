package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.FavoritesUpdateEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMessagingFavoritesUpdateWamEvent")
@WamEvent(id = 5460)
public interface MessagingFavoritesUpdateEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong contactFavCountAfterUpdate();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong contactFavCountBeforeUpdate();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<FavoritesUpdateEntryPoint> favoritesUpdateEntryPoint();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong groupFavCountAfterUpdate();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong groupFavCountBeforeUpdate();
}
