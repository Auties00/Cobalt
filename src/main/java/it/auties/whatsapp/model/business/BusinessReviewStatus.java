package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * An enumeration of possible ReviewStatuses.
 */
@ProtobufEnum
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
    OUTDATED
}
