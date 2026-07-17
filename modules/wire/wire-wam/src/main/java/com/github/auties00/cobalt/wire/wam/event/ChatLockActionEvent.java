package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ActionEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.AuthType;
import com.github.auties00.cobalt.wire.wam.type.ChatLockActionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChatLockActionWamEvent")
@WamEvent(id = 4212)
public interface ChatLockActionEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ActionEntryPoint> actionEntryPoint();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong actionFolderChatsCount();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<AuthType> authType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatLockActionType> chatLockActionType();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> chatLockIsGroup();
}
