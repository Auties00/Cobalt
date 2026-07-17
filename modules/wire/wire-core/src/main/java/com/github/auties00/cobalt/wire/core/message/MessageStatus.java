package com.github.auties00.cobalt.wire.core.message;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Lifecycle stages a sent message passes through, as mirrored by the
 * checkmark indicator in the WhatsApp UI.
 *
 * <p>When the logged-in user sends a message, its status progresses
 * through a well-defined sequence as acknowledgments flow back from
 * the server and the recipient devices:
 * <pre>
 *     PENDING -&gt; SERVER_ACK -&gt; DELIVERED -&gt; READ -&gt; PLAYED
 * </pre>
 *
 * <p>The {@link #PLAYED} state only applies to voice notes and
 * view-once media; for text and other content the sequence ends at
 * {@link #READ}. The {@link #ERROR} state is reserved for messages
 * that could not be sent at all.
 */
@ProtobufEnum(name = "WebMessageInfo.Status")
public enum MessageStatus {
    /**
     * The message could not be delivered because of a local or remote
     * error.
     *
     * <p>No checkmark is shown in the UI and the message typically
     * appears with a failure indicator.
     */
    ERROR(0),
    /**
     * The message has been enqueued locally but has not yet been
     * acknowledged by the server.
     *
     * <p>Rendered as a clock icon in the UI.
     */
    PENDING(1),
    /**
     * The server has received the message but it has not yet been
     * relayed to the recipient.
     *
     * <p>Rendered as a single grey checkmark in the UI.
     */
    SERVER_ACK(2),
    /**
     * At least one of the recipient's devices has confirmed delivery.
     *
     * <p>Rendered as two grey checkmarks in the UI.
     */
    DELIVERED(3),
    /**
     * The recipient has opened the chat and the message is now
     * considered read.
     *
     * <p>Rendered as two blue checkmarks in the UI.
     */
    READ(4),
    /**
     * The recipient has played back the voice note or view-once media
     * attached to the message.
     *
     * <p>Rendered as two blue checkmarks in the UI, with the audio
     * waveform switching to blue for voice notes.
     */
    PLAYED(5);

    /**
     * The protobuf wire index identifying this constant.
     */
    final int index;

    /**
     * Constructs a new enum constant with the given protobuf wire
     * index.
     *
     * @param index the protobuf wire index associated with this
     *              constant
     */
    MessageStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }
}
