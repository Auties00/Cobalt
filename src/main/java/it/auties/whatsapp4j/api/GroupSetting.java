package it.auties.whatsapp4j.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
public enum GroupSetting {
    EDIT_GROUP_INFO("locked"),
    SEND_MESSAGES("announcement");

    private final @Getter String data;
}
