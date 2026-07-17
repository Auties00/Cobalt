package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.StickerAddToFavoriteOriginType;
import com.github.auties00.cobalt.wire.wam.type.StickerMakerSourceType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebStickerAddToFavoriteWamEvent")
@WamEvent(id = 1842)
public interface StickerAddToFavoriteEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<StickerAddToFavoriteOriginType> stickerAddToFavoriteOrigin();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsAi();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsAnimated();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsAvatar();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsFirstParty();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsFromStickerMaker();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsFromUserCreatedPack();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsGiphy();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsKlipy();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsPremium();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsTenor();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsText();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<StickerMakerSourceType> stickerMakerSourceType();
}
