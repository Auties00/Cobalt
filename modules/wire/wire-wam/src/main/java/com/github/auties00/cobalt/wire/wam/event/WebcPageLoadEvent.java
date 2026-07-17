package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcAppcacheStatusCode;
import com.github.auties00.cobalt.wire.wam.type.WebcNavigationType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;

@WhatsAppWebModule(moduleName = "WAWebWebcPageLoadWamEvent")
@WamEvent(id = 642)
public interface WebcPageLoadEvent extends WamEventSpec {
    @WamProperty(index = 29, type = WamType.ENUM)
    Optional<WebcAppcacheStatusCode> webcAppcacheStatus();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> webcCached();

    @WamProperty(index = 10, type = WamType.TIMER)
    Optional<Instant> webcConnectEnd();

    @WamProperty(index = 9, type = WamType.TIMER)
    Optional<Instant> webcConnectStart();

    @WamProperty(index = 19, type = WamType.TIMER)
    Optional<Instant> webcDomComplete();

    @WamProperty(index = 18, type = WamType.TIMER)
    Optional<Instant> webcDomContentLoadedEventEnd();

    @WamProperty(index = 17, type = WamType.TIMER)
    Optional<Instant> webcDomContentLoadedEventStart();

    @WamProperty(index = 16, type = WamType.TIMER)
    Optional<Instant> webcDomInteractive();

    @WamProperty(index = 15, type = WamType.TIMER)
    Optional<Instant> webcDomLoading();

    @WamProperty(index = 8, type = WamType.TIMER)
    Optional<Instant> webcDomainLookupEnd();

    @WamProperty(index = 7, type = WamType.TIMER)
    Optional<Instant> webcDomainLookupStart();

    @WamProperty(index = 23, type = WamType.TIMER)
    Optional<Instant> webcExeDone();

    @WamProperty(index = 22, type = WamType.TIMER)
    Optional<Instant> webcExeStart();

    @WamProperty(index = 6, type = WamType.TIMER)
    Optional<Instant> webcFetchStart();

    @WamProperty(index = 38, type = WamType.TIMER)
    Optional<Instant> webcInitialMountT();

    @WamProperty(index = 39, type = WamType.TIMER)
    Optional<Instant> webcInitialNavMountT();

    @WamProperty(index = 42, type = WamType.STRING)
    Optional<String> webcInitialPanel();

    @WamProperty(index = 43, type = WamType.TIMER)
    Optional<Instant> webcInitialPanelMountStartT();

    @WamProperty(index = 40, type = WamType.TIMER)
    Optional<Instant> webcInitialPanelMountT();

    @WamProperty(index = 46, type = WamType.TIMER)
    Optional<Instant> webcInitialPanelRenderT();

    @WamProperty(index = 37, type = WamType.TIMER)
    Optional<Instant> webcJsLoadT();

    @WamProperty(index = 21, type = WamType.TIMER)
    Optional<Instant> webcLoadEventEnd();

    @WamProperty(index = 20, type = WamType.TIMER)
    Optional<Instant> webcLoadEventStart();

    @WamProperty(index = 53, type = WamType.BOOLEAN)
    Optional<Boolean> webcLoadInForeground();

    @WamProperty(index = 45, type = WamType.TIMER)
    Optional<Instant> webcMainScriptEnd();

    @WamProperty(index = 44, type = WamType.TIMER)
    Optional<Instant> webcMainScriptStart();

    @WamProperty(index = 36, type = WamType.TIMER)
    Optional<Instant> webcNativeLoadT();

    @WamProperty(index = 32, type = WamType.ENUM)
    Optional<WebcNavigationType> webcNavigation();

    @WamProperty(index = 54, type = WamType.STRING)
    Optional<String> webcPageLoadId();

    @WamProperty(index = 34, type = WamType.TIMER)
    Optional<Instant> webcPageLoadT();

    @WamProperty(index = 41, type = WamType.BOOLEAN)
    Optional<Boolean> webcParallellyFetched();

    @WamProperty(index = 31, type = WamType.BOOLEAN)
    Optional<Boolean> webcQrCode();

    @WamProperty(index = 33, type = WamType.FLOAT)
    OptionalDouble webcRedirectCount();

    @WamProperty(index = 5, type = WamType.TIMER)
    Optional<Instant> webcRedirectEnd();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> webcRedirectStart();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> webcRequestStart();

    @WamProperty(index = 14, type = WamType.TIMER)
    Optional<Instant> webcResponseEnd();

    @WamProperty(index = 13, type = WamType.TIMER)
    Optional<Instant> webcResponseStart();

    @WamProperty(index = 11, type = WamType.TIMER)
    Optional<Instant> webcSecureConnectionStart();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> webcUnloadEventEnd();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> webcUnloadEventStart();

    @WamProperty(index = 28, type = WamType.FLOAT)
    OptionalDouble webcWsAttempts();

    @WamProperty(index = 27, type = WamType.TIMER)
    Optional<Instant> webcWsNormal();

    @WamProperty(index = 24, type = WamType.TIMER)
    Optional<Instant> webcWsOpening();

    @WamProperty(index = 25, type = WamType.TIMER)
    Optional<Instant> webcWsPairing();

    @WamProperty(index = 26, type = WamType.TIMER)
    Optional<Instant> webcWsSyncing();
}
