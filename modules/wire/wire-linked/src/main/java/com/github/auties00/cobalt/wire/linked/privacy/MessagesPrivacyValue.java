package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Optional;

/**
 * The audiences accepted by the {@link PrivacySettingType#MESSAGES} setting.
 *
 * <p>This controls who can message the user without a prior conversation: everyone or every
 * saved contact.
 */
public sealed interface MessagesPrivacyValue extends PrivacySettingValue
        permits MessagesPrivacyValue.Everyone, MessagesPrivacyValue.Contacts {
    /**
     * {@inheritDoc}
     *
     * @return {@link PrivacySettingType#MESSAGES}
     */
    @Override
    default PrivacySettingType<MessagesPrivacyValue> type() {
        return PrivacySettingType.MESSAGES;
    }

    /**
     * {@inheritDoc}
     *
     * @return an empty list; this setting carries no refinement list
     */
    @Override
    default List<Jid> excluded() {
        return List.of();
    }

    /**
     * Resolves a server value token into the matching messages audience.
     *
     * @param token    the server value token
     * @param excluded the blocklist applied to a contacts-except audience, otherwise ignored
     * @return the matching audience, or empty if the token is not accepted by this setting
     */
    static Optional<MessagesPrivacyValue> of(String token, List<Jid> excluded) {
        return switch (token) {
            case "all" -> Optional.of(new Everyone());
            case "contacts" -> Optional.of(new Contacts());
            default -> Optional.empty();
        };
    }

    /**
     * The audience that includes every WhatsApp user.
     */
    record Everyone() implements MessagesPrivacyValue {
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
    record Contacts() implements MessagesPrivacyValue {
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
}
