package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Optional;

/**
 * The audiences accepted by the {@link PrivacySettingType#ONLINE} setting.
 *
 * <p>The online presence indicator can be shown to everyone, or it can mirror the last-seen
 * audience so the two presence surfaces share a single configuration.
 */
public sealed interface OnlinePrivacyValue extends PrivacySettingValue
        permits OnlinePrivacyValue.Everyone, OnlinePrivacyValue.MatchLastSeen {
    /**
     * {@inheritDoc}
     *
     * @return {@link PrivacySettingType#ONLINE}
     */
    @Override
    default PrivacySettingType<OnlinePrivacyValue> type() {
        return PrivacySettingType.ONLINE;
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
     * Resolves a server value token into the matching online presence audience.
     *
     * @param token    the server value token
     * @param excluded the blocklist applied to a contacts-except audience, otherwise ignored
     * @return the matching audience, or empty if the token is not accepted by this setting
     */
    static Optional<OnlinePrivacyValue> of(String token, List<Jid> excluded) {
        return switch (token) {
            case "all" -> Optional.of(new Everyone());
            case "match_last_seen" -> Optional.of(new MatchLastSeen());
            default -> Optional.empty();
        };
    }

    /**
     * The audience that includes every WhatsApp user.
     */
    record Everyone() implements OnlinePrivacyValue {
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
     * The audience that mirrors the configured last-seen audience.
     */
    record MatchLastSeen() implements OnlinePrivacyValue {
        /**
         * {@inheritDoc}
         *
         * @return {@code "match_last_seen"}
         */
        @Override
        public String token() {
            return "match_last_seen";
        }
    }
}
