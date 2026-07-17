package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * Generic metadata attached to URLs embedded in a message.
 *
 * <p>Alongside the preview card carried by {@link MessageLinkPreviewMetadata},
 * this smaller record carries server-side tagging information (currently an A/B
 * experiment identifier) that clients should forward back when reporting
 * interactions with the URL.
 */
@ProtobufMessage(name = "Message.URLMetadata")
public final class MessageURLMetadata implements Message {
    /**
     * Experiment identifier used for server-side A/B testing of URL handling.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer fbExperimentId;


    /**
     * Constructs a new URL metadata record.
     *
     * @param fbExperimentId the experiment identifier, or {@code null}
     */
    MessageURLMetadata(Integer fbExperimentId) {
        this.fbExperimentId = fbExperimentId;
    }

    /**
     * Returns the Facebook experiment identifier for this URL.
     *
     * @return the identifier, or empty if unset
     */
    public OptionalInt fbExperimentId() {
        return fbExperimentId == null ? OptionalInt.empty() : OptionalInt.of(fbExperimentId);
    }

    /**
     * Updates the experiment identifier.
     *
     * @param fbExperimentId the new identifier, or {@code null} to clear
     */
    public void setFbExperimentId(Integer fbExperimentId) {
        this.fbExperimentId = fbExperimentId;
    }
}
