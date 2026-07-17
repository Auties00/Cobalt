package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupBulkRemovalAction;
import com.github.auties00.cobalt.wire.wam.type.GroupBulkRemovalEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebGroupBulkRemovalWamEvent")
@WamEvent(id = 7222)
public interface GroupBulkRemovalEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> bulkRemovalGroupId();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<GroupBulkRemovalAction> groupBulkRemovalAction();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<GroupBulkRemovalEntryPoint> groupBulkRemovalEntryPoint();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong removedMembersCount();
}
