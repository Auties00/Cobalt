package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PsGroupSafetyCheckUiInteractions;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsGroupSafetyCheckUiInteractionsWamEvent")
@WamEvent(id = 6242, channel = WamChannel.PRIVATE, privateStatsId = 216763284)
public interface PsGroupSafetyCheckUiInteractionsEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> integrityGroupUserHashedId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> psSafetyCheckGroupJid();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PsGroupSafetyCheckUiInteractions> psSafetyCheckInteraction();
}
