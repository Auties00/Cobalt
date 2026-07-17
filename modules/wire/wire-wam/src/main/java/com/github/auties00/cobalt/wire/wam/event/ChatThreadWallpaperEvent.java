package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatThemeEntryType;
import com.github.auties00.cobalt.wire.wam.type.DeviceAppearanceType;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebChatThreadWallpaperWamEvent")
@WamEvent(id = 5286)
public interface ChatThreadWallpaperEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<DeviceAppearanceType> appearanceType();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> belongsToCommunity();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> chatThemeId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ChatThemeEntryType> chatThemeSource();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MessageChatType> chatType();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> colorSchemeId();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> wallpaperApplied();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> wallpaperId();
}
