package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Per-user action carried by each {@link IqSetPrivacyUserEntry} on the user-list payload of an
 * {@link IqSetPrivacyRequest}.
 * <p>
 * {@link #ADD} extends the per-category exclusion list (for example, blocking someone from seeing
 * the user's last-seen) and {@link #REMOVE} retracts a previous block. The relay applies the
 * mutations in place and echoes the new {@link IqSetPrivacyResponse.CategoryOutcome} back.
 *
 * @implNote
 * This implementation mirrors the {@code PrivacyUserAction} enum exported by
 * {@code WAWebSetPrivacyJob} (values {@code "add"} and {@code "remove"}).
 */
@WhatsAppWebModule(moduleName = "WAWebSetPrivacyJob")
public enum IqSetPrivacyUserAction {
    /**
     * The {@code add} action.
     * <p>
     * Adds the user to the per-category exclusion list.
     */
    ADD("add"),

    /**
     * The {@code remove} action.
     * <p>
     * Removes the user from the per-category exclusion list.
     */
    REMOVE("remove");

    /**
     * The wire token emitted in the {@code action} attribute of each {@code <user>} child.
     */
    private final String wire;

    /**
     * Constructs a user-action constant from its wire token.
     *
     * @param wire the wire token; never {@code null}
     */
    IqSetPrivacyUserAction(String wire) {
        this.wire = wire;
    }

    /**
     * Returns the wire token for this action.
     * <p>
     * Used when serialising a {@code <user action=...>} attribute on an
     * {@link IqSetPrivacyUserEntry}.
     *
     * @return the wire token; never {@code null}
     */
    public String wire() {
        return wire;
    }
}
