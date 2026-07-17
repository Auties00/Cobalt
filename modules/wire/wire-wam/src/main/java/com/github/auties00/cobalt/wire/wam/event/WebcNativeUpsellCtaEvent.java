package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcNativeUpsellCtaEventType;
import com.github.auties00.cobalt.wire.wam.type.WebcNativeUpsellCtaQrScreenExperimentGroup;
import com.github.auties00.cobalt.wire.wam.type.WebcNativeUpsellCtaReleaseChannel;
import com.github.auties00.cobalt.wire.wam.type.WebcNativeUpsellCtaSourceType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcNativeUpsellCtaWamEvent")
@WamEvent(id = 3934)
public interface WebcNativeUpsellCtaEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcNativeUpsellCtaEventType> webcNativeUpsellCtaEventType();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> webcNativeUpsellCtaIsBetaUser();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<WebcNativeUpsellCtaQrScreenExperimentGroup> webcNativeUpsellCtaQrScreenExperimentGroup();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<WebcNativeUpsellCtaReleaseChannel> webcNativeUpsellCtaReleaseChannel();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<WebcNativeUpsellCtaSourceType> webcNativeUpsellCtaSource();
}
