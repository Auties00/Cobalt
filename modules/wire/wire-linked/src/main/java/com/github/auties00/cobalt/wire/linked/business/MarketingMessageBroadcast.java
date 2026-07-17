package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model representing the broadcast lifecycle status of a single WhatsApp
 * Business marketing-message template.
 *
 * <p>When a marketing-message template is sent to a broadcast list, the
 * resulting broadcast progresses through a server-tracked lifecycle (for
 * example {@code SCHEDULED}, {@code SENT}, {@code FAILED}). This record
 * pairs the originating
 * {@linkplain #templateId() template identifier} with the latest reported
 * {@linkplain #status() lifecycle status string}.
 *
 * <p>Cobalt persists each entry independently so callers can query the
 * broadcast status of a single template without iterating the whole map.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class MarketingMessageBroadcast {
    /**
     * The non-{@code null} stable identifier of the marketing-message
     * template whose broadcast lifecycle this entry tracks. Used as the
     * primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String templateId;

    /**
     * The latest server-reported broadcast lifecycle status string, or
     * {@code null} when the server has not yet reported a status.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String status;

    /**
     * Constructs a new marketing-message broadcast lifecycle record with the
     * given identifier and status.
     *
     * @param templateId the non-{@code null} template identifier
     * @param status     the lifecycle status string, or {@code null}
     */
    MarketingMessageBroadcast(String templateId, String status) {
        this.templateId = Objects.requireNonNull(templateId, "templateId cannot be null");
        this.status = status;
    }

    /**
     * Returns the non-{@code null} template identifier.
     *
     * @return the template identifier
     */
    public String templateId() {
        return templateId;
    }

    /**
     * Returns the latest server-reported broadcast lifecycle status of the
     * template.
     *
     * @return an {@code Optional} containing the status string, or empty if
     *         the server has not yet reported a status
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Updates the broadcast lifecycle status.
     *
     * @param status the new status string, or {@code null} to clear it
     * @return this broadcast instance for method chaining
     */
    public MarketingMessageBroadcast setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Returns a hash code derived from this entry's
     * {@linkplain #templateId() template identifier}.
     *
     * @return the hash code of the template identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(templateId);
    }

    /**
     * Returns whether this broadcast is equal to the given object.
     *
     * <p>Two broadcasts are considered equal when they share the same
     * {@linkplain #templateId() template identifier}, regardless of their
     * status string.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code MarketingMessageBroadcast}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof MarketingMessageBroadcast that && Objects.equals(this.templateId, that.templateId);
    }
}
