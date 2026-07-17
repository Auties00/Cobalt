package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BugReportEntryPointName;
import com.github.auties00.cobalt.wire.wam.type.BugReportFlowAction;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebBugReportSessionWamEvent")
@WamEvent(id = 3850)
public interface BugReportSessionEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> bugReportErrorMessage();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BugReportFlowAction> bugReportFlowAction();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong bugReportImageCount();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong bugReportMediaCount();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong bugReportNumberOfChars();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong bugReportNumberOfWords();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> bugReportTaskId();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong bugReportVideoCount();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> bugReportingEndpoint();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<BugReportEntryPointName> bugReportingEntryPoint();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> clientServerJoinKey();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> submitBugCategory();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> submitBugContainsTitle();
}
