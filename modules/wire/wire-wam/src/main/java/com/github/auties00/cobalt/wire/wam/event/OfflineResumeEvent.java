package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.OfflineResumeResultType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebOfflineResumeWamEvent")
@WamEvent(id = 3112)
public interface OfflineResumeEvent extends WamEventSpec {
    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> affectedBySleepMode();

    @WamProperty(index = 49, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 36, type = WamType.INTEGER)
    OptionalLong attemptNumber();

    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong chatQueueSize();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong chatThreadCount();

    @WamProperty(index = 39, type = WamType.TIMER)
    Optional<Instant> dbDurationT();

    @WamProperty(index = 40, type = WamType.TIMER)
    Optional<Instant> dbMainThreadDurationT();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong dbMainThreadReadsCount();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong dbMainThreadWritesCount();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong dbReadsCount();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong dbWritesCount();

    @WamProperty(index = 45, type = WamType.BOOLEAN)
    Optional<Boolean> disconnected();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong e2eeQueueSize();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong expectedOfflineCallCount();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong expectedOfflineMessageCount();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong expectedOfflineNotificationCount();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong expectedOfflineReceiptCount();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isOfflineCompleteMissed();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isResumeInForeground();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> isResumeStartedInForeground();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> isRunningFromServiceExtension();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> lastStanzaT();

    @WamProperty(index = 38, type = WamType.INTEGER)
    OptionalLong logoutSessionId();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong mailboxAge();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> mainScreenLoadT();

    @WamProperty(index = 54, type = WamType.TIMER)
    Optional<Instant> nseMergeT();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong offlineCallCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong offlineDecryptErrorCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong offlineMessageCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong offlineNotificationCount();

    @WamProperty(index = 8, type = WamType.TIMER)
    Optional<Instant> offlinePreviewT();

    @WamProperty(index = 20, type = WamType.TIMER)
    Optional<Instant> offlineProcessingT();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong offlineReceiptCount();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<OfflineResumeResultType> offlineResumeResult();

    @WamProperty(index = 46, type = WamType.TIMER)
    Optional<Instant> offlineSessionT();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong offlineSizeBytes();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> onTrickleMode();

    @WamProperty(index = 11, type = WamType.TIMER)
    Optional<Instant> pageLoadT();

    @WamProperty(index = 25, type = WamType.TIMER)
    Optional<Instant> passiveModeT();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong preackCallCount();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong preackMessageCount();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong preackNotificationCount();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong preackReceiptCount();

    @WamProperty(index = 47, type = WamType.INTEGER)
    OptionalLong preacksCount();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong processedCallCount();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong processedMessageCount();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong processedNotificationCount();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong processedReceiptCount();

    @WamProperty(index = 51, type = WamType.INTEGER)
    OptionalLong queuedMessageCount();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong queuedNotificationCount();

    @WamProperty(index = 53, type = WamType.INTEGER)
    OptionalLong queuedReceiptCount();

    @WamProperty(index = 48, type = WamType.STRING)
    Optional<String> runningTasks();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> socketConnectT();

    @WamProperty(index = 34, type = WamType.STRING)
    Optional<String> transientOfflineSessionId();

    @WamProperty(index = 57, type = WamType.INTEGER)
    OptionalLong unorderedQueueSize();
}
