package com.github.auties00.cobalt.wire.stanza.iq.privacy;

/**
 * Wire addressing mode for the user-list payload inside an {@link IqSetPrivacyRequest}.
 * <p>
 * Selects which JID kind the relay treats as authoritative for the per-user mutations on a
 * {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST} category. The choice maps to the
 * presence or absence of an {@code addressing_mode="lid"} attribute on the {@code <privacy>}
 * envelope plus the JID kind on each {@code <user>} child.
 *
 * @implNote
 * This implementation gates only the envelope shape; the migration decision is the caller's. WA
 * Web routes the choice through
 * {@code WAWebQueryPrivacyDisallowedListUtil.isPrivacyDisallowedListTypeLidMigrated(name)}, which
 * consults per-category AB props and falls back to the phone-number variant on failure. Cobalt
 * does not run that gate and the caller is expected to pick the mode that matches the contact's
 * migration state.
 */
public enum IqSetPrivacyAddressingMode {
    /**
     * The legacy phone-number-addressed variant.
     * <p>
     * Emits a bare {@code <privacy>} envelope and {@code <user jid=PN_JID/>} children, where the
     * JIDs are the user's phone-number identity. Used for categories that have not opted into
     * LID-addressed privacy storage on the server.
     */
    PN,

    /**
     * The LID-addressed variant.
     * <p>
     * Emits {@code <privacy addressing_mode="lid">} and {@code <user jid=LID_JID/>} children with
     * either a {@code username} discriminator (when the contact has a WhatsApp username and the
     * {@code username_contact_privacy_setting_allow_uncontact_set_enable} AB prop is on) or a
     * {@code pn_jid} fallback echoing the contact's phone-number identity. Used for categories that
     * have already migrated to LID-addressed privacy storage.
     */
    LID
}
