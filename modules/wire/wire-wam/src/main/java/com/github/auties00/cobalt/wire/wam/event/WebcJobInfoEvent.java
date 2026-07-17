package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcJobResultTypeCode;
import com.github.auties00.cobalt.wire.wam.type.WebcScenarioType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcJobInfoWamEvent")
@WamEvent(id = 3054)
public interface WebcJobInfoEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> jobName();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> jobPriority();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<WebcJobResultTypeCode> jobResultType();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong pendingJobsCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<WebcScenarioType> scenario();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong webcJobAddedT();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong webcJobCompletedT();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong webcJobStartedT();
}
