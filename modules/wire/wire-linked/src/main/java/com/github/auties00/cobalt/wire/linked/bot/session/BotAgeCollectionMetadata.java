package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata controlling age-verification collection for AI bot interactions on
 * WhatsApp.
 *
 * <p>Before a user can interact with certain AI features (such as Meta AI),
 * the client may need to collect and verify the user's age. This metadata
 * signals whether the user is eligible for age collection, whether the
 * client should present the age-collection UI, and which collection method
 * to use.
 *
 * <p>This metadata is carried inside
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata#botAgeCollectionMetadata()}
 * and is populated by the server when age gating is required for the
 * conversation.
 */
@ProtobufMessage(name = "BotAgeCollectionMetadata")
public final class BotAgeCollectionMetadata {
    /**
     * Whether the user is eligible for age collection. When {@code null},
     * the server has not yet determined eligibility.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean ageCollectionEligible;

    /**
     * Whether the client should trigger the age-collection UI flow when
     * rendering this message. When {@code null}, the field has not been set
     * by the server.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean shouldTriggerAgeCollectionOnClient;

    /**
     * The type of age-collection mechanism to use, such as a simple binary
     * over-18 check or a Meta Waffle experimentation-driven flow.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    AgeCollectionType ageCollectionType;


    /**
     * Constructs a new {@code BotAgeCollectionMetadata} with the specified
     * values.
     *
     * @param ageCollectionEligible             whether the user is eligible for
     *                                          age collection, or {@code null}
     * @param shouldTriggerAgeCollectionOnClient whether the client should
     *                                          present the age-collection UI,
     *                                          or {@code null}
     * @param ageCollectionType                 the collection mechanism to use,
     *                                          or {@code null}
     */
    BotAgeCollectionMetadata(Boolean ageCollectionEligible, Boolean shouldTriggerAgeCollectionOnClient, AgeCollectionType ageCollectionType) {
        this.ageCollectionEligible = ageCollectionEligible;
        this.shouldTriggerAgeCollectionOnClient = shouldTriggerAgeCollectionOnClient;
        this.ageCollectionType = ageCollectionType;
    }

    /**
     * Returns whether the user is eligible for age collection.
     *
     * @return {@code true} if the user is eligible, {@code false} if not
     *         eligible or if the field was not set by the server
     */
    public boolean ageCollectionEligible() {
        return ageCollectionEligible != null && ageCollectionEligible;
    }

    /**
     * Returns whether the client should trigger the age-collection UI flow
     * when rendering this message.
     *
     * @return {@code true} if the client should present the age-collection UI,
     *         {@code false} if not required or if the field was not set
     */
    public boolean shouldTriggerAgeCollectionOnClient() {
        return shouldTriggerAgeCollectionOnClient != null && shouldTriggerAgeCollectionOnClient;
    }

    /**
     * Returns the type of age-collection mechanism to use for this
     * interaction.
     *
     * @return an {@code Optional} describing the collection type, or an
     *         empty {@code Optional} if the server did not specify one
     */
    public Optional<AgeCollectionType> ageCollectionType() {
        return Optional.ofNullable(ageCollectionType);
    }

    /**
     * Sets whether the user is eligible for age collection.
     *
     * @param ageCollectionEligible the new eligibility flag, or {@code null}
     *                              to clear the value
     */
    public void setAgeCollectionEligible(Boolean ageCollectionEligible) {
        this.ageCollectionEligible = ageCollectionEligible;
    }

    /**
     * Sets whether the client should trigger the age-collection UI flow
     * when rendering this message.
     *
     * @param shouldTriggerAgeCollectionOnClient the new trigger flag, or
     *        {@code null} to clear the value
     */
    public void setShouldTriggerAgeCollectionOnClient(Boolean shouldTriggerAgeCollectionOnClient) {
        this.shouldTriggerAgeCollectionOnClient = shouldTriggerAgeCollectionOnClient;
    }

    /**
     * Sets the type of age-collection mechanism to use for this interaction.
     *
     * @param ageCollectionType the new collection type, or {@code null} to
     *                          clear the value
     */
    public void setAgeCollectionType(AgeCollectionType ageCollectionType) {
        this.ageCollectionType = ageCollectionType;
    }

    /**
     * Enumerates the mechanisms available for collecting and verifying a
     * user's age before granting access to AI bot features.
     */
    @ProtobufEnum(name = "BotAgeCollectionMetadata.AgeCollectionType")
    public static enum AgeCollectionType {
        /**
         * A simple binary over-18 age check where the user confirms whether
         * they are at least 18 years old.
         */
        OVER_18_BINARY(0),

        /**
         * A progressive age-collection flow managed by Meta's Waffle
         * experimentation framework, which may present different
         * verification steps depending on the user's experiment cohort.
         */
        WAFFLE(1);

        /**
         * Constructs a new age-collection type constant with the specified
         * protobuf index.
         *
         * @param index the protobuf enum index
         */
        AgeCollectionType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        /**
         * Returns the protobuf enum index of this age-collection type.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
