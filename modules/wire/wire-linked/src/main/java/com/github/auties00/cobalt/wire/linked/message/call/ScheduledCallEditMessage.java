package com.github.auties00.cobalt.wire.linked.message.call;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that edits a previously announced scheduled call.
 *
 * <p>After a {@link ScheduledCallCreationMessage} has been posted, the creator may need to revise
 * the event. This message carries the reference to the original announcement plus the kind of
 * edit being applied; today the only supported edit is a cancellation, but the enum is kept
 * extensible to accommodate future lifecycle actions.
 */
@ProtobufMessage(name = "Message.ScheduledCallEditMessage")
public final class ScheduledCallEditMessage implements Message {
    /**
     * {@link MessageKey} of the original {@link ScheduledCallCreationMessage} being edited.
     *
     * <p>Used by clients to locate the announcement in the chat history and update its rendering
     * accordingly.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * Kind of edit being applied to the scheduled call.
     *
     * <p>See {@link EditType} for the supported lifecycle actions.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    EditType editType;

    /**
     * Constructs a new scheduled call edit message.
     *
     * <p>This constructor is package-private: use the generated
     * {@code ScheduledCallEditMessageBuilder} to create instances.
     *
     * @param key      the {@link MessageKey} of the original scheduled call announcement, may be {@code null}
     * @param editType the kind of edit being applied, may be {@code null}
     */
    ScheduledCallEditMessage(MessageKey key, EditType editType) {
        this.key = key;
        this.editType = editType;
    }

    /**
     * Returns the {@link MessageKey} of the original scheduled call announcement being edited.
     *
     * @return an {@link Optional} containing the {@link MessageKey}, or empty if not set
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the kind of edit being applied to the scheduled call.
     *
     * @return an {@link Optional} containing the {@link EditType}, or empty if not set
     */
    public Optional<EditType> editType() {
        return Optional.ofNullable(editType);
    }

    /**
     * Sets the {@link MessageKey} of the original scheduled call announcement.
     *
     * @param key the {@link MessageKey}, or {@code null} to clear the field
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the kind of edit being applied to the scheduled call.
     *
     * @param editType the {@link EditType}, or {@code null} to clear the field
     */
    public void setEditType(EditType editType) {
        this.editType = editType;
    }

    /**
     * Describes the kind of lifecycle edit applied to a previously announced scheduled call.
     *
     * <p>The only lifecycle action currently supported by WhatsApp is the cancellation of the
     * scheduled call; the {@link #UNKNOWN} value is reserved for values that are not recognised
     * by this client.
     */
    @ProtobufEnum(name = "Message.ScheduledCallEditMessage.EditType")
    public static enum EditType {
        /**
         * The edit type is not specified or is not recognised by this client.
         */
        UNKNOWN(0),
        /**
         * The scheduled call is being cancelled.
         */
        CANCEL(1);

        /**
         * Creates an edit type constant with the given protobuf index.
         *
         * @param index the wire value assigned to this constant by the protobuf schema
         */
        EditType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire value assigned to this constant by the protobuf schema.
         */
        final int index;

        /**
         * Returns the wire value of this constant.
         *
         * @return the protobuf index of this enum constant
         */
        public int index() {
            return this.index;
        }
    }
}
