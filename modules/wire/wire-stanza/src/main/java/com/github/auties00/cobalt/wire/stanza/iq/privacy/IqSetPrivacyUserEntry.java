package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import java.util.Objects;
import java.util.Optional;

/**
 * One {@code <user/>} entry in the user-list payload of an {@link IqSetPrivacyRequest} that targets
 * the {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST} value.
 * <p>
 * One entry exists per peer being added to or removed from a category's exclusion list. The
 * {@link #username()} and {@link #pnJid()} fields are only emitted under
 * {@link IqSetPrivacyAddressingMode#LID} and provide the legacy discriminator that lets the relay
 * resolve the LID back to a phone-number identity (matching WA Web's {@code createLidUserNode}
 * branch in {@code WAWebSetPrivacyJob}).
 *
 * @implNote
 * This implementation is structurally final and value-equality based. The {@link #jid()} field is
 * the wire JID exactly; the addressing mode is decided one level up on the request and the entry
 * does not validate that its {@code jid} matches the chosen mode.
 */
@WhatsAppWebModule(moduleName = "WAWebSetPrivacyJob")
public final class IqSetPrivacyUserEntry {
    /**
     * The action to apply to this user on the per-category exclusion list.
     */
    private final IqSetPrivacyUserAction action;

    /**
     * The user's JID; interpreted as a phone-number identity or LID per the enclosing request's
     * {@link IqSetPrivacyAddressingMode}.
     */
    private final Jid jid;

    /**
     * The optional WhatsApp username discriminator emitted on the LID variant when the contact has a
     * username and the {@code username_contact_privacy_setting_allow_uncontact_set_enable} AB prop
     * is on.
     */
    private final String username;

    /**
     * The optional phone-number JID fallback emitted on the LID variant when the contact has no
     * {@link #username} so the relay can still resolve a legacy phone-number identity.
     */
    private final Jid pnJid;

    /**
     * Constructs a user entry.
     * <p>
     * Most callers pass {@code null} for {@code username} and {@code pnJid}; the discriminators are
     * only used under {@link IqSetPrivacyAddressingMode#LID}.
     *
     * @param action   the action to apply; never {@code null}
     * @param jid      the user's JID; never {@code null}
     * @param username the optional WhatsApp username discriminator; may be {@code null}
     * @param pnJid    the optional phone-number JID fallback; may be {@code null}
     * @throws NullPointerException if {@code action} or {@code jid} is {@code null}
     */
    public IqSetPrivacyUserEntry(IqSetPrivacyUserAction action, Jid jid, String username, Jid pnJid) {
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.username = username;
        this.pnJid = pnJid;
    }

    /**
     * Returns the action to apply to this user.
     *
     * @return the action; never {@code null}
     */
    public IqSetPrivacyUserAction action() {
        return action;
    }

    /**
     * Returns the user's JID.
     * <p>
     * Interpreted as a phone-number JID under {@link IqSetPrivacyAddressingMode#PN} or a LID under
     * {@link IqSetPrivacyAddressingMode#LID}; the mode is decided one level up on
     * {@link IqSetPrivacyRequest#addressingMode()}.
     *
     * @return the user's JID; never {@code null}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the optional WhatsApp username discriminator.
     * <p>
     * Only emitted on the LID variant. Present when the contact has a username and the
     * {@code username_contact_privacy_setting_allow_uncontact_set_enable} AB prop is on.
     *
     * @return the username, or {@link Optional#empty()} when omitted
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Returns the optional phone-number JID fallback.
     * <p>
     * Only emitted on the LID variant when the contact has no {@link #username()} so the relay can
     * still resolve the legacy phone-number identity.
     *
     * @return the phone-number JID, or {@link Optional#empty()} when omitted
     */
    public Optional<Jid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation compares every field by value; two entries are equal when every field
     * matches.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetPrivacyUserEntry) obj;
        return this.action == that.action
                && Objects.equals(this.jid, that.jid)
                && Objects.equals(this.username, that.username)
                && Objects.equals(this.pnJid, that.pnJid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation hashes every field consistently with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(action, jid, username, pnJid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a debug-only representation of every field; the format is not stable
     * and must not be parsed.
     */
    @Override
    public String toString() {
        return "IqSetPrivacyUserEntry[action=" + action + ", jid=" + jid
                + ", username=" + username + ", pnJid=" + pnJid + ']';
    }
}
