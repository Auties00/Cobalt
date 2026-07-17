package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

/**
 * The local user's Status privacy configuration: who can and who cannot see
 * the 24-hour Status updates the user posts.
 *
 * <p>Pairs a {@link StatusPrivacyMode} (the audience selector) with the
 * companion JID list whose meaning depends on the mode:
 * {@link StatusPrivacyMode#WHITELIST} treats it as the allowlist,
 * {@link StatusPrivacyMode#CONTACTS_EXCEPT} treats it as the blocklist,
 * and {@link StatusPrivacyMode#CONTACTS} ignores it (the list is empty).
 *
 * <p>This DTO is the user-prefs-level view of the Status privacy setting
 * exposed by the WhatsApp server through the {@code <iq xmlns="status">}
 * IQ.
 */
@ProtobufMessage
public final class StatusPrivacySetting {
    /**
     * The selected distribution mode that determines how the paired JID
     * list is interpreted (allowlist vs. blocklist vs. unused).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    StatusPrivacyMode mode;

    /**
     * The JIDs paired with the mode: the allowlist for
     * {@link StatusPrivacyMode#WHITELIST}, the blocklist for
     * {@link StatusPrivacyMode#CONTACTS_EXCEPT}, and unused (empty) for
     * {@link StatusPrivacyMode#CONTACTS}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    List<Jid> jids;

    /**
     * Constructs a new {@code StatusPrivacySetting} with the supplied mode
     * and paired JID list.
     *
     * @param mode the distribution mode
     * @param jids the paired JID list; {@code null} is treated as an empty
     *             list
     */
    StatusPrivacySetting(StatusPrivacyMode mode, List<Jid> jids) {
        this.mode = mode;
        this.jids = jids == null ? List.of() : jids;
    }

    /**
     * Returns the selected distribution mode.
     *
     * @return an {@code Optional} containing the mode, or empty if not set
     */
    public Optional<StatusPrivacyMode> mode() {
        return Optional.ofNullable(mode);
    }

    /**
     * Returns the JIDs paired with the mode (allowlist, blocklist, or
     * empty depending on the mode).
     *
     * @return an unmodifiable list of JIDs; never {@code null}, possibly
     *         empty
     */
    public List<Jid> jids() {
        return jids;
    }

    /**
     * Sets the selected distribution mode.
     *
     * @param mode the mode to set, or {@code null} to clear
     */
    public void setMode(StatusPrivacyMode mode) {
        this.mode = mode;
    }

    /**
     * Sets the JIDs paired with the mode.
     *
     * @param jids the JID list to set; {@code null} is treated as an empty
     *             list
     */
    public void setJids(List<Jid> jids) {
        this.jids = jids == null ? List.of() : jids;
    }
}
