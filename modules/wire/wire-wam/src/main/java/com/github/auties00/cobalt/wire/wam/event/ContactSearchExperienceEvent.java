package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ContactSearchEntrypoint;
import com.github.auties00.cobalt.wire.wam.type.SearchActionName;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebContactSearchExperienceWamEvent")
@WamEvent(id = 6574)
public interface ContactSearchExperienceEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ContactSearchEntrypoint> contactSearchEntrypoint();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> isUsernameSearch();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<SearchActionName> searchActionName();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> searchStartsWithAt();
}
