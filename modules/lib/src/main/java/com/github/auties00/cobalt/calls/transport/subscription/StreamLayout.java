package com.github.auties00.cobalt.calls.transport.subscription;

import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptor;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptors;

import java.util.Objects;

/**
 * The set of SSRCs and feature streams a client publishes for one call.
 *
 * <p>A client allocates its full media layout up front and declares it to the selective
 * forwarding unit as a {@link StreamDescriptors} list, one descriptor per active logical
 * stream. This record is the typed input the {@link LiveSubscriptionPublisher} turns into
 * those descriptors: it names the audio SSRC, the two video simulcast layer SSRCs (each
 * carrying its own media plus the paired forward error correction and negative
 * acknowledgement streams), the application data SSRC, the optional live transcription
 * SSRC, and the optional hop by hop forward error correction transmit and receive SSRCs.
 * An SSRC that is absent is encoded as {@link #ABSENT_SSRC} and its descriptors are
 * omitted, so an audio only call declares a single audio descriptor while a video call
 * declares the audio, simulcast, and feature descriptors.
 *
 * <p>A full layout expands to a media plus FEC plus NACK triple per audio and per video
 * simulcast layer, an application data descriptor carrying the uplink prefetch flag, a
 * live transcription descriptor, and the hop by hop FEC transmit and receive descriptors,
 * for the ceiling of {@link #MAX_STREAM_DESCRIPTORS} entries.
 *
 * @param audioSsrc           the audio media SSRC, or {@link #ABSENT_SSRC} when no audio is sent
 * @param videoStream0Ssrc    the lower resolution video simulcast layer SSRC, or {@link #ABSENT_SSRC}
 * @param videoStream1Ssrc    the higher resolution video simulcast layer SSRC, or {@link #ABSENT_SSRC}
 * @param appDataSsrc         the application data SSRC, or {@link #ABSENT_SSRC} when none is allocated
 * @param liveTranscriptionSsrc the live transcription SSRC, or {@link #ABSENT_SSRC} when disabled
 * @param hbhFecTxSsrc        the client to server hop by hop FEC SSRC, or {@link #ABSENT_SSRC}
 * @param hbhFecRxSsrc        the server to client hop by hop FEC SSRC, or {@link #ABSENT_SSRC}
 * @param uplinkPrefetch      whether uplink prefetch is engaged on the published video layers
 */
public record StreamLayout(int audioSsrc,
                           int videoStream0Ssrc,
                           int videoStream1Ssrc,
                           int appDataSsrc,
                           int liveTranscriptionSsrc,
                           int hbhFecTxSsrc,
                           int hbhFecRxSsrc,
                           boolean uplinkPrefetch) {
    /**
     * The sentinel SSRC standing in for a stream this layout does not publish.
     *
     * <p>An SSRC equal to this value is treated as unallocated: its
     * {@link StreamDescriptor} entries are omitted, since a descriptor is appended only
     * for a nonzero SSRC slot.
     */
    public static final int ABSENT_SSRC = 0;

    /**
     * The largest number of descriptors a single layout can yield.
     *
     * <p>Bounds the descriptor count to 21: the media plus FEC plus NACK triples for audio
     * and the two video simulcast layers, plus the application data, live transcription,
     * and two hop by hop FEC descriptors. Used to size the descriptor accumulator.
     */
    public static final int MAX_STREAM_DESCRIPTORS = 21;

    /**
     * Returns whether this layout publishes any video simulcast layer.
     *
     * <p>True when either {@link #videoStream0Ssrc()} or {@link #videoStream1Ssrc()} is
     * allocated, which the publisher uses to decide whether to emit the video media, FEC,
     * and NACK descriptors at all.
     *
     * @return {@code true} if at least one video simulcast SSRC is present
     */
    public boolean hasVideo() {
        return videoStream0Ssrc != ABSENT_SSRC || videoStream1Ssrc != ABSENT_SSRC;
    }

    /**
     * Builds an audio only layout that publishes a single audio media stream.
     *
     * <p>Convenience for the common one to one audio call where the only published stream
     * is the local audio carried on one SSRC with no video, application data, or feature
     * streams. All other SSRCs are {@link #ABSENT_SSRC} and uplink prefetch is disabled.
     *
     * @param audioSsrc the local audio SSRC this client sends
     * @return an audio only layout
     */
    public static StreamLayout audioOnly(int audioSsrc) {
        return new StreamLayout(audioSsrc, ABSENT_SSRC, ABSENT_SSRC, ABSENT_SSRC,
                ABSENT_SSRC, ABSENT_SSRC, ABSENT_SSRC, false);
    }

    /**
     * Returns a hash code derived from every SSRC and the uplink prefetch flag.
     *
     * <p>Combines all eight record components so two layouts hash equally exactly when
     * they publish the same streams, consistent with {@code equals}.
     *
     * @return the combined hash code of this layout
     */
    @Override
    public int hashCode() {
        return Objects.hash(audioSsrc, videoStream0Ssrc, videoStream1Ssrc, appDataSsrc,
                liveTranscriptionSsrc, hbhFecTxSsrc, hbhFecRxSsrc, uplinkPrefetch);
    }
}
