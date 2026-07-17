package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DeleteSuspendedGroupBtn;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebSuspendedGroupDeleteWamEvent")
@WamEvent(id = 4342, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface SuspendedGroupDeleteEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DeleteSuspendedGroupBtn> deleteBtnSource();
}
