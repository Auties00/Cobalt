package com.github.auties00.cobalt.wire.linked.business.promotion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Eligible WhatsApp quick-promotion banners returned for one client
 * surface.
 *
 * <p>WhatsApp shows quick-promotion banners (small in-app cards
 * advertising a feature or an action the user might want to take) on
 * named surfaces inside the app. When the client asks the server which
 * banners may show, the server replies with one batch per surface: the
 * surface identifier, and the ordered list of {@link QuickPromotion
 * banners} the pacing engine selected as eligible.
 *
 * <p>This model is one such per-surface batch.
 */
@ProtobufMessage(name = "QuickPromotionSurfaceBatch")
public final class QuickPromotionSurfaceBatch {
    /**
     * Client surface identifier this batch was evaluated against, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String surfaceId;

    /**
     * Eligible banners selected for the surface, in the order the server
     * returned them. Never {@code null}, possibly empty when the pacing
     * engine selected none.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<QuickPromotion> banners;

    /**
     * Constructs a new {@code QuickPromotionSurfaceBatch}. A
     * {@code null} {@code banners} is coerced to an empty list.
     *
     * @param surfaceId the client surface identifier, or {@code null}
     *                  when the server omitted it
     * @param banners   the eligible banners; {@code null} treated as
     *                  empty
     */
    QuickPromotionSurfaceBatch(String surfaceId, List<QuickPromotion> banners) {
        this.surfaceId = surfaceId;
        this.banners = banners == null ? List.of() : banners;
    }

    /**
     * Returns the client surface identifier this batch was evaluated
     * against.
     *
     * @return an {@code Optional} carrying the surface identifier, or
     *         empty when the server omitted it
     */
    public Optional<String> surfaceId() {
        return Optional.ofNullable(surfaceId);
    }

    /**
     * Returns the eligible banners selected for the surface.
     *
     * @return an unmodifiable view of the banners; never {@code null},
     *         possibly empty
     */
    public List<QuickPromotion> banners() {
        return Collections.unmodifiableList(banners);
    }
}
