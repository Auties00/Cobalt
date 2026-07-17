package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdAppStateOfflineNotificationsWamEvent")
@WamEvent(id = 2602, betaWeight = 20, releaseWeight = 1000)
public interface MdAppStateOfflineNotificationsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong redundantCount();
}
