package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;

/**
 * Closed set of visibility tokens that a privacy {@link IqQueryPrivacySettingsCategoryName} may take
 * on the wire.
 * <p>
 * Not every constant is legal for every category; the per-category constraints documented on each
 * value mirror the {@code VISIBILITY}, {@code ALL_NONE}, {@code ONLINE_VISIBILITY},
 * {@code ALL_CONTACTS}, {@code CALL_ADD}, and {@code DEFENSE_MODE_STATE} enum tables exported by WA
 * Web's {@code WAWebPrivacySettings} constants module. These constants are used when constructing an
 * {@link IqSetPrivacyRequest} and when reading the
 * {@link IqQueryPrivacySettingsResponse.Success#categories()} map.
 *
 * @implNote
 * This implementation collapses the six WA Web sub-tables into a single union enum; the
 * {@code error} sentinel that WA Web's {@code *_WITH_ERROR} tables emit when the relay rejects an
 * individual category is intentionally not modelled. The rejection signal is surfaced instead by an
 * empty {@link IqSetPrivacyResponse.CategoryOutcome#value()} on the typed parse path.
 */
@WhatsAppWebModule(moduleName = "WAWebPrivacySettings")
public enum IqQueryPrivacySettingsVisibility {
    /**
     * The {@code all} token.
     * <p>
     * Legal on every category. Means visible to everyone for visibility categories, on for boolean
     * toggles, and anyone can add for permission categories.
     */
    ALL("all"),

    /**
     * The {@code contacts} token.
     * <p>
     * Legal on {@link IqQueryPrivacySettingsCategoryName#LAST_SEEN},
     * {@link IqQueryPrivacySettingsCategoryName#PROFILE_PICTURE},
     * {@link IqQueryPrivacySettingsCategoryName#ABOUT},
     * {@link IqQueryPrivacySettingsCategoryName#GROUP_ADD},
     * {@link IqQueryPrivacySettingsCategoryName#CALL_ADD}, and
     * {@link IqQueryPrivacySettingsCategoryName#MESSAGES}. Restricts visibility to entries in the
     * user's address book.
     */
    CONTACTS("contacts"),

    /**
     * The {@code contact_blacklist} token.
     * <p>
     * Legal on {@link IqQueryPrivacySettingsCategoryName#LAST_SEEN},
     * {@link IqQueryPrivacySettingsCategoryName#PROFILE_PICTURE},
     * {@link IqQueryPrivacySettingsCategoryName#ABOUT}, and
     * {@link IqQueryPrivacySettingsCategoryName#GROUP_ADD}. Means visible to contacts except the
     * named exclusion set, and requires the request to carry a non-empty
     * {@link IqSetPrivacyRequest#users()} list together with the
     * {@link IqSetPrivacyRequest#dhash()} digest of that list (matching WA Web's
     * {@code privacyDisallowedList} persistence).
     */
    CONTACT_BLACKLIST("contact_blacklist"),

    /**
     * The {@code none} token.
     * <p>
     * Means hidden from everyone or off. Legal on the visibility categories
     * ({@link IqQueryPrivacySettingsCategoryName#LAST_SEEN},
     * {@link IqQueryPrivacySettingsCategoryName#PROFILE_PICTURE},
     * {@link IqQueryPrivacySettingsCategoryName#ABOUT}) and on the read-receipts toggle
     * ({@link IqQueryPrivacySettingsCategoryName#READ_RECEIPTS}).
     */
    NONE("none"),

    /**
     * The {@code match_last_seen} token.
     * <p>
     * Legal only on {@link IqQueryPrivacySettingsCategoryName#ONLINE}; means the online indicator
     * follows whatever {@link IqQueryPrivacySettingsCategoryName#LAST_SEEN} is set to.
     */
    MATCH_LAST_SEEN("match_last_seen"),

    /**
     * The {@code known} token.
     * <p>
     * Legal only on {@link IqQueryPrivacySettingsCategoryName#CALL_ADD}; restricts who may add the
     * user to a group call to peers the user has previously interacted with.
     */
    KNOWN("known"),

    /**
     * The {@code off} token.
     * <p>
     * Legal only on {@link IqQueryPrivacySettingsCategoryName#DEFENSE_MODE}; signals defense mode is
     * disabled (no server-side quarantine of unsolicited messages).
     */
    OFF("off"),

    /**
     * The {@code on_standard} token.
     * <p>
     * Legal only on {@link IqQueryPrivacySettingsCategoryName#DEFENSE_MODE}; enables the standard
     * tier of server-side quarantine for messages from unknown senders.
     */
    ON_STANDARD("on_standard");

    /**
     * The wire token emitted in the {@code value} attribute of each {@code <category>} child.
     */
    private final String wire;

    /**
     * Constructs a visibility constant from its wire token.
     *
     * @param wire the wire token; never {@code null}
     */
    IqQueryPrivacySettingsVisibility(String wire) {
        this.wire = wire;
    }

    /**
     * Returns the wire token for this visibility.
     * <p>
     * Used when serialising a {@code <category value=...>} attribute on an
     * {@link IqSetPrivacyRequest}; consumers compare the enum value rather than pattern matching on
     * the string.
     *
     * @return the wire token; never {@code null}
     */
    public String wire() {
        return wire;
    }

    /**
     * Resolves the constant whose {@link #wire()} equals the supplied token.
     * <p>
     * Projects an inbound {@code value} attribute back into the typed model; used by
     * {@link IqQueryPrivacySettingsResponse.Success#of(Stanza, Stanza)}
     * and
     * {@link IqSetPrivacyResponse.Success#of(Stanza, Stanza)}.
     *
     * @implNote
     * This implementation walks {@link #values()} linearly; the enum is small enough that a static
     * map is not worth the construction cost. The WA Web {@code error} sentinel (emitted when the
     * relay rejected a per-category mutation) is intentionally not a constant and resolves to
     * {@link Optional#empty()}; callers handle that signal explicitly.
     *
     * @param wire the wire token; may be {@code null}
     * @return the resolved constant, or {@link Optional#empty()} when no constant matches, when
     *         {@code wire} is {@code null}, or when {@code wire} is the {@code error} marker
     */
    public static Optional<IqQueryPrivacySettingsVisibility> fromWire(String wire) {
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
