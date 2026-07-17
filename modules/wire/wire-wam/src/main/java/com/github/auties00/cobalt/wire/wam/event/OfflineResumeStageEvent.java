package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.OfflineResumeModes;
import com.github.auties00.cobalt.wire.wam.type.OfflineResumeStages;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebOfflineResumeStageWamEvent")
@WamEvent(id = 3536)
public interface OfflineResumeStageEvent extends WamEventSpec {
    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong attemptId();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong chatThreadCount();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<OfflineResumeStages> currentOfflineStage();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isResumeInForeground();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isResumeStartedInForeground();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong lastPushTimestampMs();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mailboxAge();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong offlineCallCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong offlineDecryptErrorCount();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong offlineMessageCount();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong offlineNotificationCount();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong offlineReceiptCount();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<OfflineResumeModes> offlineResumeMode();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> offlineSessionId();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong offlineSizeBytes();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong offlineStageTimestampMs();

    @WamProperty(index = 17, type = WamType.TIMER)
    Optional<Instant> passiveModeT();
}
