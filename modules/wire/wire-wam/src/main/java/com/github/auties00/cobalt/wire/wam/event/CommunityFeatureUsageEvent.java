package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CommunityFeatureUiActionTakenType;
import com.github.auties00.cobalt.wire.wam.type.CommunityUiFeatureType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebCommunityFeatureUsageWamEvent")
@WamEvent(id = 3696)
public interface CommunityFeatureUsageEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> communityId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<CommunityFeatureUiActionTakenType> communityUiAction();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<CommunityUiFeatureType> communityUiFeature();
}
