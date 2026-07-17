package com.github.auties00.cobalt.wire.linked.business.promotion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One WhatsApp quick-promotion banner the server has selected as
 * eligible to show on a given client surface.
 *
 * <p>WhatsApp shows quick-promotion banners (small in-app cards
 * advertising a feature or an action the user might want to take) on
 * both consumer and Business surfaces. The server picks which banners
 * are eligible to show, with what content, how long the client may show
 * them, and how often: the pacing engine reads this metadata together
 * with the per-user impression counters to decide whether the banner is
 * actually displayed.
 *
 * <p>This model is one selected banner: its identifier, the validity
 * window the server pins the banner to, the rendering
 * {@linkplain QuickPromotionCreative creatives}, the dismissibility flag
 * resolved from the first creative, and an opaque encrypted logging blob
 * the client echoes back when it logs an interaction with the banner.
 */
@ProtobufMessage(name = "QuickPromotion")
public final class QuickPromotion {
    /**
     * Server-issued banner identifier, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String promotionId;

    /**
     * Rendering creatives shipped for this banner, in the order the
     * server returned them. Never {@code null}, possibly empty when the
     * server returned none.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<QuickPromotionCreative> creatives;

    /**
     * Instant at which the banner becomes eligible to show, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final Long validFromEpochSecond;

    /**
     * Instant at which the banner stops being eligible to show, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final Long validUntilEpochSecond;

    /**
     * Opaque encrypted logging blob the client echoes back when it logs
     * an interaction with the banner, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String loggingBlob;

    /**
     * Constructs a new {@code QuickPromotion}. {@code null}
     * {@code creatives} is coerced to an empty list; the other
     * reference arguments may be {@code null} when the server omitted
     * the corresponding field.
     *
     * @param promotionId           the server-issued banner identifier,
     *                              or {@code null}
     * @param creatives             the rendering creatives; {@code null}
     *                              treated as empty
     * @param validFromEpochSecond  the validity start epoch second, or
     *                              {@code null}
     * @param validUntilEpochSecond the validity end epoch second, or
     *                              {@code null}
     * @param loggingBlob           the opaque encrypted logging blob, or
     *                              {@code null}
     */
    QuickPromotion(String promotionId, List<QuickPromotionCreative> creatives, Long validFromEpochSecond,
                   Long validUntilEpochSecond, String loggingBlob) {
        this.promotionId = promotionId;
        this.creatives = creatives == null ? List.of() : creatives;
        this.validFromEpochSecond = validFromEpochSecond;
        this.validUntilEpochSecond = validUntilEpochSecond;
        this.loggingBlob = loggingBlob;
    }

    /**
     * Returns the server-issued banner identifier.
     *
     * @return an {@code Optional} carrying the identifier, or empty when
     *         the server omitted it
     */
    public Optional<String> promotionId() {
        return Optional.ofNullable(promotionId);
    }

    /**
     * Returns the rendering creatives shipped for this banner.
     *
     * @return an unmodifiable view of the creatives; never {@code null},
     *         possibly empty
     */
    public List<QuickPromotionCreative> creatives() {
        return Collections.unmodifiableList(creatives);
    }

    /**
     * Returns the instant at which the banner becomes eligible to show.
     *
     * @return an {@code Optional} carrying the start instant, or empty
     *         when the server omitted it
     */
    public Optional<Instant> validFrom() {
        return Optional.ofNullable(validFromEpochSecond).map(Instant::ofEpochSecond);
    }

    /**
     * Returns the instant at which the banner stops being eligible to
     * show.
     *
     * @return an {@code Optional} carrying the end instant, or empty
     *         when the server omitted it
     */
    public Optional<Instant> validUntil() {
        return Optional.ofNullable(validUntilEpochSecond).map(Instant::ofEpochSecond);
    }

    /**
     * Returns the opaque encrypted logging blob the client echoes back
     * when it logs an interaction with the banner.
     *
     * @return an {@code Optional} carrying the blob, or empty when the
     *         server omitted it
     */
    public Optional<String> loggingBlob() {
        return Optional.ofNullable(loggingBlob);
    }
}
