package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatActionChatType;
import com.github.auties00.cobalt.wire.wam.type.ChatActionEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChatActionType;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebChatActionWamEvent")
@WamEvent(id = 2312)
public interface ChatActionEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ChatActionChatType> chatActionChatType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChatActionEntryPoint> chatActionEntryPoint();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> chatActionMuteDuration();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatActionType> chatActionType();
}
