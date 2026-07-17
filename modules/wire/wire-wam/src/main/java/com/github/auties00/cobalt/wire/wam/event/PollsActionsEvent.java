package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ClientGroupSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;
import com.github.auties00.cobalt.wire.wam.type.PollActionType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPollsActionsWamEvent")
@WamEvent(id = 3676)
public interface PollsActionsEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<MessageChatType> chatType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ClientGroupSizeBucket> groupSizeBucket();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> hideVoterName();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isAdmin();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PollActionType> pollAction();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong pollCreationDs();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong pollDurationMs();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong pollOptionsCount();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();
}
