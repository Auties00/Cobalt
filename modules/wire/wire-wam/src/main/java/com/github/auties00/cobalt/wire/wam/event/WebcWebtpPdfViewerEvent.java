package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebtpEventType;
import com.github.auties00.cobalt.wire.wam.type.WebtpSourceType;

import java.util.Optional;
import java.util.OptionalDouble;

@WhatsAppWebModule(moduleName = "WAWebWebcWebtpPdfViewerWamEvent")
@WamEvent(id = 7506)
public interface WebcWebtpPdfViewerEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> webtpErrorCode();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> webtpErrorMessage();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> webtpErrorStack();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> webtpErrorType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<WebtpEventType> webtpEvent();

    @WamProperty(index = 6, type = WamType.FLOAT)
    OptionalDouble webtpFileSize();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> webtpSdkVersion();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> webtpSessionId();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<WebtpSourceType> webtpSource();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> webtpTelemetryData();
}
