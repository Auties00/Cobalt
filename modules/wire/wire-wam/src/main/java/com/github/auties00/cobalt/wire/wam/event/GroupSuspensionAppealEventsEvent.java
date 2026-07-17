package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupSuspensionAppealUiAction;
import com.github.auties00.cobalt.wire.wam.type.GroupSuspensionAppealUiSurface;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebGroupSuspensionAppealEventsWamEvent")
@WamEvent(id = 7574)
public interface GroupSuspensionAppealEventsEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> groupJid();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> groupSuspensionAppealErrorMessage();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<GroupSuspensionAppealUiAction> groupSuspensionAppealUiAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<GroupSuspensionAppealUiSurface> groupSuspensionAppealUiSurface();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<GroupTypeClient> groupTypeClient();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isAdmin();
}
