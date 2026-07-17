package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.ContactContent;

import java.util.Optional;

/**
 * The Cloud transport's single-contact content body.
 */
public final class CloudContactContent implements ContactContent {
    /**
     * The contact display name, or {@code null} when unset.
     */
    private final String displayName;

    /**
     * The vCard rendering, or {@code null} when unset.
     */
    private final String vcard;

    /**
     * Constructs a Cloud contact body.
     *
     * @param displayName the contact display name, or {@code null} when unset
     * @param vcard       the vCard rendering, or {@code null} when unset
     */
    public CloudContactContent(String displayName, String vcard) {
        this.displayName = displayName;
        this.vcard = vcard;
    }

    @Override
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    @Override
    public Optional<String> vcard() {
        return Optional.ofNullable(vcard);
    }
}
