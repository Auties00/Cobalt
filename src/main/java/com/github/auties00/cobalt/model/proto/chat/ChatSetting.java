package com.github.auties00.cobalt.model.proto.chat;

/**
 * Common interface for chat settings
 */
public sealed interface ChatSetting permits GroupSetting, CommunitySetting {
    int index();
}
