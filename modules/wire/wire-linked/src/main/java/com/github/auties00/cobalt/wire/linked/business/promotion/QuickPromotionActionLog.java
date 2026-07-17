package com.github.auties00.cobalt.wire.linked.business.promotion;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model recording a single interaction with a WhatsApp quick-promotion
 * banner so the in-app pacing engine can decide whether to keep showing the
 * same banner.
 *
 * <p>A quick promotion is an in-app banner WhatsApp shows on a specific
 * surface (chat list header, settings entry, business catalog screen, and
 * the like). Whenever the user sees, dismisses, or taps the primary action
 * of a banner the client reports the interaction so the server can refine
 * the pacing model. The same input shape backs both the consumer and the
 * business quick-promotion log endpoints.
 *
 * <p>{@link #event()} is the interaction kind (for example {@code VIEW}
 * for an impression or {@code ACTION} for a tap on a control); the full
 * server-side value set is not declared in the WhatsApp client and is
 * therefore carried as an opaque string. {@link #action()} discriminates
 * the action for an {@code ACTION} event (for example {@code DISMISS} for
 * the close affordance or {@code PRIMARY} for the primary call to
 * action); it is likewise an opaque string. {@link #promotionId()} names
 * the banner the interaction applies to and {@link #surfaceId()} names the
 * surface the banner was shown on. {@link #loggingBlob()} is an opaque
 * server-issued blob the client echoes back so the server can correlate
 * the interaction with the original eligibility decision.
 * {@link #clientTime()} records when the client logged the interaction
 * with second precision.
 */
@ProtobufMessage(name = "QuickPromotionActionLog")
public final class QuickPromotionActionLog {
    /**
     * Interaction kind (for example {@code VIEW} or {@code ACTION}). Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String event;

    /**
     * Action discriminator for an {@code ACTION} event (for example
     * {@code DISMISS} or {@code PRIMARY}). Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String action;

    /**
     * Identifier of the quick-promotion banner the interaction applies to.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String promotionId;

    /**
     * Identifier of the surface the banner was shown on. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String surfaceId;

    /**
     * Opaque server-issued blob the client echoes back so the server can
     * correlate the interaction with the original eligibility decision.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String loggingBlob;

    /**
     * Instant the client logged the interaction, carried with second
     * precision. Unset omits the variable.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant clientTime;

    /**
     * Constructs a new {@code QuickPromotionActionLog}. Every argument may
     * be {@code null} to omit the corresponding variable from the request.
     *
     * @param event       the interaction kind, or {@code null}
     * @param action      the action discriminator, or {@code null}
     * @param promotionId the banner identifier, or {@code null}
     * @param surfaceId   the surface identifier, or {@code null}
     * @param loggingBlob the opaque echoed blob, or {@code null}
     * @param clientTime  the client-side log instant, or {@code null}
     */
    public QuickPromotionActionLog(String event, String action, String promotionId, String surfaceId,
                                   String loggingBlob, Instant clientTime) {
        this.event = event;
        this.action = action;
        this.promotionId = promotionId;
        this.surfaceId = surfaceId;
        this.loggingBlob = loggingBlob;
        this.clientTime = clientTime;
    }

    /**
     * Returns the interaction kind.
     *
     * @return an {@link Optional} carrying the kind marker, or empty when
     *         unset
     */
    public Optional<String> event() {
        return Optional.ofNullable(event);
    }

    /**
     * Returns the action discriminator.
     *
     * @return an {@link Optional} carrying the action marker, or empty
     *         when unset
     */
    public Optional<String> action() {
        return Optional.ofNullable(action);
    }

    /**
     * Returns the banner identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> promotionId() {
        return Optional.ofNullable(promotionId);
    }

    /**
     * Returns the surface identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> surfaceId() {
        return Optional.ofNullable(surfaceId);
    }

    /**
     * Returns the opaque echoed blob.
     *
     * @return an {@link Optional} carrying the blob, or empty when unset
     */
    public Optional<String> loggingBlob() {
        return Optional.ofNullable(loggingBlob);
    }

    /**
     * Returns the instant the client logged the interaction.
     *
     * @return an {@link Optional} carrying the instant with second
     *         precision, or empty when unset
     */
    public Optional<Instant> clientTime() {
        return Optional.ofNullable(clientTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (QuickPromotionActionLog) obj;
        return Objects.equals(event, that.event)
                && Objects.equals(action, that.action)
                && Objects.equals(promotionId, that.promotionId)
                && Objects.equals(surfaceId, that.surfaceId)
                && Objects.equals(loggingBlob, that.loggingBlob)
                && Objects.equals(clientTime, that.clientTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, action, promotionId, surfaceId, loggingBlob, clientTime);
    }

    @Override
    public String toString() {
        return "QuickPromotionActionLog[" +
                "event=" + event + ", " +
                "action=" + action + ", " +
                "promotionId=" + promotionId + ", " +
                "surfaceId=" + surfaceId + ", " +
                "loggingBlob=" + loggingBlob + ", " +
                "clientTime=" + clientTime + ']';
    }
}
