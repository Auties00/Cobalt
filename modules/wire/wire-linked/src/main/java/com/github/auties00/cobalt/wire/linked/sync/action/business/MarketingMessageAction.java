package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A sync action describing a saved marketing (premium) message template maintained by a business
 * account.
 *
 * <p>Marketing message templates are reusable pieces of outbound content that the business can
 * personalise and send to many recipients through broadcast flows. This action carries the
 * template's display name, textual body, prototype kind (for example
 * {@link MarketingMessagePrototypeType#PERSONALIZED}), creation and last-send timestamps, an
 * optional attached media identifier, and a deletion flag. Every linked device consumes this
 * action so that the business template library stays consistent across surfaces.
 *
 * <p>This action is transported in the {@code REGULAR} sync collection and keyed by the
 * marketing message identifier through {@link MarketingMessageActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.MarketingMessageAction")
public final class MarketingMessageAction implements SyncAction<MarketingMessageActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "marketingMessage";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * The sync patch collection that carries this action between devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version for this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The operator-chosen display name of the marketing message template.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;

    /**
     * The textual body of the marketing message template.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String message;

    /**
     * The prototype kind of the template, describing how the body will be personalised at send time.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    MarketingMessagePrototypeType type;

    /**
     * The timestamp, in milliseconds, at which the template was originally created.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    Long createdAt;

    /**
     * The timestamp, in milliseconds, at which the template was most recently sent from.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    Long lastSentAt;

    /**
     * Whether the template has been deleted from the library.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean isDeleted;

    /**
     * The identifier of the media attachment associated with this template, if any.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String mediaId;


    /**
     * Creates a marketing message action with all template fields populated.
     *
     * @param name       the template display name, or {@code null}
     * @param message    the template body, or {@code null}
     * @param type       the template prototype kind, or {@code null}
     * @param createdAt  the creation timestamp in milliseconds, or {@code null}
     * @param lastSentAt the last-sent timestamp in milliseconds, or {@code null}
     * @param isDeleted  {@code true} if the template has been deleted, {@code false} or {@code null} otherwise
     * @param mediaId    the identifier of the attached media, or {@code null}
     */
    MarketingMessageAction(String name, String message, MarketingMessagePrototypeType type, Long createdAt, Long lastSentAt, Boolean isDeleted, String mediaId) {
        this.name = name;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.lastSentAt = lastSentAt;
        this.isDeleted = isDeleted;
        this.mediaId = mediaId;
    }

    /**
     * Returns the display name of this marketing message template.
     *
     * @return the name, or {@link Optional#empty()} if not set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the textual body of this marketing message template.
     *
     * @return the body, or {@link Optional#empty()} if not set
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns the prototype kind of this marketing message template.
     *
     * @return the prototype kind, or {@link Optional#empty()} if not set
     */
    public Optional<MarketingMessagePrototypeType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the creation timestamp of this template, in milliseconds.
     *
     * @return the creation timestamp, or {@link OptionalLong#empty()} if not set
     */
    public OptionalLong createdAt() {
        return createdAt == null ? OptionalLong.empty() : OptionalLong.of(createdAt);
    }

    /**
     * Returns the timestamp of the last send performed from this template, in milliseconds.
     *
     * @return the last-sent timestamp, or {@link OptionalLong#empty()} if the template has never been sent
     */
    public OptionalLong lastSentAt() {
        return lastSentAt == null ? OptionalLong.empty() : OptionalLong.of(lastSentAt);
    }

    /**
     * Returns whether this template has been deleted from the business template library.
     *
     * @return {@code true} if the template is deleted, {@code false} otherwise
     */
    public boolean isDeleted() {
        return isDeleted != null && isDeleted;
    }

    /**
     * Returns the identifier of the media attachment associated with this template.
     *
     * @return the media identifier, or {@link Optional#empty()} if no media is attached
     */
    public Optional<String> mediaId() {
        return Optional.ofNullable(mediaId);
    }

    /**
     * Updates the display name of this template.
     *
     * @param name the new name, or {@code null} to clear it
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Updates the textual body of this template.
     *
     * @param message the new body, or {@code null} to clear it
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Updates the prototype kind of this template.
     *
     * @param type the new prototype kind, or {@code null} to clear it
     */
    public void setType(MarketingMessagePrototypeType type) {
        this.type = type;
    }

    /**
     * Updates the creation timestamp of this template.
     *
     * @param createdAt the new creation timestamp in milliseconds, or {@code null} to clear it
     */
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Updates the last-sent timestamp of this template.
     *
     * @param lastSentAt the new last-sent timestamp in milliseconds, or {@code null} to clear it
     */
    public void setLastSentAt(Long lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    /**
     * Updates the deletion flag of this template.
     *
     * @param isDeleted {@code true} to mark the template as deleted, {@code false} or {@code null} otherwise
     */
    public void setDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    /**
     * Updates the identifier of the media attachment associated with this template.
     *
     * @param mediaId the new media identifier, or {@code null} to clear it
     */
    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    /**
     * The prototype kind of a {@link MarketingMessageAction}, describing how the template body
     * is personalised when the template is sent.
     */
    @ProtobufEnum(name = "SyncActionValue.MarketingMessageAction.MarketingMessagePrototypeType")
    public static enum MarketingMessagePrototypeType {
        /**
         * Indicates a template whose body is personalised per-recipient at send time.
         */
        PERSONALIZED(0);

        /**
         * Creates a prototype kind constant with the supplied protobuf wire index.
         *
         * @param index the protobuf wire index of this constant
         */
        MarketingMessagePrototypeType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index of this prototype kind constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this prototype kind constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }


}
