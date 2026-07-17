package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChatLockDailyWamEvent")
@WamEvent(id = 4214)
public interface ChatLockDailyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong folderChatsCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong folderOpenCount();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> lockFolderHidden();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong newAddChatCount();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong newRemoveChatCount();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> secretCodeActive();
}
