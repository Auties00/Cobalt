package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Optional;

/**
 * The audiences accepted by the {@link PrivacySettingType#GROUP_ADD} setting.
 *
 * <p>This controls who can add the user to new groups without an invitation: everyone, every
 * saved contact, every contact except an explicit blocklist, or nobody. The contacts-except
 * audience carries its blocklist inline through {@link ContactsExcept#excluded()}.
 */
public sealed interface GroupAddPrivacyValue extends PrivacySettingValue
        permits GroupAddPrivacyValue.Everyone, GroupAddPrivacyValue.Contacts,
        GroupAddPrivacyValue.ContactsExcept, GroupAddPrivacyValue.Nobody {
    /**
     * {@inheritDoc}
     *
     * @return {@link PrivacySettingType#GROUP_ADD}
     */
    @Override
    default PrivacySettingType<GroupAddPrivacyValue> type() {
        return PrivacySettingType.GROUP_ADD;
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
     * Resolves a server value token into the matching group-add audience.
     *
     * @param token    the server value token
     * @param excluded the blocklist applied to {@link ContactsExcept}, otherwise ignored
     * @return the matching audience, or empty if the token is not accepted by this setting
     */
    static Optional<GroupAddPrivacyValue> of(String token, List<Jid> excluded) {
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
    record Everyone() implements GroupAddPrivacyValue {
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
    record Contacts() implements GroupAddPrivacyValue {
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
    record ContactsExcept(List<Jid> excluded) implements GroupAddPrivacyValue {
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
    record Nobody() implements GroupAddPrivacyValue {
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
