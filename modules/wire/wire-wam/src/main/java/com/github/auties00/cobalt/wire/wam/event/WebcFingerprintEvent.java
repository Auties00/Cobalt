package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BrowserEngineName;
import com.github.auties00.cobalt.wire.wam.type.PlatformName;
import com.github.auties00.cobalt.wire.wam.type.WebcWindowNavigatorWebdriverType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcFingerprintWamEvent")
@WamEvent(id = 1704)
public interface WebcFingerprintEvent extends WamEventSpec {
    @WamProperty(index = 50, type = WamType.STRING)
    Optional<String> audioFingerprint();

    @WamProperty(index = 51, type = WamType.STRING)
    Optional<String> automationSignals();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong batteryLevel();

    @WamProperty(index = 26, type = WamType.ENUM)
    Optional<BrowserEngineName> browserEngine();

    @WamProperty(index = 52, type = WamType.STRING)
    Optional<String> chromeStructure();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong connectionRtt();

    @WamProperty(index = 38, type = WamType.STRING)
    Optional<String> cpuMake();

    @WamProperty(index = 39, type = WamType.STRING)
    Optional<String> deviceMemory();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> extentionIds();

    @WamProperty(index = 36, type = WamType.STRING)
    Optional<String> externalSources();

    @WamProperty(index = 54, type = WamType.STRING)
    Optional<String> foreignDbList();

    @WamProperty(index = 40, type = WamType.STRING)
    Optional<String> gpuMake();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> hasChrome();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> hasTaskbar();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> hasWebShare();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong historyLength();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong mimeTypeCount();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> notificationPermission();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> pdfViewerEnabled();

    @WamProperty(index = 42, type = WamType.STRING)
    Optional<String> peripherals();

    @WamProperty(index = 53, type = WamType.STRING)
    Optional<String> permissionsConsistency();

    @WamProperty(index = 34, type = WamType.ENUM)
    Optional<PlatformName> platformEstimate();

    @WamProperty(index = 35, type = WamType.INTEGER)
    OptionalLong pluginCount();

    @WamProperty(index = 43, type = WamType.STRING)
    Optional<String> screenResolution();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong sessionStorageLength();

    @WamProperty(index = 45, type = WamType.STRING)
    Optional<String> timezone();

    @WamProperty(index = 46, type = WamType.BOOLEAN)
    Optional<Boolean> touchPresence();

    @WamProperty(index = 47, type = WamType.STRING)
    Optional<String> viewportSize();

    @WamProperty(index = 48, type = WamType.STRING)
    Optional<String> waUlCookie();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> webcCanvasFingerprint();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> webcWebglFingerprint();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> webcWebglRenderer();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> webcWebglVendor();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<WebcWindowNavigatorWebdriverType> webcWindowNavigatorWebdriver();
}
