package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BusinessOwnerPlatform;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedInteractionAction;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedInteractionAssetType;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedInteractionReferral;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedInteractionSurface;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMetaVerifiedInteractionWamEvent")
@WamEvent(id = 4870, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface MetaVerifiedInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> businessOwnerJid();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<BusinessOwnerPlatform> businessOwnerPlatform();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isMetaVerifiedSubscribed();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isSelfView();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MetaVerifiedInteractionAction> metaVerifiedInteractionAction();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MetaVerifiedInteractionAssetType> metaVerifiedInteractionAssetType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MetaVerifiedInteractionReferral> metaVerifiedInteractionReferral();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MetaVerifiedInteractionSurface> metaVerifiedInteractionSurface();
}
