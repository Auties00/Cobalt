package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupJoinRequestActionType;
import com.github.auties00.cobalt.wire.wam.type.GroupJoinRequestEntrypointType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWaFsGroupJoinRequestActionWamEvent")
@WamEvent(id = 3944)
public interface WaFsGroupJoinRequestActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> groupJid();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<GroupJoinRequestActionType> groupJoinRequestAction();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<GroupJoinRequestEntrypointType> groupJoinRequestEntrypoint();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong groupJoinRequestGroupsInCommon();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isSuccessful();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> serverResponseTime();
}
