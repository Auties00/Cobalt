package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.HarmfulFileWarningClickthroughAction;
import com.github.auties00.cobalt.wire.wam.type.HarmfulFileWarningSenderRelationship;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebDangerousFileOpenStatsV2WamEvent")
@WamEvent(id = 6708, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface DangerousFileOpenStatsV2Event extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<HarmfulFileWarningClickthroughAction> harmfulFileWarningClickthroughAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<HarmfulFileWarningSenderRelationship> harmfulFileWarningSenderRelationship();
}
