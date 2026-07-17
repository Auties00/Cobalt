package com.github.auties00.cobalt.wire.linked.bot.rendering;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Metadata describing the user's usage quotas for premium AI bot features.
 *
 * <p>Certain bot features, such as the
 * {@linkplain BotFeatureQuotaMetadata.BotFeatureType#REASONING_FEATURE reasoning mode},
 * have limited usage quotas. This metadata carries per-feature quota
 * information including the number of remaining uses and when the quota resets.
 *
 * <p>This type is referenced from
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata BotMetadata} as the
 * {@code botQuotaMetadata} field (protobuf index 23).
 */
@ProtobufMessage(name = "BotQuotaMetadata")
public final class BotQuotaMetadata {
    /**
     * The list of per-feature quota entries.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotFeatureQuotaMetadata> botFeatureQuotaMetadata;


    /**
     * Constructs a new {@code BotQuotaMetadata} with the specified quota entries.
     *
     * @param botFeatureQuotaMetadata the per-feature quota entries, or {@code null}
     */
    BotQuotaMetadata(List<BotFeatureQuotaMetadata> botFeatureQuotaMetadata) {
        this.botFeatureQuotaMetadata = botFeatureQuotaMetadata;
    }

    /**
     * Returns an unmodifiable view of the per-feature quota entries.
     *
     * @return the list of feature quota entries, never {@code null}
     */
    public List<BotFeatureQuotaMetadata> botFeatureQuotaMetadata() {
        return botFeatureQuotaMetadata == null ? List.of() : Collections.unmodifiableList(botFeatureQuotaMetadata);
    }

    /**
     * Sets the per-feature quota entries.
     *
     * @param botFeatureQuotaMetadata the new list of quota entries, or {@code null}
     */
    public void setBotFeatureQuotaMetadata(List<BotFeatureQuotaMetadata> botFeatureQuotaMetadata) {
        this.botFeatureQuotaMetadata = botFeatureQuotaMetadata;
    }

    /**
     * Quota information for a single premium bot feature, including the
     * remaining number of uses and when the quota resets.
     */
    @ProtobufMessage(name = "BotQuotaMetadata.BotFeatureQuotaMetadata")
    public static final class BotFeatureQuotaMetadata {
        /**
         * The type of bot feature this quota applies to.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        BotFeatureQuotaMetadata.BotFeatureType featureType;

        /**
         * The number of remaining uses the user has for this feature before
         * the quota is exhausted.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        Integer remainingQuota;

        /**
         * The timestamp at which the quota resets, represented in epoch
         * milliseconds.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
        Instant expirationTimestamp;


        /**
         * Constructs a new {@code BotFeatureQuotaMetadata} with the specified values.
         *
         * @param featureType         the feature type, or {@code null}
         * @param remainingQuota      the remaining uses, or {@code null}
         * @param expirationTimestamp the quota reset timestamp, or {@code null}
         */
        BotFeatureQuotaMetadata(BotFeatureType featureType, Integer remainingQuota, Instant expirationTimestamp) {
            this.featureType = featureType;
            this.remainingQuota = remainingQuota;
            this.expirationTimestamp = expirationTimestamp;
        }

        /**
         * Returns the type of bot feature this quota applies to.
         *
         * @return an {@code Optional} describing the feature type, or an empty
         *         {@code Optional} if not set
         */
        public Optional<BotFeatureType> featureType() {
            return Optional.ofNullable(featureType);
        }

        /**
         * Returns the number of remaining uses for this feature.
         *
         * @return an {@code OptionalInt} describing the remaining quota, or an
         *         empty {@code OptionalInt} if not set
         */
        public OptionalInt remainingQuota() {
            return remainingQuota == null ? OptionalInt.empty() : OptionalInt.of(remainingQuota);
        }

        /**
         * Returns the timestamp at which the quota resets.
         *
         * @return an {@code Optional} describing the expiration timestamp, or an
         *         empty {@code Optional} if not set
         */
        public Optional<Instant> expirationTimestamp() {
            return Optional.ofNullable(expirationTimestamp);
        }

        /**
         * Sets the type of bot feature this quota applies to.
         *
         * @param featureType the new feature type, or {@code null}
         */
        public void setFeatureType(BotFeatureType featureType) {
            this.featureType = featureType;
    }

        /**
         * Sets the number of remaining uses for this feature.
         *
         * @param remainingQuota the new remaining quota, or {@code null}
         */
        public void setRemainingQuota(Integer remainingQuota) {
            this.remainingQuota = remainingQuota;
    }

        /**
         * Sets the timestamp at which the quota resets.
         *
         * @param expirationTimestamp the new expiration timestamp, or {@code null}
         */
        public void setExpirationTimestamp(Instant expirationTimestamp) {
            this.expirationTimestamp = expirationTimestamp;
    }

        /**
         * The type of premium bot feature that has a usage quota.
         */
        @ProtobufEnum(name = "BotQuotaMetadata.BotFeatureQuotaMetadata.BotFeatureType")
        public static enum BotFeatureType {
            /**
             * An unknown or unrecognized feature type.
             */
            UNKNOWN_FEATURE(0),

            /**
             * The reasoning feature, which uses a more capable AI model for
             * complex queries at the cost of quota usage.
             */
            REASONING_FEATURE(1);

            /**
             * Constructs a new {@code BotFeatureType} with the given protobuf index.
             *
             * @param index the protobuf enum index
             */
            BotFeatureType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf enum index for this feature type.
             */
            final int index;

            /**
             * Returns the protobuf index of this feature type.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }
}
