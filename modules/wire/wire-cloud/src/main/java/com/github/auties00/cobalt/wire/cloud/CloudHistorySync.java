package com.github.auties00.cobalt.wire.cloud;

import com.github.auties00.cobalt.wire.core.message.MessageInfo;

import java.util.List;
import java.util.OptionalInt;

/**
 * One chunk of a phone-number history synchronization, decoded from a {@code history} webhook
 * change.
 *
 * <p>When a phone number with existing conversation history onboards to the Cloud API, the
 * platform streams the history over the webhook in ordered chunks; each chunk reports its sync
 * phase, its position in the stream, the overall progress, and the messages it carries, decoded
 * into the universal message model.
 */
public final class CloudHistorySync {
    /**
     * The synchronization phase, or {@code -1} when not reported.
     */
    private final int phase;

    /**
     * The position of this chunk in the stream, or {@code -1} when not reported.
     */
    private final int chunkOrder;

    /**
     * The overall progress percentage, or {@code -1} when not reported.
     */
    private final int progress;

    /**
     * The messages carried by this chunk.
     */
    private final List<MessageInfo> messages;

    /**
     * Constructs a new history sync chunk.
     *
     * @param phase      the synchronization phase, or {@code -1} when not reported
     * @param chunkOrder the chunk position, or {@code -1} when not reported
     * @param progress   the progress percentage, or {@code -1} when not reported
     * @param messages   the messages carried by the chunk, or {@code null} for none
     */
    public CloudHistorySync(int phase, int chunkOrder, int progress, List<? extends MessageInfo> messages) {
        this.phase = phase;
        this.chunkOrder = chunkOrder;
        this.progress = progress;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
    }

    /**
     * Returns the synchronization phase.
     *
     * @return an {@link OptionalInt} carrying the phase, or empty when not reported
     */
    public OptionalInt phase() {
        return phase < 0 ? OptionalInt.empty() : OptionalInt.of(phase);
    }

    /**
     * Returns the position of this chunk in the stream.
     *
     * @return an {@link OptionalInt} carrying the chunk order, or empty when not reported
     */
    public OptionalInt chunkOrder() {
        return chunkOrder < 0 ? OptionalInt.empty() : OptionalInt.of(chunkOrder);
    }

    /**
     * Returns the overall progress percentage.
     *
     * @return an {@link OptionalInt} carrying the progress, or empty when not reported
     */
    public OptionalInt progress() {
        return progress < 0 ? OptionalInt.empty() : OptionalInt.of(progress);
    }

    /**
     * Returns the messages carried by this chunk.
     *
     * @return an unmodifiable list of messages, empty when the chunk carried none
     */
    public List<MessageInfo> messages() {
        return messages;
    }
}
