package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the compliance review status of a product or collection in a
 * WhatsApp Business catalog.
 *
 * <p>Products and collections submitted to a WhatsApp Business catalog
 * undergo a compliance review process before they become visible to
 * customers. The review ensures that listed items conform to WhatsApp's
 * commerce policies. This enum captures the possible outcomes of that
 * review process.
 *
 * <p>A product must have an {@link #APPROVED} review status (and not be
 * {@linkplain BusinessCatalogEntry#hidden() hidden}) to be visible in the
 * storefront. Products with a {@link #REJECTED} status may be eligible for
 * appeal, depending on the rejection reason. The {@link #OUTDATED} status
 * indicates that a previous review determination is no longer current and
 * a new review cycle may be required.
 *
 * <p>The review status can be resolved from its string name using the
 * {@link #ofName(String)} method, which performs case-insensitive matching.
 */
@ProtobufEnum
public enum BusinessReviewStatus {
    /**
     * No compliance review has been performed on this item.
     *
     * <p>This value indicates that the review process has not yet been
     * initiated for the product or collection.
     */
    NO_REVIEW,

    /**
     * The item has been submitted and is awaiting compliance review.
     *
     * <p>Products in this state are typically not yet visible to customers
     * until the review is completed.
     */
    PENDING,

    /**
     * The item did not pass compliance review and is not visible to
     * customers.
     *
     * <p>Depending on the rejection reason, the business owner may be able
     * to appeal the decision or edit the product to address the compliance
     * issue and resubmit it.
     */
    REJECTED,

    /**
     * The item passed compliance review and is eligible to be visible to
     * customers.
     *
     * <p>An approved product is displayed in the storefront unless it has
     * been explicitly {@linkplain BusinessCatalogEntry#hidden() hidden} by
     * the business owner or flagged for sanctions.
     */
    APPROVED,

    /**
     * The previous review determination is no longer current.
     *
     * <p>This status indicates that a new review cycle may be required,
     * typically because the product was modified after its last review or
     * because review policies have changed.
     */
    OUTDATED;

    /**
     * Lookup map from lowercase status names to their corresponding enum
     * constants.
     */
    private static final Map<String, BusinessReviewStatus> PRETTY_NAME_TO_REVIEW_STATUS = Arrays.stream(BusinessReviewStatus.values())
            .collect(Collectors.toMap(entry -> entry.name().toLowerCase(), Function.identity()));

    /**
     * Returns the review status constant matching the given name.
     *
     * <p>The lookup is case-insensitive (for example, {@code "approved"},
     * {@code "APPROVED"}, and {@code "Approved"} all match
     * {@link #APPROVED}). This matches the format used in catalog query
     * responses. For collection-specific prefixed forms such as
     * {@code "STATUS_APPROVED"}, callers should strip the {@code "STATUS_"}
     * prefix before calling this method.
     *
     * @param reviewStatus the review status name to look up, or
     *                     {@code null}
     * @return an {@code Optional} containing the matching review status
     *         constant, or an empty {@code Optional} if
     *         {@code reviewStatus} is {@code null} or no match is found
     */
    public static Optional<BusinessReviewStatus> ofName(String reviewStatus) {
        return reviewStatus == null
                ? Optional.empty()
                : Optional.ofNullable(PRETTY_NAME_TO_REVIEW_STATUS.get(reviewStatus.toLowerCase()));
    }
}
