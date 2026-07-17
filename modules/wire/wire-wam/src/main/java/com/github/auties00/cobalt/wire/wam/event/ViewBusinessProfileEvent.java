package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BusinessToolsLinkedAccountType;
import com.github.auties00.cobalt.wire.wam.type.ProfileEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.TrustSignalBuckets;
import com.github.auties00.cobalt.wire.wam.type.ViewBusinessProfileAction;
import com.github.auties00.cobalt.wire.wam.type.WebsiteSourceType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebViewBusinessProfileWamEvent")
@WamEvent(id = 1522)
public interface ViewBusinessProfileEvent extends WamEventSpec {
    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<TrustSignalBuckets> bizFbSize();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<TrustSignalBuckets> bizIgSize();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> businessProfileJid();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> catalogSessionId();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> hasCoverPhoto();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isProfileLinked();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isSelfView();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<BusinessToolsLinkedAccountType> linkedAccount();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ProfileEntryPoint> profileEntryPoint();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong scrollDepth();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ViewBusinessProfileAction> viewBusinessProfileAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebsiteSourceType> websiteSource();
}
