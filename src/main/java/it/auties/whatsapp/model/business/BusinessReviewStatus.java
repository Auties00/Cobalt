package it.auties.whatsapp.model.business;

import java.util.Locale;

/**
 * An enumeration of possible ReviewStatuses.
 */
public enum BusinessReviewStatus {
    /**
     * Indicates that no review has been performed.
     */
    NO_REVIEW,
    /**
     * Indicates that the review is pending.
     */
    PENDING,
    /**
     * Indicates that the review was rejected.
     */
    REJECTED,
    /**
     * Indicates that the review was approved.
     */
    APPROVED,
    /**
     * Indicates that the review is outdated.
     */
    OUTDATED;

    /**
     * Returns a ReviewStatus based on the given name.
     *
     * @param name the name of the ReviewStatus
     * @return a ReviewStatus
     */
    public static BusinessReviewStatus of(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
