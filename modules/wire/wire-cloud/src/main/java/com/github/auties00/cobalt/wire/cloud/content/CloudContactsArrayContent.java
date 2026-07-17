package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.ContactContent;
import com.github.auties00.cobalt.wire.core.message.ContactsArrayContent;

import java.util.List;
import java.util.Optional;

/**
 * The Cloud transport's multi-contact content body.
 */
public final class CloudContactsArrayContent implements ContactsArrayContent {
    /**
     * The group display name, or {@code null} when unset.
     */
    private final String displayName;

    /**
     * The individual contacts; never {@code null}.
     */
    private final List<ContactContent> contacts;

    /**
     * Constructs a Cloud multi-contact body.
     *
     * @param displayName the group display name, or {@code null} when unset
     * @param contacts    the individual contacts, or {@code null} for none
     */
    public CloudContactsArrayContent(String displayName, List<ContactContent> contacts) {
        this.displayName = displayName;
        this.contacts = contacts == null ? List.of() : List.copyOf(contacts);
    }

    @Override
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    @Override
    public List<ContactContent> contacts() {
        return contacts;
    }
}
