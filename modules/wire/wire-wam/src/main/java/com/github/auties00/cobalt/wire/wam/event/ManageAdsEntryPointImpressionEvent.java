package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ManageAdsEntryPoint;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebManageAdsEntryPointImpressionWamEvent")
@WamEvent(id = 4124)
public interface ManageAdsEntryPointImpressionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ManageAdsEntryPoint> manageAdsEntryPoint();
}
