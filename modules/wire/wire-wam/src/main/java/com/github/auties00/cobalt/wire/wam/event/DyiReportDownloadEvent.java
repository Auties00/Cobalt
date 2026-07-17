package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DyiReportTypeCode;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebDyiReportDownloadWamEvent")
@WamEvent(id = 7162)
public interface DyiReportDownloadEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> dyiDownloadErrorMessage();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> dyiDownloadSucceeded();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DyiReportTypeCode> dyiReportType();
}
