package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatWallpaperEntryType;
import com.github.auties00.cobalt.wire.wam.type.ChatWallpaperType;
import com.github.auties00.cobalt.wire.wam.type.DeviceAppearanceType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebChatWallpaperWamEvent")
@WamEvent(id = 5264)
public interface ChatWallpaperEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<DeviceAppearanceType> appearanceType();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> chatWallpaperChangeApplied();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChatWallpaperEntryType> chatWallpaperSource();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ChatWallpaperType> chatWallpaperType();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> chatWallpaperVisit();
}
