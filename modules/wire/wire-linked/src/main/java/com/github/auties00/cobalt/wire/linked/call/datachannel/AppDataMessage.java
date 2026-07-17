package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * One application-data message broadcast on the call's AppData stream.
 *
 * <p>An {@code AppDataMessage} is a oneof container: exactly one of its
 * payload fields ({@linkplain #reactionInfo() reaction info} or
 * {@linkplain #transcriptionInfo() transcription info}) is set per
 * instance. Messages flow over the AppData DataChannel
 * ({@link StreamDescriptor.StreamLayer#APP_DATA_STREAM0}) inside an
 * {@link AppDataPayloads} batch.
 */
@ProtobufMessage(name = "appDataMessage")
public final class AppDataMessage {
    /**
     * The reaction-info payload variant, if this message carries a reaction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ReactionInfo reactionInfo;

    /**
     * The live-transcription payload variant, if this message carries a
     * transcript fragment.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final LiveTranscriptionInfo transcriptionInfo;

    /**
     * Constructs a new {@code AppDataMessage}.
     *
     * <p>At most one of the two payload fields should be non-{@code null}
     * to keep the oneof wire semantics well defined.
     *
     * @param reactionInfo      the reaction payload, or {@code null}
     * @param transcriptionInfo the transcription payload, or {@code null}
     */
    AppDataMessage(ReactionInfo reactionInfo, LiveTranscriptionInfo transcriptionInfo) {
        this.reactionInfo = reactionInfo;
        this.transcriptionInfo = transcriptionInfo;
    }

    /**
     * Returns the reaction payload variant.
     *
     * @return an {@link Optional} with the reaction info, or empty
     */
    public Optional<ReactionInfo> reactionInfo() {
        return Optional.ofNullable(reactionInfo);
    }

    /**
     * Returns the live-transcription payload variant.
     *
     * @return an {@link Optional} with the transcription info, or empty
     */
    public Optional<LiveTranscriptionInfo> transcriptionInfo() {
        return Optional.ofNullable(transcriptionInfo);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof AppDataMessage that
                && Objects.equals(this.reactionInfo, that.reactionInfo)
                && Objects.equals(this.transcriptionInfo, that.transcriptionInfo));
    }

    @Override
    public int hashCode() {
        return Objects.hash(reactionInfo, transcriptionInfo);
    }

    @Override
    public String toString() {
        return "AppDataMessage[reactionInfo=" + reactionInfo
                + ", transcriptionInfo=" + transcriptionInfo + ']';
    }
}
