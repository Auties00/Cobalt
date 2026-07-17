package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleErrorActionType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleErrorCodeType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleLinkStateType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleTraceActionType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleTraceSourceType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWaffleCompanionStateLifecycleWamEvent")
@WamEvent(id = 8248)
public interface WaffleCompanionStateLifecycleEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong waffleLifecycleElapsedMs();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WaffleLifecycleErrorActionType> waffleLifecycleErrorAction();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<WaffleLifecycleErrorCodeType> waffleLifecycleErrorCode();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> waffleLifecycleHasAccessToken();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> waffleLifecycleHasExistingRow();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<WaffleLifecycleLinkStateType> waffleLifecycleLinkState();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong waffleLifecycleRetryCount();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<WaffleLifecycleTraceActionType> waffleLifecycleTraceAction();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<WaffleLifecycleTraceSourceType> waffleLifecycleTraceSource();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong waffleLifecycleUnlinkType();
}
