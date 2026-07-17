package com.github.auties00.cobalt.wire.linked.setting.privacy;

/**
 * Enumerates the JID addressing modes accepted by the per-category
 * contact-blacklist query.
 *
 * <p>The contact blacklist is the list of contacts that are excluded
 * from one of WhatsApp's privacy categories ({@code last seen},
 * {@code profile picture}, {@code online}, {@code status}, etc.).
 * WhatsApp ships two parallel addressing schemes for the entries:
 * the legacy phone-number form and the modern lid form that
 * decouples the privacy entry from the user's phone number. Callers
 * pick the desired addressing scheme through this enum when calling
 * the per-category contact-blacklist refresh.
 */
public enum ContactBlacklistAddressingMode {
    /**
     * The legacy phone-number addressing mode. Each blacklist entry is
     * keyed by a phone-number JID, optionally accompanied by the lid
     * echo when the relay has already migrated the entry.
     */
    PN,

    /**
     * The modern lid addressing mode. Each blacklist entry is keyed by
     * a lid JID; the legacy phone-number JID is only carried as
     * additional metadata.
     */
    LID
}
