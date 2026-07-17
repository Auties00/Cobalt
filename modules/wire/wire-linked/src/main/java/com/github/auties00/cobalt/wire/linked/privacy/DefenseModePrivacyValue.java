package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Optional;

/**
 * The enablement tier of WhatsApp Defense Mode, accepted by the
 * {@link PrivacySettingType#DEFENSE_MODE} setting.
 *
 * <p>Defense Mode quarantines unsolicited messages from senders that are not in the user's
 * address book. It is either disabled or enabled at the standard tier.
 */
public sealed interface DefenseModePrivacyValue extends PrivacySettingValue
        permits DefenseModePrivacyValue.Off, DefenseModePrivacyValue.OnStandard {
    /**
     * {@inheritDoc}
     *
     * @return {@link PrivacySettingType#DEFENSE_MODE}
     */
    @Override
    default PrivacySettingType<DefenseModePrivacyValue> type() {
        return PrivacySettingType.DEFENSE_MODE;
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
     * Resolves a server value token into the matching defense mode tier.
     *
     * @param token    the server value token
     * @param excluded the blocklist applied to a contacts-except audience, otherwise ignored
     * @return the matching tier, or empty if the token is not accepted by this setting
     */
    static Optional<DefenseModePrivacyValue> of(String token, List<Jid> excluded) {
        return switch (token) {
            case "off" -> Optional.of(new Off());
            case "on_standard" -> Optional.of(new OnStandard());
            default -> Optional.empty();
        };
    }

    /**
     * The tier in which defense mode is disabled.
     */
    record Off() implements DefenseModePrivacyValue {
        /**
         * {@inheritDoc}
         *
         * @return {@code "off"}
         */
        @Override
        public String token() {
            return "off";
        }
    }

    /**
     * The tier in which defense mode is enabled at the standard tier.
     */
    record OnStandard() implements DefenseModePrivacyValue {
        /**
         * {@inheritDoc}
         *
         * @return {@code "on_standard"}
         */
        @Override
        public String token() {
            return "on_standard";
        }
    }
}
