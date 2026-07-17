package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SettingsClickEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.SettingsItemType;
import com.github.auties00.cobalt.wire.wam.type.SettingsPageType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebSettingsClickWamEvent")
@WamEvent(id = 2214)
public interface SettingsClickEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isBookmarkAppInstalled();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<SettingsClickEntryPoint> settingsClickEntryPoint();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<SettingsItemType> settingsItem();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<SettingsPageType> settingsPageType();
}
