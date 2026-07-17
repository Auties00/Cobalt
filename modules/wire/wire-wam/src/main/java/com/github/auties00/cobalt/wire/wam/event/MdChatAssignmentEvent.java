package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatAssignmentActionType;
import com.github.auties00.cobalt.wire.wam.type.ChatAssignmentChatType;
import com.github.auties00.cobalt.wire.wam.type.ChatAssignmentEntryPointType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdChatAssignmentWamEvent")
@WamEvent(id = 3752)
public interface MdChatAssignmentEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> assignerAgentId();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> assignerBrowserId();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong assignerMdId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ChatAssignmentActionType> chatAssignmentAction();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> chatAssignmentAgentId();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> chatAssignmentBrowserId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ChatAssignmentChatType> chatAssignmentChatType();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ChatAssignmentEntryPointType> chatAssignmentEntryPoint();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong chatAssignmentMdId();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong chatsCnt();
}
