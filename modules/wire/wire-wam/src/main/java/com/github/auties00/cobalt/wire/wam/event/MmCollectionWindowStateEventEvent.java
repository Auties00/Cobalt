package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMmCollectionWindowStateEventWamEvent")
@WamEvent(id = 6744, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface MmCollectionWindowStateEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> businessLidOrJid();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> entSourceSubplatform();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isUserDisclosed();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mmDisclosureFlags();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> mmHasDisclosedToken();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> mmHasDisclosedUrl();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> mmHasShowDisclosureFlag();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> mmHasUndisclosedToken();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> templateId();
}
