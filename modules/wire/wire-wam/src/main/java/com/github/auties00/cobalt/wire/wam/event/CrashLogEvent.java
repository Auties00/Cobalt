package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CrashApplicationState;
import com.github.auties00.cobalt.wire.wam.type.CrashType;
import com.github.auties00.cobalt.wire.wam.type.IphoneProcessNonGlobal;
import com.github.auties00.cobalt.wire.wam.type.ProductArea;
import com.github.auties00.cobalt.wire.wam.type.SubfunnelType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UfadReportType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCrashLogWamEvent")
@WamEvent(id = 494)
public interface CrashLogEvent extends WamEventSpec {
    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> activeSubfunnelId();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<SubfunnelType> activeSubfunnelType();

    @WamProperty(index = 32, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 34, type = WamType.STRING)
    Optional<String> callTestBucketIdList();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> chatSessionId();

    @WamProperty(index = 23, type = WamType.ENUM)
    Optional<CrashApplicationState> crashApplicationState();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> crashContext();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong crashCount();

    @WamProperty(index = 28, type = WamType.ENUM)
    Optional<ProductArea> crashLogProductArea();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> crashLogSasEnabled();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> crashLogTimeSpentViewName();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> crashReason();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong crashTimeout();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<CrashType> crashType();

    @WamProperty(index = 30, type = WamType.ENUM)
    Optional<IphoneProcessNonGlobal> iphoneProcessNonGlobal();

    @WamProperty(index = 26, type = WamType.ENUM)
    Optional<TsSurface> iphoneTimeSpentSurfaceId();

    @WamProperty(index = 31, type = WamType.BOOLEAN)
    Optional<Boolean> lowPowerModeEnabled();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> peripheralConnected();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong processIdentifier();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> runningTasks();

    @WamProperty(index = 35, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 29, type = WamType.ENUM)
    Optional<UfadReportType> ufadReportType();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
