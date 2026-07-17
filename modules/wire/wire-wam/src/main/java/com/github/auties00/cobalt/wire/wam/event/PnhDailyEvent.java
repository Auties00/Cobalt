package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPnhDailyWamEvent")
@WamEvent(id = 3806)
public interface PnhDailyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> communityId();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong mappingMissing();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong pnhIndicatorClicksChat();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong pnhIndicatorClicksInfoScreen();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong reactionDeleteCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong reactionOpenTrayCount();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong totalContacts();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();
}
