package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SmbDataSharingConsentScreenEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.SmbDataSharingConsentScreenType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebSmbDataSharingConsentScreenWamEvent")
@WamEvent(id = 3972)
public interface SmbDataSharingConsentScreenEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong elapsedTimeMs();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong previousImpressionCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong previousOptOutImpressionCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<SmbDataSharingConsentScreenEntryPoint> smbDataSharingConsentScreenEntryPoint();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<SmbDataSharingConsentScreenType> smbDataSharingConsentScreenType();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong smbDataSharingConsentScreenVersion();
}
