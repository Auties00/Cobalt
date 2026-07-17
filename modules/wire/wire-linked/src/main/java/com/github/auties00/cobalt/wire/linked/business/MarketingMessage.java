package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.business.MarketingMessageAction.MarketingMessagePrototypeType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A model representing a single WhatsApp Business marketing-message
 * template.
 *
 * <p>Marketing messages are pre-approved promotional templates that a
 * business sends to opted-in customers via campaigns. Each template has a
 * stable {@linkplain #templateId() identifier} chosen at creation time, an
 * operator-chosen {@linkplain #name() display name}, the
 * {@linkplain #message() text body}, the
 * {@linkplain #type() prototype kind} that controls how the body is
 * personalised at send time, the {@linkplain #createdAt() creation} and
 * {@linkplain #lastSentAt() last-sent} timestamps, an optional
 * {@linkplain #mediaId() attached media identifier}, and a
 * {@linkplain #deleted() deletion flag} that tombstones removed
 * templates without erasing them outright.
 *
 * <p>Cobalt persists each template independently so callers can resolve a
 * single template by id without iterating the entire library.
 *
 * <p>This class is a local model only. Modifying its fields does not send
 * any request to the WhatsApp servers; it simply reflects the locally
 * cached state.
 */
@ProtobufMessage
public final class MarketingMessage {
    /**
     * The non-{@code null} stable identifier of the marketing-message
     * template. Used as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String templateId;

    /**
     * The operator-chosen display name of the template, or {@code null}
     * when no name has been set.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * The textual body of the template, or {@code null} when no body has
     * been set.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String message;

    /**
     * The prototype kind of the template (for example
     * {@link MarketingMessagePrototypeType#PERSONALIZED}), describing how
     * the body is personalised at send time, or {@code null} when no
     * prototype has been chosen.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    MarketingMessagePrototypeType type;

    /**
     * The instant at which the template was originally created, or
     * {@code null} when no creation time is known.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant createdAt;

    /**
     * The instant at which the template was most recently sent from, or
     * {@code null} when the template has never been sent.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant lastSentAt;

    /**
     * The tombstone flag set to {@code true} when the template has been
     * deleted from the library.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean deleted;

    /**
     * The identifier of the media attachment associated with this
     * template, or {@code null} when no media is attached.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String mediaId;

    /**
     * Constructs a new marketing-message template with the given fields.
     *
     * @param templateId the non-{@code null} template identifier
     * @param name       the display name, or {@code null}
     * @param message    the textual body, or {@code null}
     * @param type       the prototype kind, or {@code null}
     * @param createdAt  the creation instant, or {@code null}
     * @param lastSentAt the last-sent instant, or {@code null}
     * @param deleted    whether the template is tombstoned
     * @param mediaId    the attached media identifier, or {@code null}
     */
    MarketingMessage(String templateId, String name, String message, MarketingMessagePrototypeType type,
                     Instant createdAt, Instant lastSentAt, boolean deleted, String mediaId) {
        this.templateId = Objects.requireNonNull(templateId, "templateId cannot be null");
        this.name = name;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.lastSentAt = lastSentAt;
        this.deleted = deleted;
        this.mediaId = mediaId;
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
     * Returns the operator-chosen display name of the template.
     *
     * @return an {@code Optional} containing the name, or empty if not
     *         set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Updates the display name of the template.
     *
     * @param name the new name, or {@code null} to clear
     * @return this template instance for method chaining
     */
    public MarketingMessage setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the textual body of the template.
     *
     * @return an {@code Optional} containing the body, or empty if not
     *         set
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    /**
     * Updates the textual body of the template.
     *
     * @param message the new body, or {@code null} to clear
     * @return this template instance for method chaining
     */
    public MarketingMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Returns the prototype kind of the template.
     *
     * @return an {@code Optional} containing the prototype kind, or empty
     *         if not set
     */
    public Optional<MarketingMessagePrototypeType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Updates the prototype kind of the template.
     *
     * @param type the new prototype kind, or {@code null} to clear
     * @return this template instance for method chaining
     */
    public MarketingMessage setType(MarketingMessagePrototypeType type) {
        this.type = type;
        return this;
    }

    /**
     * Returns the creation instant of the template.
     *
     * @return an {@code Optional} containing the creation instant, or
     *         empty if not set
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Updates the creation instant of the template.
     *
     * @param createdAt the new creation instant, or {@code null} to
     *                  clear
     * @return this template instance for method chaining
     */
    public MarketingMessage setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Returns the instant at which the template was most recently sent
     * from.
     *
     * @return an {@code Optional} containing the last-sent instant, or
     *         empty when the template has never been sent
     */
    public Optional<Instant> lastSentAt() {
        return Optional.ofNullable(lastSentAt);
    }

    /**
     * Updates the last-sent instant of the template.
     *
     * @param lastSentAt the new last-sent instant, or {@code null} to
     *                   clear
     * @return this template instance for method chaining
     */
    public MarketingMessage setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
        return this;
    }

    /**
     * Returns whether the template is tombstoned.
     *
     * @return {@code true} if the template has been deleted
     */
    public boolean deleted() {
        return deleted;
    }

    /**
     * Updates the tombstone flag of this template.
     *
     * @param deleted the new flag
     * @return this template instance for method chaining
     */
    public MarketingMessage setDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    /**
     * Returns the identifier of the media attachment associated with
     * this template.
     *
     * @return an {@code Optional} containing the media identifier, or
     *         empty when no media is attached
     */
    public Optional<String> mediaId() {
        return Optional.ofNullable(mediaId);
    }

    /**
     * Updates the attached-media identifier.
     *
     * @param mediaId the new media identifier, or {@code null} to clear
     * @return this template instance for method chaining
     */
    public MarketingMessage setMediaId(String mediaId) {
        this.mediaId = mediaId;
        return this;
    }

    /**
     * Returns a hash code derived from this template's
     * {@linkplain #templateId() identifier}.
     *
     * @return the hash code of the template identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(templateId);
    }

    /**
     * Returns whether this template is equal to the given object.
     *
     * <p>Two templates are considered equal when they share the same
     * {@linkplain #templateId() identifier}, regardless of the other
     * fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code MarketingMessage}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof MarketingMessage that && Objects.equals(this.templateId, that.templateId);
    }
}
