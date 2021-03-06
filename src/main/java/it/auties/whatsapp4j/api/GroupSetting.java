package it.auties.whatsapp4j.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a group
 * Said settings can be changed using various methods in {@code WhatsappAPI}
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum GroupSetting {
    EDIT_GROUP_INFO("locked"),
    SEND_MESSAGES("announcement");

    private final @Getter String data;
}
