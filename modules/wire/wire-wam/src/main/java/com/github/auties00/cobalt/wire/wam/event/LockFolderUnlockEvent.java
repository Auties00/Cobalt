package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LandingSurface;
import com.github.auties00.cobalt.wire.wam.type.UnlockEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLockFolderUnlockWamEvent")
@WamEvent(id = 4218)
public interface LockFolderUnlockEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<LandingSurface> landingSurface();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong totalChatCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<UnlockEntryPoint> unlockEntryPoint();
}
