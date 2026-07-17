package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ShareContentUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.ShareContentUserJourneyEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebShareContentUserJourneyWamEvent")
@WamEvent(id = 5734)
public interface ShareContentUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> captionAdded();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> forwardUserJourneyFunnelId();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> hasBotImagineImages();

    @WamProperty(index = 21, type = WamType.BOOLEAN)
    Optional<Boolean> hasCaptionPrefilled();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> hasFiles();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> hasImages();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> hasLinks();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> hasMusic();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> hasStatusRecipient();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> hasVideo();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isForwardFlow();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mediaCount();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong messageSelectedCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong numberOfRecipients();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> prefilledCaptionRemoved();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ShareContentUserJourneyAction> shareContentUserJourneyAction();

    @WamProperty(index = 24, type = WamType.ENUM)
    Optional<ShareContentUserJourneyEntryPoint> shareContentUserJourneyEntryPoint();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<TsSurface> shareContentUserJourneySurfaceEntryPoint();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
