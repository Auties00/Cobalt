package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatFilterActionTypes;
import com.github.auties00.cobalt.wire.wam.type.ChatFilterTargetScreen;
import com.github.auties00.cobalt.wire.wam.type.ChatFilterTypes;
import com.github.auties00.cobalt.wire.wam.type.ChatSearchResultType;
import com.github.auties00.cobalt.wire.wam.type.ListType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChatFilterEventWamEvent")
@WamEvent(id = 1616)
public interface ChatFilterEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatFilterActionTypes> actionType();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> activitySessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChatFilterTypes> filterType();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> labelName();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong listId();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong listIndex();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<ListType> listType();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> metadata();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong predefinedId();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> searchQueryId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> searchRequestId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ChatSearchResultType> searchResultType();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong sessionId();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ChatFilterTargetScreen> targetScreen();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> threadId();
}
