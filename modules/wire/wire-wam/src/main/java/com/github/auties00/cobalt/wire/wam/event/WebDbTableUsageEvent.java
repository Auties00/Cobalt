package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebScenarioCode;
import com.github.auties00.cobalt.wire.wam.type.WebTableLogReasonCode;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebDbTableUsageWamEvent")
@WamEvent(id = 5074, releaseWeight = 100)
public interface WebDbTableUsageEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> offlineSessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebScenarioCode> webScenario();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> webTable();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<WebTableLogReasonCode> webTableLogReason();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong webTableReadCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong webTableWriteCount();
}
