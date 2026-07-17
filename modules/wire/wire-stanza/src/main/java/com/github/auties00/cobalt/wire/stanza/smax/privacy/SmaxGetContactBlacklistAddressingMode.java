package com.github.auties00.cobalt.wire.stanza.smax.privacy;

/**
 * Selects the addressing mode for a {@link SmaxGetContactBlacklistRequest}.
 *
 * <p>The {@link #LID} mode drives the migration of the Last-Seen, About, Group-Add, and Profile-Picture privacy
 * lists to LID, while the {@link #PN} mode is the historical default for non-migrated clients.
 */
public enum SmaxGetContactBlacklistAddressingMode {
    /**
     * Selects the legacy phone-number addressing mode that emits a bare {@code <privacy>} envelope.
     *
     * <p>Used for clients still on the PN-addressed disallowed-list flow; the relay returns a
     * {@link SmaxGetContactBlacklistResponse.Success} variant whose {@code <user/>} children are PN JIDs.
     */
    PN,

    /**
     * Selects the migrated LID addressing mode that emits a {@code <privacy addressing_mode="lid">} envelope.
     *
     * <p>The relay returns a {@link SmaxGetContactBlacklistResponse.SuccessLID} variant whose {@code <user/>}
     * children carry the {@link SmaxGetContactBlacklistContactListId} discriminator.
     */
    LID
}
