package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents a label (tag) assigned to a participant within a WhatsApp group.
 *
 * <p>Group administrators can tag individual participants with custom labels
 * for organizational purposes. Each label has a text value and a timestamp
 * recording when the label was assigned. This feature is available only when
 * participant labels are enabled for the group (see
 * {@link GroupMetadata#isParticipantLabelEnabled()}).
 *
 * @see GroupParticipant
 * @see GroupMetadata
 */
@ProtobufMessage(name = "MemberLabel")
public final class GroupParticipantLabel {
    /**
     * The text of the label assigned to the participant, or {@code null} if
     * not set.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String label;

    /**
     * The instant at which this label was assigned to the participant, or
     * {@code null} if the timestamp is not available.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant labelTimestamp;


    /**
     * Constructs a new {@code GroupParticipantLabel} with the specified text
     * and timestamp.
     *
     * @param label          the label text, or {@code null}
     * @param labelTimestamp the instant the label was assigned, or
     *                       {@code null}
     */
    GroupParticipantLabel(String label, Instant labelTimestamp) {
        this.label = label;
        this.labelTimestamp = labelTimestamp;
    }

    /**
     * Returns the text of the label, if set.
     *
     * @return an {@code Optional} containing the label text, or empty if not
     *         set
     */
    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns the instant at which this label was assigned, if available.
     *
     * @return an {@code Optional} containing the timestamp, or empty if not
     *         available
     */
    public Optional<Instant> labelTimestamp() {
        return Optional.ofNullable(labelTimestamp);
    }

    /**
     * Sets the text of the label.
     *
     * @param label the label text to set, or {@code null} to clear
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Sets the instant at which this label was assigned.
     *
     * @param labelTimestamp the timestamp to set, or {@code null} to clear
     */
    public void setLabelTimestamp(Instant labelTimestamp) {
        this.labelTimestamp = labelTimestamp;
    }
}
