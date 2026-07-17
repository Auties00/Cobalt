package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ReportToAdminInteraction;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebReportToAdminEventsWamEvent")
@WamEvent(id = 4420)
public interface ReportToAdminEventsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ReportToAdminInteraction> reportToAdminInteraction();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> rtaGroupId();
}
