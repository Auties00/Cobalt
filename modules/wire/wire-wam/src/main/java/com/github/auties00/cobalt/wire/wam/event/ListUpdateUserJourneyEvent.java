package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ListAction;
import com.github.auties00.cobalt.wire.wam.type.ListType;
import com.github.auties00.cobalt.wire.wam.type.ListUpdateUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.UpdateEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebListUpdateUserJourneyWamEvent")
@WamEvent(id = 5958)
public interface ListUpdateUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong customListCount();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ListAction> listAction();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong listId();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ListType> listType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ListUpdateUserJourneyAction> listUpdateUserJourneyAction();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong predefinedId();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong presetListCount();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<UpdateEntryPoint> updateEntryPoint();
}
