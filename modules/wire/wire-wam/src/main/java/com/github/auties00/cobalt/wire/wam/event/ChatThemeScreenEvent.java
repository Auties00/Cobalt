package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatThemeEntryType;
import com.github.auties00.cobalt.wire.wam.type.ChatWallpaperType;
import com.github.auties00.cobalt.wire.wam.type.DeviceAppearanceType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebChatThemeScreenWamEvent")
@WamEvent(id = 6036)
public interface ChatThemeScreenEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DeviceAppearanceType> appearanceType();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> chatThemeChangeApplied();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> chatThemeId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ChatThemeEntryType> chatThemeSource();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ChatWallpaperType> chatWallpaperType();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> colorSchemeId();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> wallpaperId();
}
