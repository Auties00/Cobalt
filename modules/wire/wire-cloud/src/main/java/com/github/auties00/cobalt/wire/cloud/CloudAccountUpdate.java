package com.github.auties00.cobalt.wire.cloud;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Business Account status change, decoded from an {@code account_update},
 * {@code account_alerts}, or {@code account_review_update} webhook change.
 *
 * <p>The platform reports account-level transitions through this family of changes: verification
 * outcomes, bans, messaging restrictions, policy violations, and review decisions. The fields are
 * a union of the payloads the three changes carry; absent members are empty. Because the platform may
 * combine signals in one payload (for example an {@code account_update} event alongside
 * {@code restriction_info}), the type is kept as a flat union rather than a hard variant split, and the
 * present facets are projected through the computed {@link #kind()}, {@link #asReview()}, and
 * {@link #asRestrictions()} accessors.
 */
public final class CloudAccountUpdate {
    /**
     * The transition event, for example {@code VERIFIED_ACCOUNT} or {@code DISABLED_UPDATE}, or
     * {@code null} when the change is a review decision.
     */
    private final String event;

    /**
     * The phone number the update applies to, or {@code null} when account-wide.
     */
    private final String phoneNumber;

    /**
     * The review decision, for example {@code APPROVED}, or {@code null} when the change is not a
     * review.
     */
    private final String decision;

    /**
     * The ban state, for example {@code SCHEDULE_FOR_DISABLE}, or {@code null} when no ban was
     * reported.
     */
    private final String banState;

    /**
     * The instant the ban takes effect, or {@code null} when no ban was reported.
     */
    private final Instant banDate;

    /**
     * The messaging restrictions in effect.
     */
    private final List<Restriction> restrictions;

    /**
     * The policy violation type, or {@code null} when no violation was reported.
     */
    private final String violationType;

    /**
     * Constructs a new account update.
     *
     * @param event         the transition event, or {@code null}
     * @param phoneNumber   the phone number, or {@code null}
     * @param decision      the review decision, or {@code null}
     * @param banState      the ban state, or {@code null}
     * @param banDate       the ban instant, or {@code null}
     * @param restrictions  the messaging restrictions, or {@code null} for none
     * @param violationType the policy violation type, or {@code null}
     */
    public CloudAccountUpdate(String event, String phoneNumber, String decision, String banState,
                              Instant banDate, List<Restriction> restrictions, String violationType) {
        this.event = event;
        this.phoneNumber = phoneNumber;
        this.decision = decision;
        this.banState = banState;
        this.banDate = banDate;
        this.restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
        this.violationType = violationType;
    }

    /**
     * Returns the transition event.
     *
     * @return an {@link Optional} carrying the event, or empty when the change is a review decision
     */
    public Optional<String> event() {
        return Optional.ofNullable(event);
    }

    /**
     * Returns the phone number the update applies to.
     *
     * @return an {@link Optional} carrying the phone number, or empty when account-wide
     */
    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Returns the review decision.
     *
     * @return an {@link Optional} carrying the decision, or empty when the change is not a review
     */
    public Optional<String> decision() {
        return Optional.ofNullable(decision);
    }

    /**
     * Returns the ban state.
     *
     * @return an {@link Optional} carrying the ban state, or empty when no ban was reported
     */
    public Optional<String> banState() {
        return Optional.ofNullable(banState);
    }

    /**
     * Returns the instant the ban takes effect.
     *
     * @return an {@link Optional} carrying the ban instant, or empty when no ban was reported
     */
    public Optional<Instant> banDate() {
        return Optional.ofNullable(banDate);
    }

    /**
     * Returns the messaging restrictions in effect.
     *
     * @return an unmodifiable list of restrictions, empty when none were reported
     */
    public List<Restriction> restrictions() {
        return restrictions;
    }

    /**
     * Returns the policy violation type.
     *
     * @return an {@link Optional} carrying the violation type, or empty when none was reported
     */
    public Optional<String> violationType() {
        return Optional.ofNullable(violationType);
    }

    /**
     * Returns the primary facet carried by this update.
     *
     * <p>The platform may co-send several signals in one payload; this accessor names the most specific
     * present facet for a quick top-level branch: {@link Kind#REVIEW} when a {@link #decision()} is
     * present, {@link Kind#BAN} when a {@link #banState()} is present, {@link Kind#RESTRICTIONS} when any
     * {@link #restrictions()} are present, {@link Kind#STATUS} when only an {@link #event()} is present,
     * and {@link Kind#UNKNOWN} when none of those are present. To inspect co-sent facets use the
     * {@link #asReview()} and {@link #asRestrictions()} projections rather than this single value.
     *
     * @return the primary facet kind
     */
    public Kind kind() {
        if (decision != null) {
            return Kind.REVIEW;
        }
        if (banState != null) {
            return Kind.BAN;
        }
        if (!restrictions.isEmpty()) {
            return Kind.RESTRICTIONS;
        }
        if (event != null) {
            return Kind.STATUS;
        }
        return Kind.UNKNOWN;
    }

    /**
     * Returns the review-decision facet projected onto a {@link Review} view.
     *
     * <p>This is a computed projection over the flat {@link #decision()} field and is not a serialized
     * property.
     *
     * @return an {@link Optional} carrying the {@link Review}, or empty when no review decision was
     *         reported
     */
    public Optional<Review> asReview() {
        return Optional.ofNullable(decision).map(Review::new);
    }

    /**
     * Returns the restrictions facet projected onto a {@link Restrictions} view.
     *
     * <p>This is a computed projection over the flat {@link #restrictions()} and {@link #violationType()}
     * fields and is not a serialized property; it is present whenever any restriction or a violation type
     * was reported.
     *
     * @return an {@link Optional} carrying the {@link Restrictions}, or empty when none was reported
     */
    public Optional<Restrictions> asRestrictions() {
        if (restrictions.isEmpty() && violationType == null) {
            return Optional.empty();
        }
        return Optional.of(new Restrictions(restrictions, violationType));
    }

    /**
     * The primary facet kind of a {@link CloudAccountUpdate}.
     *
     * <p>Names the most specific facet present on an update so a consumer can branch at the top level.
     * Because the platform may co-send facets, this is a hint, not an exclusive discriminator.
     */
    public enum Kind {
        /**
         * No recognised facet was present.
         */
        UNKNOWN,

        /**
         * A status transition carrying an {@link CloudAccountUpdate#event() event}.
         */
        STATUS,

        /**
         * A review decision carrying a {@link CloudAccountUpdate#decision() decision}.
         */
        REVIEW,

        /**
         * A ban carrying a {@link CloudAccountUpdate#banState() ban state}.
         */
        BAN,

        /**
         * One or more messaging {@link CloudAccountUpdate#restrictions() restrictions}.
         */
        RESTRICTIONS
    }

    /**
     * The review-decision projection of a {@link CloudAccountUpdate}.
     *
     * <p>Carries the review {@linkplain #decision() decision}, for example {@code APPROVED}.
     */
    public static final class Review {
        /**
         * The review decision.
         */
        private final String decision;

        /**
         * Constructs a new review projection.
         *
         * @param decision the review decision
         * @throws NullPointerException if {@code decision} is {@code null}
         */
        Review(String decision) {
            this.decision = Objects.requireNonNull(decision, "decision must not be null");
        }

        /**
         * Returns the review decision.
         *
         * @return the decision, for example {@code APPROVED}
         */
        public String decision() {
            return decision;
        }
    }

    /**
     * The restrictions projection of a {@link CloudAccountUpdate}.
     *
     * <p>Carries the messaging {@linkplain #restrictions() restrictions} in effect and the optional
     * policy {@linkplain #violationType() violation type} that drove them.
     */
    public static final class Restrictions {
        /**
         * The messaging restrictions in effect.
         */
        private final List<Restriction> restrictions;

        /**
         * The policy violation type, or {@code null} when none was reported.
         */
        private final String violationType;

        /**
         * Constructs a new restrictions projection.
         *
         * @param restrictions  the messaging restrictions, or {@code null} for none
         * @param violationType the policy violation type, or {@code null}
         */
        Restrictions(List<Restriction> restrictions, String violationType) {
            this.restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
            this.violationType = violationType;
        }

        /**
         * Returns the messaging restrictions in effect.
         *
         * @return an unmodifiable list of restrictions, empty when none were reported
         */
        public List<Restriction> restrictions() {
            return restrictions;
        }

        /**
         * Returns the policy violation type.
         *
         * @return an {@link Optional} carrying the violation type, or empty when none was reported
         */
        public Optional<String> violationType() {
            return Optional.ofNullable(violationType);
        }
    }

    /**
     * A single messaging restriction reported by an account update.
     */
    public static final class Restriction {
        /**
         * The restriction type, for example {@code RESTRICTED_ADD_PHONE_NUMBER_ACTION}.
         */
        private final String restrictionType;

        /**
         * The instant the restriction expires, or {@code null} when indefinite.
         */
        private final Instant expiration;

        /**
         * Constructs a new restriction.
         *
         * @param restrictionType the restriction type
         * @param expiration      the expiration instant, or {@code null} when indefinite
         * @throws NullPointerException if {@code restrictionType} is {@code null}
         */
        public Restriction(String restrictionType, Instant expiration) {
            this.restrictionType = Objects.requireNonNull(restrictionType, "restrictionType must not be null");
            this.expiration = expiration;
        }

        /**
         * Returns the restriction type.
         *
         * @return the restriction type
         */
        public String restrictionType() {
            return restrictionType;
        }

        /**
         * Returns the instant the restriction expires.
         *
         * @return an {@link Optional} carrying the expiration, or empty when indefinite
         */
        public Optional<Instant> expiration() {
            return Optional.ofNullable(expiration);
        }
    }
}
