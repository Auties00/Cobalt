package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ApplicationState;
import com.github.auties00.cobalt.wire.wam.type.MdLinkDeviceCompanionStage;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdLinkDeviceCompanionWamEvent")
@WamEvent(id = 2576)
public interface MdLinkDeviceCompanionEvent extends WamEventSpec {
    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<ApplicationState> applicationState();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> mdCompanionRefHash();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong mdDurationS();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mdLinkDeviceCompanionErrorCode();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MdLinkDeviceCompanionStage> mdLinkDeviceCompanionStage();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong mdLinkDeviceExperienceId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> mdRegAttemptId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> mdSessionId();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong mdTimestampS();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> mdWasUpgraded();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> userLocale();
}
