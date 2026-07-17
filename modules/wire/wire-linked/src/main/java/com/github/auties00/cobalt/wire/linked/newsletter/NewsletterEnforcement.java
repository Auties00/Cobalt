package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single enforcement action recorded against a newsletter.
 *
 * <p>The enforcement query groups every action the moderation pipeline
 * has taken on a given channel into four categories: profile-picture
 * deletions, account suspensions, violating message removals, and
 * geographic suspensions. This type collapses all four categories into
 * a single descriptor with a {@link #category()} discriminator, so
 * callers can iterate over a flat list rather than four parallel lists.
 *
 * <p>Each enforcement entry exposes the violation category and source
 * strings, the relay-assigned enforcement identifier, the moment the
 * action was taken, the current appeal state and (when applicable) the
 * moment any appeal was filed. The {@link #policyExplanation()}
 * accessor returns the localised user-facing copy that explains the
 * policy a violation breached. Category-specific extras (the impacted
 * server message identifiers, geo-suspension countries) live under
 * {@link #affectedMessageIds()} and {@link #affectedCountries()}.
 */
@ProtobufMessage
public final class NewsletterEnforcement {
    /**
     * The category of enforcement action.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Category category;

    /**
     * The relay-assigned identifier of this enforcement action.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String enforcementId;

    /**
     * The wire-level violation category string (for example
     * {@code "HATE_SPEECH"}).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String violationCategory;

    /**
     * The source that triggered the enforcement (for example
     * {@code "USER_REPORT"}).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String source;

    /**
     * The instant at which the enforcement action was taken.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant creationTime;

    /**
     * The state of the appeal the channel owner has filed against this
     * enforcement, if any.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String appealState;

    /**
     * The instant at which the appeal was filed, if any.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant appealCreationTime;

    /**
     * The localised user-facing copy that explains the policy this
     * action enforces.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String policyExplanation;

    /**
     * The server-assigned identifiers of the messages this enforcement
     * action removed, when the category is
     * {@link Category#VIOLATING_MESSAGES}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    List<String> affectedMessageIds;

    /**
     * The ISO country codes affected by this enforcement, when the
     * category is {@link Category#GEOSUSPENSIONS}.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    List<String> affectedCountries;

    /**
     * Constructs a new {@code NewsletterEnforcement}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param category            the enforcement category, may be {@code null}
     * @param enforcementId       the enforcement identifier, may be {@code null}
     * @param violationCategory   the violation category, may be {@code null}
     * @param source              the enforcement source, may be {@code null}
     * @param creationTime        the moment the action was taken, may be {@code null}
     * @param appealState         the appeal state, may be {@code null}
     * @param appealCreationTime  the moment the appeal was filed, may be {@code null}
     * @param policyExplanation   the localised explanatory copy, may be {@code null}
     * @param affectedMessageIds  the affected message identifiers; defaulted to
     *                            an empty list when {@code null}
     * @param affectedCountries   the affected country codes; defaulted to an
     *                            empty list when {@code null}
     */
    NewsletterEnforcement(Category category, String enforcementId, String violationCategory, String source, Instant creationTime, String appealState, Instant appealCreationTime, String policyExplanation, List<String> affectedMessageIds, List<String> affectedCountries) {
        this.category = category;
        this.enforcementId = enforcementId;
        this.violationCategory = violationCategory;
        this.source = source;
        this.creationTime = creationTime;
        this.appealState = appealState;
        this.appealCreationTime = appealCreationTime;
        this.policyExplanation = policyExplanation;
        this.affectedMessageIds = affectedMessageIds == null ? List.of() : List.copyOf(affectedMessageIds);
        this.affectedCountries = affectedCountries == null ? List.of() : List.copyOf(affectedCountries);
    }

    /**
     * Returns the enforcement category.
     *
     * @return an {@link Optional} carrying the category, or empty when
     *         not classified
     */
    public Optional<Category> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the relay-assigned identifier of this enforcement action.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         not reported
     */
    public Optional<String> enforcementId() {
        return Optional.ofNullable(enforcementId);
    }

    /**
     * Returns the wire-level violation category string.
     *
     * @return an {@link Optional} carrying the category string, or empty
     *         when not reported
     */
    public Optional<String> violationCategory() {
        return Optional.ofNullable(violationCategory);
    }

    /**
     * Returns the source that triggered the enforcement.
     *
     * @return an {@link Optional} carrying the source, or empty when not
     *         reported
     */
    public Optional<String> source() {
        return Optional.ofNullable(source);
    }

    /**
     * Returns the instant at which the enforcement action was taken.
     *
     * @return an {@link Optional} carrying the creation instant, or
     *         empty when not reported
     */
    public Optional<Instant> creationTime() {
        return Optional.ofNullable(creationTime);
    }

    /**
     * Returns the state of any appeal filed against this enforcement.
     *
     * @return an {@link Optional} carrying the state, or empty when no
     *         appeal has been filed
     */
    public Optional<String> appealState() {
        return Optional.ofNullable(appealState);
    }

    /**
     * Returns the instant at which the appeal was filed.
     *
     * @return an {@link Optional} carrying the appeal instant, or empty
     *         when no appeal has been filed
     */
    public Optional<Instant> appealCreationTime() {
        return Optional.ofNullable(appealCreationTime);
    }

    /**
     * Returns the localised user-facing copy that explains the policy
     * this action enforces.
     *
     * @return an {@link Optional} carrying the explanatory copy, or
     *         empty when not reported
     */
    public Optional<String> policyExplanation() {
        return Optional.ofNullable(policyExplanation);
    }

    /**
     * Returns the server-assigned identifiers of the messages this
     * enforcement action removed.
     *
     * @return an unmodifiable list of message identifiers, never
     *         {@code null}
     */
    public List<String> affectedMessageIds() {
        return Collections.unmodifiableList(affectedMessageIds);
    }

    /**
     * Returns the ISO country codes affected by this enforcement.
     *
     * @return an unmodifiable list of country codes, never {@code null}
     */
    public List<String> affectedCountries() {
        return Collections.unmodifiableList(affectedCountries);
    }

    /**
     * Returns whether this enforcement equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterEnforcement}
     *         carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterEnforcement that
                && category == that.category
                && Objects.equals(enforcementId, that.enforcementId)
                && Objects.equals(violationCategory, that.violationCategory)
                && Objects.equals(source, that.source)
                && Objects.equals(creationTime, that.creationTime)
                && Objects.equals(appealState, that.appealState)
                && Objects.equals(appealCreationTime, that.appealCreationTime)
                && Objects.equals(policyExplanation, that.policyExplanation)
                && Objects.equals(affectedMessageIds, that.affectedMessageIds)
                && Objects.equals(affectedCountries, that.affectedCountries);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(category, enforcementId, violationCategory, source, creationTime, appealState, appealCreationTime, policyExplanation, affectedMessageIds, affectedCountries);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterEnforcement[category=" + category +
                ", enforcementId=" + enforcementId +
                ", violationCategory=" + violationCategory + ']';
    }

    /**
     * Discriminates the four enforcement categories returned by the
     * relay.
     */
    @ProtobufEnum
    public enum Category {
        /**
         * The channel's profile picture has been deleted.
         */
        PROFILE_PICTURE_DELETION(0),

        /**
         * The channel has been suspended.
         */
        SUSPENSION(1),

        /**
         * One or more messages have been removed for violating policy.
         */
        VIOLATING_MESSAGES(2),

        /**
         * The channel has been suspended in one or more geographies.
         */
        GEOSUSPENSIONS(3);

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Constructs a new enum constant bound to the supplied protobuf
         * wire index.
         *
         * @param index the protobuf wire index
         */
        Category(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}
