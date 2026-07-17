package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DyiReportTypeCode;
import com.github.auties00.cobalt.wire.wam.type.DyiTriggerTypeCode;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebDyiReportRequestWamEvent")
@WamEvent(id = 7166)
public interface DyiReportRequestEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DyiReportTypeCode> dyiReportType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DyiTriggerTypeCode> dyiTriggerType();
}
