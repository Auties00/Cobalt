package com.github.auties00.cobalt.wire.linked.message.poll;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Represents the decrypted content of a vote cast against a poll.
 *
 * <p>This message appears as the plaintext inside a {@link PollEncValue}
 * payload once the receiver decrypts an incoming {@link PollUpdateMessage}.
 * It lists the hashes of the options the voter selected, matching the hashes
 * declared in the originating {@link PollCreationMessage.Option} entries.
 */
@ProtobufMessage(name = "Message.PollVoteMessage")
public final class PollVoteMessage implements Message {
    /**
     * The hashes of the options selected by the voter.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    List<byte[]> selectedOptions;


    /**
     * Creates a new poll vote message with the provided selected option hashes.
     *
     * @param selectedOptions the hashes of the selected options
     */
    PollVoteMessage(List<byte[]> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }

    /**
     * Returns the hashes of the options selected by the voter.
     *
     * @return an unmodifiable list of option hashes, or an empty list when none are set
     */
    public List<byte[]> selectedOptions() {
        return selectedOptions == null ? List.of() : Collections.unmodifiableList(selectedOptions);
    }

    /**
     * Sets the hashes of the options selected by the voter.
     *
     * @param selectedOptions the hashes of the selected options
     */
    public void setSelectedOptions(List<byte[]> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }
}
