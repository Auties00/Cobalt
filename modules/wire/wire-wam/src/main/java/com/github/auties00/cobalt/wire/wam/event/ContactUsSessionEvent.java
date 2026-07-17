package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ContactUsExitState;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;

@WhatsAppWebModule(moduleName = "WAWebContactUsSessionWamEvent")
@WamEvent(id = 470)
public interface ContactUsSessionEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> contactUsAutomaticEmail();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ContactUsExitState> contactUsExitState();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> contactUsFaq();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> contactUsLogs();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> contactUsMenuFaqT();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> contactUsOutage();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> contactUsOutageEmail();

    @WamProperty(index = 19, type = WamType.FLOAT)
    OptionalDouble contactUsScreenshotC();

    @WamProperty(index = 11, type = WamType.TIMER)
    Optional<Instant> contactUsT();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> languageCode();
}
