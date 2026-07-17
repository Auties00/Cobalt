package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Optional;

/**
 * The audiences accepted by the {@link PrivacySettingType#ABOUT} setting.
 *
 * <p>This governs the about-text visibility, not the Status story feed audience, which is a
 * separate setting. The about text can be shown to everyone, to every saved contact, to every
 * contact except an explicit blocklist, or to nobody. The contacts-except audience carries its
 * blocklist inline through {@link ContactsExcept#excluded()}.
 */
public sealed interface AboutPrivacyValue extends PrivacySettingValue
        permits AboutPrivacyValue.Everyone, AboutPrivacyValue.Contacts,
        AboutPrivacyValue.ContactsExcept, AboutPrivacyValue.Nobody {
    /**
     * {@inheritDoc}
     *
     * @return {@link PrivacySettingType#ABOUT}
     */
    @Override
    default PrivacySettingType<AboutPrivacyValue> type() {
        return PrivacySettingType.ABOUT;
    }

    /**
     * {@inheritDoc}
     *
     * @return an empty list; overridden by {@link ContactsExcept} to carry its blocklist
     */
    @Override
    default List<Jid> excluded() {
        return List.of();
    }

    /**
     * Resolves a server value token into the matching about-text audience.
     *
     * @param token    the server value token
     * @param excluded the blocklist applied to {@link ContactsExcept}, otherwise ignored
     * @return the matching audience, or empty if the token is not accepted by this setting
     */
    static Optional<AboutPrivacyValue> of(String token, List<Jid> excluded) {
        return switch (token) {
            case "all" -> Optional.of(new Everyone());
            case "contacts" -> Optional.of(new Contacts());
            case "contact_blacklist" -> Optional.of(new ContactsExcept(excluded));
            case "none" -> Optional.of(new Nobody());
            default -> Optional.empty();
        };
    }

    /**
     * The audience that includes every WhatsApp user.
     */
    record Everyone() implements AboutPrivacyValue {
        /**
         * {@inheritDoc}
         *
         * @return {@code "all"}
         */
        @Override
        public String token() {
            return "all";
        }
    }

    /**
     * The audience that includes every saved contact.
     */
    record Contacts() implements AboutPrivacyValue {
        /**
         * {@inheritDoc}
         *
         * @return {@code "contacts"}
         */
        @Override
        public String token() {
            return "contacts";
        }
    }

    /**
     * The audience that includes every saved contact except an explicit blocklist.
     *
     * @param excluded the contacts denied access; {@code null} is normalized to an empty list
     */
    record ContactsExcept(List<Jid> excluded) implements AboutPrivacyValue {
        /**
         * Normalizes the blocklist so {@link #excluded()} never returns {@code null}.
         *
         * @param excluded the contacts denied access, or {@code null}
         */
        public ContactsExcept {
            excluded = excluded == null ? List.of() : List.copyOf(excluded);
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code "contact_blacklist"}
         */
        @Override
        public String token() {
            return "contact_blacklist";
        }
    }

    /**
     * The audience that includes no other user.
     */
    record Nobody() implements AboutPrivacyValue {
        /**
         * {@inheritDoc}
         *
         * @return {@code "none"}
         */
        @Override
        public String token() {
            return "none";
        }
    }
}
