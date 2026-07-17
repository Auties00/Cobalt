package com.github.auties00.cobalt.wire.linked.business.crossposting;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for checking the cross-posting eligibility of a set of
 * WhatsApp statuses against the targeted Meta destinations.
 *
 * <p>When the WhatsApp client posts a status it can optionally cross-post
 * the same media to Facebook or Instagram. Before showing the
 * cross-posting controls, the client asks the server whether the statuses
 * in question may be cross-posted to the targeted destinations and, if so,
 * which per-destination identities and parameters apply.
 *
 * <p>{@link #expirationTimes()} lists the request expiration instants the
 * server uses to bound how long the eligibility result stays valid; each
 * instant is carried with second precision. {@link #purposeClientPublicKey()}
 * is the per-call ephemeral public key the WhatsApp client posts so the
 * server can encrypt the eligibility result back at the calling client.
 * {@link #uniqueIds()} enumerates the unique identifiers of the statuses
 * whose eligibility is being checked. {@link #destinations()} names the
 * targeted cross-posting destinations (the Meta application paired with
 * its surface marker). {@link #sessionId()} groups eligibility checks
 * belonging to the same cross-posting session.
 */
public final class CrossPostingEligibilityQuery {
    /**
     * Request expiration instants the server uses to bound how long the
     * eligibility result stays valid. Never {@code null}; defaults to
     * {@link List#of()}.
     */
    private final List<Instant> expirationTimes;

    /**
     * Per-call ephemeral public key the WhatsApp client posts so the
     * server can encrypt the eligibility result back at the calling
     * client, base64-encoded. {@code null} when unset.
     */
    private final String purposeClientPublicKey;

    /**
     * Unique identifiers of the statuses whose eligibility is being
     * checked. Never {@code null}; defaults to {@link List#of()}.
     */
    private final List<String> uniqueIds;

    /**
     * Targeted cross-posting destinations. Never {@code null}; defaults
     * to {@link List#of()}.
     */
    private final List<CrossPostingDestination> destinations;

    /**
     * Cross-posting session identifier. {@code null} when unset.
     */
    private final String sessionId;

    /**
     * Constructs a new {@code CrossPostingEligibilityQuery}. The list
     * arguments may be {@code null} to default to {@link List#of()}; the
     * scalar arguments may be {@code null} to omit the corresponding
     * variable from the request.
     *
     * @param expirationTimes        the request expiration instants, or
     *                               {@code null} to default to empty
     * @param purposeClientPublicKey the per-call ephemeral public key, or
     *                               {@code null}
     * @param uniqueIds              the status unique identifiers, or
     *                               {@code null} to default to empty
     * @param destinations           the targeted cross-posting
     *                               destinations, or {@code null} to
     *                               default to empty
     * @param sessionId              the cross-posting session identifier,
     *                               or {@code null}
     */
    public CrossPostingEligibilityQuery(List<Instant> expirationTimes, String purposeClientPublicKey,
                                        List<String> uniqueIds, List<CrossPostingDestination> destinations,
                                        String sessionId) {
        this.expirationTimes = expirationTimes == null ? List.of() : List.copyOf(expirationTimes);
        this.purposeClientPublicKey = purposeClientPublicKey;
        this.uniqueIds = uniqueIds == null ? List.of() : List.copyOf(uniqueIds);
        this.destinations = destinations == null ? List.of() : List.copyOf(destinations);
        this.sessionId = sessionId;
    }

    /**
     * Returns the request expiration instants.
     *
     * @return an unmodifiable view of the instants; never {@code null},
     *         possibly empty
     */
    public List<Instant> expirationTimes() {
        return expirationTimes;
    }

    /**
     * Returns the per-call ephemeral public key.
     *
     * @return an {@link Optional} carrying the base64-encoded key, or
     *         empty when unset
     */
    public Optional<String> purposeClientPublicKey() {
        return Optional.ofNullable(purposeClientPublicKey);
    }

    /**
     * Returns the unique identifiers of the statuses being checked.
     *
     * @return an unmodifiable view of the identifiers; never {@code null},
     *         possibly empty
     */
    public List<String> uniqueIds() {
        return uniqueIds;
    }

    /**
     * Returns the targeted cross-posting destinations.
     *
     * @return an unmodifiable view of the destinations; never {@code null},
     *         possibly empty
     */
    public List<CrossPostingDestination> destinations() {
        return destinations;
    }

    /**
     * Returns the cross-posting session identifier.
     *
     * @return an {@link Optional} carrying the session identifier, or
     *         empty when unset
     */
    public Optional<String> sessionId() {
        return Optional.ofNullable(sessionId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CrossPostingEligibilityQuery) obj;
        return Objects.equals(expirationTimes, that.expirationTimes)
                && Objects.equals(purposeClientPublicKey, that.purposeClientPublicKey)
                && Objects.equals(uniqueIds, that.uniqueIds)
                && Objects.equals(destinations, that.destinations)
                && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expirationTimes, purposeClientPublicKey, uniqueIds, destinations, sessionId);
    }

    @Override
    public String toString() {
        return "CrossPostingEligibilityQuery[" +
                "expirationTimes=" + expirationTimes + ", " +
                "purposeClientPublicKey=" + purposeClientPublicKey + ", " +
                "uniqueIds=" + uniqueIds + ", " +
                "destinations=" + destinations + ", " +
                "sessionId=" + sessionId + ']';
    }
}
