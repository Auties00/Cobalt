package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;

/**
 * Closed set of privacy-category names recognised by the relay's legacy
 * {@code <iq xmlns="privacy">} surface.
 * <p>
 * Each constant names one row of the WhatsApp privacy-settings screen (last seen, online presence,
 * profile picture, about text, read receipts, group-add permission, call-add permission, message
 * acceptance, defense-mode toggle). The constant identifies the row when constructing an
 * {@link IqSetPrivacyRequest} for a specific row, and keys into the
 * {@link IqQueryPrivacySettingsResponse.Success#categories()} map returned for an
 * {@link IqQueryPrivacySettingsRequest}.
 *
 * @implNote
 * This implementation enumerates the nine wire names that the WA Web
 * {@code WAWebQueryPrivacySettingsJob} parser recognises and routes into a typed shape; six
 * additional names ({@code pix}, {@code linked_profiles}, {@code stickers},
 * {@code dependentaccountmessages}, {@code cover_photo}, {@code dependent_account_calling},
 * {@code groupcreation}) are silently skipped by WA Web's parser and are intentionally not modelled
 * here.
 */
@WhatsAppWebModule(moduleName = "WAWebPrivacySettings")
public enum IqQueryPrivacySettingsCategoryName {
    /**
     * The {@code last} row.
     * <p>
     * Selects the audience that may see the user's last-seen timestamp; legal values are
     * {@link IqQueryPrivacySettingsVisibility#ALL}, {@link IqQueryPrivacySettingsVisibility#CONTACTS},
     * {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST}, and
     * {@link IqQueryPrivacySettingsVisibility#NONE}.
     */
    LAST_SEEN("last"),

    /**
     * The {@code online} row.
     * <p>
     * Selects the audience that may see the user's live online indicator; legal values are
     * {@link IqQueryPrivacySettingsVisibility#ALL} and
     * {@link IqQueryPrivacySettingsVisibility#MATCH_LAST_SEEN} (the second mirrors whatever
     * {@link #LAST_SEEN} is set to).
     */
    ONLINE("online"),

    /**
     * The {@code profile} row.
     * <p>
     * Selects the audience that may see the user's profile picture; uses the
     * {@link IqQueryPrivacySettingsVisibility#ALL} / {@link IqQueryPrivacySettingsVisibility#CONTACTS}
     * / {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST} /
     * {@link IqQueryPrivacySettingsVisibility#NONE} set.
     */
    PROFILE_PICTURE("profile"),

    /**
     * The {@code status} row.
     * <p>
     * Selects the audience that may see the user's about text (not the Status story feed, which
     * lives in a separate sync action). Uses the {@link IqQueryPrivacySettingsVisibility#ALL} /
     * {@link IqQueryPrivacySettingsVisibility#CONTACTS} /
     * {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST} /
     * {@link IqQueryPrivacySettingsVisibility#NONE} set.
     */
    ABOUT("status"),

    /**
     * The {@code readreceipts} row.
     * <p>
     * Toggles whether the user's read receipts are shipped to peers; legal values are
     * {@link IqQueryPrivacySettingsVisibility#ALL} and {@link IqQueryPrivacySettingsVisibility#NONE}.
     * The row is mutated through the dedicated typed setter {@link IqSetReadReceiptRequest} rather
     * than the multi-row {@link IqSetPrivacyRequest}.
     */
    READ_RECEIPTS("readreceipts"),

    /**
     * The {@code groupadd} row.
     * <p>
     * Selects the audience that may add the user to groups without an invite link; uses the
     * {@link IqQueryPrivacySettingsVisibility#ALL} / {@link IqQueryPrivacySettingsVisibility#CONTACTS}
     * / {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST} /
     * {@link IqQueryPrivacySettingsVisibility#NONE} set.
     */
    GROUP_ADD("groupadd"),

    /**
     * The {@code calladd} row.
     * <p>
     * Selects the audience that may add the user to group calls; legal values are
     * {@link IqQueryPrivacySettingsVisibility#ALL}, {@link IqQueryPrivacySettingsVisibility#KNOWN},
     * and {@link IqQueryPrivacySettingsVisibility#CONTACTS}.
     */
    CALL_ADD("calladd"),

    /**
     * The {@code messages} row.
     * <p>
     * Selects who may message the user without a prior conversation; legal values are
     * {@link IqQueryPrivacySettingsVisibility#ALL} and
     * {@link IqQueryPrivacySettingsVisibility#CONTACTS}.
     */
    MESSAGES("messages"),

    /**
     * The {@code defense} row.
     * <p>
     * Toggles the server-side defense-mode quarantine pipeline; legal values are
     * {@link IqQueryPrivacySettingsVisibility#OFF} and
     * {@link IqQueryPrivacySettingsVisibility#ON_STANDARD}.
     */
    DEFENSE_MODE("defense");

    /**
     * The wire token emitted in the {@code name} attribute of each {@code <category>} child.
     */
    private final String wire;

    /**
     * Constructs a category constant from its wire token.
     *
     * @param wire the wire token; never {@code null}
     */
    IqQueryPrivacySettingsCategoryName(String wire) {
        this.wire = wire;
    }

    /**
     * Returns the wire token for this category.
     * <p>
     * Used when serialising a {@code <category name=...>} attribute. Consumers compare the enum
     * value rather than pattern matching on the string.
     *
     * @return the wire token; never {@code null}
     */
    public String wire() {
        return wire;
    }

    /**
     * Resolves the constant whose {@link #wire()} equals the supplied token.
     * <p>
     * Projects an inbound {@code <category>} name attribute back into the typed model; used by
     * {@link IqQueryPrivacySettingsResponse.Success#of(Stanza, Stanza)}
     * and
     * {@link IqSetPrivacyResponse.Success#of(Stanza, Stanza)}.
     *
     * @implNote
     * This implementation walks {@link #values()} linearly; the enum is small (nine constants) so
     * an iteration is cheaper than building a static map. Unknown tokens (including the six
     * categories WA Web silently skips) resolve to {@link Optional#empty()}.
     *
     * @param wire the wire token; may be {@code null}
     * @return the resolved constant, or {@link Optional#empty()} when no constant matches or when
     *         {@code wire} is {@code null}
     */
    public static Optional<IqQueryPrivacySettingsCategoryName> fromWire(String wire) {
        if (wire == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wire.equals(wire)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
