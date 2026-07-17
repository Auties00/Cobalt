package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * One unit of in-call live transcription published over the AppData stream.
 *
 * <p>WhatsApp's live-transcription feature emits {@code LiveTranscriptionInfo}
 * messages on the AppData DataChannel ({@link StreamDescriptor.StreamLayer#LIVE_TRANSCRIPTION_STREAM0}),
 * one per transcribed caption fragment. The runtime correlates updates by
 * {@linkplain #transcriptId() transcript id} and tags each fragment with the
 * ISO {@linkplain #language() language} code of the source speech.
 */
@ProtobufMessage(name = "liveTranscriptionInfo")
public final class LiveTranscriptionInfo {
    /**
     * The transcript fragment identifier used to correlate updates.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    final Long transcriptId;

    /**
     * The transcribed caption text.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String caption;

    /**
     * The ISO language code of the source speech.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String language;

    /**
     * Constructs a new {@code LiveTranscriptionInfo}.
     *
     * @param transcriptId the transcript fragment id
     * @param caption      the caption text
     * @param language     the source language code
     */
    LiveTranscriptionInfo(Long transcriptId, String caption, String language) {
        this.transcriptId = transcriptId;
        this.caption = caption;
        this.language = language;
    }

    /**
     * Returns the transcript fragment identifier.
     *
     * @return an {@link OptionalLong} with the id, or empty
     */
    public OptionalLong transcriptId() {
        return transcriptId == null ? OptionalLong.empty() : OptionalLong.of(transcriptId);
    }

    /**
     * Returns the transcribed caption text.
     *
     * @return an {@link Optional} with the caption, or empty
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the ISO language code of the source speech.
     *
     * @return an {@link Optional} with the language, or empty
     */
    public Optional<String> language() {
        return Optional.ofNullable(language);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof LiveTranscriptionInfo that
                && Objects.equals(this.transcriptId, that.transcriptId)
                && Objects.equals(this.caption, that.caption)
                && Objects.equals(this.language, that.language));
    }

    @Override
    public int hashCode() {
        return Objects.hash(transcriptId, caption, language);
    }

    @Override
    public String toString() {
        return "LiveTranscriptionInfo[transcriptId=" + transcriptId
                + ", caption=" + caption + ", language=" + language + ']';
    }
}
