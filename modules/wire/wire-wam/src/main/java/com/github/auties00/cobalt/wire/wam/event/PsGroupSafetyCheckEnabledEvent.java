package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsGroupSafetyCheckEnabledWamEvent")
@WamEvent(id = 6238, channel = WamChannel.PRIVATE, privateStatsId = 216763284)
public interface PsGroupSafetyCheckEnabledEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> didJoinByGil();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> integrityGroupUserHashedId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> psSafetyCheckGroupJid();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> psWasSafetyCheckGroupInitiallyMuted();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> wasAddedByContact();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> willSafetyCheckBeSeen();
}
