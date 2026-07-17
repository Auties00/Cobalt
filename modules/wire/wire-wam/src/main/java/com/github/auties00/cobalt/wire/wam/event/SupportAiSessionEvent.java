package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SupportAiEventType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebSupportAiSessionWamEvent")
@WamEvent(id = 4970)
public interface SupportAiSessionEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> citationCmsId();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong supportAiErrorCode();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> supportAiErrorMessage();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<SupportAiEventType> supportAiEventType();
}
