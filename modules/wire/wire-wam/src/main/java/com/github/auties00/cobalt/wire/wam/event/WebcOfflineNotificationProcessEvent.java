package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.OfflineProcessRunReasons;
import com.github.auties00.cobalt.wire.wam.type.OfflineProcessStages;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcOfflineNotificationProcessWamEvent")
@WamEvent(id = 4222)
public interface WebcOfflineNotificationProcessEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<OfflineProcessStages> currentOfflineProcessStage();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong offlineProcessDecryptErrorCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong offlineProcessMailboxAge();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong offlineProcessMessageCount();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong offlineProcessNotificationCount();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> offlineProcessSessionId();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong offlineProcessStageTimestampMs();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<OfflineProcessRunReasons> runReason();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> swVersion();
}
