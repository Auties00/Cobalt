package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Optional;

/**
 * The audiences accepted by the {@link PrivacySettingType#CALL_ADD} setting.
 *
 * <p>This controls who can add the user to ongoing group calls: everyone, every user the local
 * user has previously interacted with, or every saved contact.
 */
public sealed interface CallAddPrivacyValue extends PrivacySettingValue
        permits CallAddPrivacyValue.Everyone, CallAddPrivacyValue.Known, CallAddPrivacyValue.Contacts {
    /**
     * {@inheritDoc}
     *
     * @return {@link PrivacySettingType#CALL_ADD}
     */
    @Override
    default PrivacySettingType<CallAddPrivacyValue> type() {
        return PrivacySettingType.CALL_ADD;
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
     * Resolves a server value token into the matching call-add audience.
     *
     * @param token    the server value token
     * @param excluded the blocklist applied to a contacts-except audience, otherwise ignored
     * @return the matching audience, or empty if the token is not accepted by this setting
     */
    static Optional<CallAddPrivacyValue> of(String token, List<Jid> excluded) {
        return switch (token) {
            case "all" -> Optional.of(new Everyone());
            case "known" -> Optional.of(new Known());
            case "contacts" -> Optional.of(new Contacts());
            default -> Optional.empty();
        };
    }

    /**
     * The audience that includes every WhatsApp user.
     */
    record Everyone() implements CallAddPrivacyValue {
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
     * The audience that includes users the local user has previously interacted with.
     */
    record Known() implements CallAddPrivacyValue {
        /**
         * {@inheritDoc}
         *
         * @return {@code "known"}
         */
        @Override
        public String token() {
            return "known";
        }
    }

    /**
     * The audience that includes every saved contact.
     */
    record Contacts() implements CallAddPrivacyValue {
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
