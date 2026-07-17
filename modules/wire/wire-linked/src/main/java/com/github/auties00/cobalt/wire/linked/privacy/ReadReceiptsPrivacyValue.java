package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Optional;

/**
 * The audiences accepted by the {@link PrivacySettingType#READ_RECEIPTS} setting.
 *
 * <p>This controls whether read receipts are exchanged in one-to-one chats: they are either
 * shared with everyone or with nobody.
 */
public sealed interface ReadReceiptsPrivacyValue extends PrivacySettingValue
        permits ReadReceiptsPrivacyValue.Everyone, ReadReceiptsPrivacyValue.Nobody {
    /**
     * {@inheritDoc}
     *
     * @return {@link PrivacySettingType#READ_RECEIPTS}
     */
    @Override
    default PrivacySettingType<ReadReceiptsPrivacyValue> type() {
        return PrivacySettingType.READ_RECEIPTS;
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
     * Resolves a server value token into the matching read-receipts audience.
     *
     * @param token    the server value token
     * @param excluded the blocklist applied to a contacts-except audience, otherwise ignored
     * @return the matching audience, or empty if the token is not accepted by this setting
     */
    static Optional<ReadReceiptsPrivacyValue> of(String token, List<Jid> excluded) {
        return switch (token) {
            case "all" -> Optional.of(new Everyone());
            case "none" -> Optional.of(new Nobody());
            default -> Optional.empty();
        };
    }

    /**
     * The audience that includes every WhatsApp user.
     */
    record Everyone() implements ReadReceiptsPrivacyValue {
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
     * The audience that includes no other user.
     */
    record Nobody() implements ReadReceiptsPrivacyValue {
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
