package com.github.auties00.cobalt.model.chat;

/**
 * Common interface for chat settings
 */
public sealed interface ChatSetting permits GroupSetting, CommunitySetting {
    int index();
}
