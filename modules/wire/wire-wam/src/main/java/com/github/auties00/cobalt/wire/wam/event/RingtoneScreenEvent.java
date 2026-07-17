package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.RingtoneEntryType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebRingtoneScreenWamEvent")
@WamEvent(id = 7608)
public interface RingtoneScreenEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong premiumRingtonesDownloadedCount();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> ringtoneChangeApplied();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> ringtoneId();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> ringtoneReset();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> ringtoneSelectionCancelled();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<RingtoneEntryType> ringtoneSource();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> ringtoneSubscribeSelected();
}
