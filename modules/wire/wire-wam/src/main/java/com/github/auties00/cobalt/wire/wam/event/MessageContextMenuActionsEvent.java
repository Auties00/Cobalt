package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MessageContextMenuActionType;
import com.github.auties00.cobalt.wire.wam.type.MessageContextMenuOptionType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMessageContextMenuActionsWamEvent")
@WamEvent(id = 3694)
public interface MessageContextMenuActionsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isMultiAction();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isOriginalSender();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MessageContextMenuActionType> messageContextMenuAction();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MessageContextMenuOptionType> messageContextMenuOption();
}
