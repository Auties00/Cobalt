package com.github.auties00.cobalt.calls.engine.participant;

import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Holds the per participant media state of a call participant.
 *
 * <p>Every participant carries a media object that tracks the participant's audio stream,
 * its two simulcast video streams, its screenshare stream, and the SSRC sets bound to
 * each, along with the subscribed stream ids the engine keeps for the participant's video
 * and screenshare. This class is the holder for that state; the actual encoder, decoder,
 * and jitter buffer objects live in the media plane (native codec and RTP layers) and are
 * not modeled here.
 *
 * <p>A participant has exactly {@value #SIMULCAST_STREAM_COUNT} simulcast video streams.
 * Each video stream and the screenshare stream carry an SSRC triple of a primary SSRC, a
 * FEC SSRC, and an out of band NACK SSRC; the audio stream carries a single SSRC. These
 * SSRCs are not generated here; they are computed by the secure SSRC generator from the
 * owning device's JID bound identifiers and installed through the per stream setters. Until
 * a stream's SSRC set is installed, its SSRC accessors report absent.
 *
 * <p>The subscribed video and screenshare encoded stream ids track which encoded stream
 * the engine has subscribed this participant to; they default to
 * {@value #NO_SUBSCRIBED_STREAM} (no subscription).
 *
 * <p>This class is not thread safe; the engine mutates a participant's media only while
 * holding the membership lock that guards the owning {@link CallParticipant}.
 */
public final class CallParticipantMedia {
    /**
     * Holds the number of simulcast video streams a participant carries.
     */
    public static final int SIMULCAST_STREAM_COUNT = 2;

    /**
     * Holds the sentinel an SSRC field carries until the secure SSRC generator fills it.
     */
    public static final int UNASSIGNED_SSRC = 0;

    /**
     * Holds the sentinel a subscribed encoded stream id carries when the participant is not
     * subscribed to any encoded stream.
     */
    public static final int NO_SUBSCRIBED_STREAM = -1;

    /**
     * Holds the audio stream's SSRC, or {@value #UNASSIGNED_SSRC} until generated.
     */
    private int audioSsrc;

    /**
     * Holds the primary SSRC of each simulcast video stream, indexed by stream id, each
     * {@value #UNASSIGNED_SSRC} until generated.
     */
    private final int[] videoPrimarySsrc;

    /**
     * Holds the FEC SSRC of each simulcast video stream, indexed by stream id, each
     * {@value #UNASSIGNED_SSRC} until generated.
     */
    private final int[] videoFecSsrc;

    /**
     * Holds the out of band NACK SSRC of each simulcast video stream, indexed by stream id,
     * each {@value #UNASSIGNED_SSRC} until generated.
     */
    private final int[] videoOobNackSsrc;

    /**
     * Holds the screenshare stream's primary SSRC, or {@value #UNASSIGNED_SSRC} until
     * generated.
     */
    private int screenSharePrimarySsrc;

    /**
     * Holds the screenshare stream's FEC SSRC, or {@value #UNASSIGNED_SSRC} until generated.
     */
    private int screenShareFecSsrc;

    /**
     * Holds the screenshare stream's out of band NACK SSRC, or {@value #UNASSIGNED_SSRC}
     * until generated.
     */
    private int screenShareOobNackSsrc;

    /**
     * Holds the subscribed encoded video stream id, or {@value #NO_SUBSCRIBED_STREAM} when
     * not subscribed.
     */
    private int subscribedVideoStreamId;

    /**
     * Holds the subscribed encoded screenshare stream id, or {@value #NO_SUBSCRIBED_STREAM}
     * when not subscribed.
     */
    private int subscribedScreenShareStreamId;

    /**
     * Constructs an empty media object with no SSRCs assigned and no stream subscriptions.
     */
    public CallParticipantMedia() {
        this.audioSsrc = UNASSIGNED_SSRC;
        this.videoPrimarySsrc = newUnassignedStreamArray();
        this.videoFecSsrc = newUnassignedStreamArray();
        this.videoOobNackSsrc = newUnassignedStreamArray();
        this.screenSharePrimarySsrc = UNASSIGNED_SSRC;
        this.screenShareFecSsrc = UNASSIGNED_SSRC;
        this.screenShareOobNackSsrc = UNASSIGNED_SSRC;
        this.subscribedVideoStreamId = NO_SUBSCRIBED_STREAM;
        this.subscribedScreenShareStreamId = NO_SUBSCRIBED_STREAM;
    }

    /**
     * Returns the audio stream's SSRC.
     *
     * @return the audio SSRC, or {@link OptionalInt#empty()} if it has not been generated
     */
    public OptionalInt audioSsrc() {
        return audioSsrc == UNASSIGNED_SSRC ? OptionalInt.empty() : OptionalInt.of(audioSsrc);
    }

    /**
     * Installs the audio stream's SSRC.
     *
     * @param audioSsrc the audio SSRC to record
     * @return this media object
     */
    public CallParticipantMedia audioSsrc(int audioSsrc) {
        this.audioSsrc = audioSsrc;
        return this;
    }

    /**
     * Returns the primary SSRC of the given simulcast video stream.
     *
     * @param streamId the simulcast stream id, in {@code 0..}{@value #SIMULCAST_STREAM_COUNT}
     *                 exclusive
     * @return the primary SSRC, or {@link OptionalInt#empty()} if it has not been generated
     * @throws IndexOutOfBoundsException if {@code streamId} is not a valid simulcast stream
     *                                   id
     */
    public OptionalInt videoPrimarySsrc(int streamId) {
        return ssrcAt(videoPrimarySsrc, streamId);
    }

    /**
     * Returns the FEC SSRC of the given simulcast video stream.
     *
     * @param streamId the simulcast stream id, in {@code 0..}{@value #SIMULCAST_STREAM_COUNT}
     *                 exclusive
     * @return the FEC SSRC, or {@link OptionalInt#empty()} if it has not been generated
     * @throws IndexOutOfBoundsException if {@code streamId} is not a valid simulcast stream
     *                                   id
     */
    public OptionalInt videoFecSsrc(int streamId) {
        return ssrcAt(videoFecSsrc, streamId);
    }

    /**
     * Returns the out of band NACK SSRC of the given simulcast video stream.
     *
     * @param streamId the simulcast stream id, in {@code 0..}{@value #SIMULCAST_STREAM_COUNT}
     *                 exclusive
     * @return the out of band NACK SSRC, or {@link OptionalInt#empty()} if it has not been
     *         generated
     * @throws IndexOutOfBoundsException if {@code streamId} is not a valid simulcast stream
     *                                   id
     */
    public OptionalInt videoOobNackSsrc(int streamId) {
        return ssrcAt(videoOobNackSsrc, streamId);
    }

    /**
     * Installs the SSRC triple of one simulcast video stream.
     *
     * <p>Records the primary, FEC, and out of band NACK SSRCs the secure SSRC generator
     * produced for the given simulcast stream, replacing any previously installed triple.
     *
     * @param streamId    the simulcast stream id, in
     *                    {@code 0..}{@value #SIMULCAST_STREAM_COUNT} exclusive
     * @param primarySsrc the primary SSRC
     * @param fecSsrc     the FEC SSRC
     * @param oobNackSsrc the out of band NACK SSRC
     * @return this media object
     * @throws IndexOutOfBoundsException if {@code streamId} is not a valid simulcast stream
     *                                   id
     */
    public CallParticipantMedia videoStreamSsrcs(int streamId, int primarySsrc, int fecSsrc, int oobNackSsrc) {
        Objects.checkIndex(streamId, SIMULCAST_STREAM_COUNT);
        videoPrimarySsrc[streamId] = primarySsrc;
        videoFecSsrc[streamId] = fecSsrc;
        videoOobNackSsrc[streamId] = oobNackSsrc;
        return this;
    }

    /**
     * Returns the screenshare stream's primary SSRC.
     *
     * @return the screenshare primary SSRC, or {@link OptionalInt#empty()} if it has not
     *         been generated
     */
    public OptionalInt screenSharePrimarySsrc() {
        return screenSharePrimarySsrc == UNASSIGNED_SSRC
                ? OptionalInt.empty()
                : OptionalInt.of(screenSharePrimarySsrc);
    }

    /**
     * Returns the screenshare stream's FEC SSRC.
     *
     * @return the screenshare FEC SSRC, or {@link OptionalInt#empty()} if it has not been
     *         generated
     */
    public OptionalInt screenShareFecSsrc() {
        return screenShareFecSsrc == UNASSIGNED_SSRC
                ? OptionalInt.empty()
                : OptionalInt.of(screenShareFecSsrc);
    }

    /**
     * Returns the screenshare stream's out of band NACK SSRC.
     *
     * @return the screenshare out of band NACK SSRC, or {@link OptionalInt#empty()} if it
     *         has not been generated
     */
    public OptionalInt screenShareOobNackSsrc() {
        return screenShareOobNackSsrc == UNASSIGNED_SSRC
                ? OptionalInt.empty()
                : OptionalInt.of(screenShareOobNackSsrc);
    }

    /**
     * Installs the SSRC triple of the screenshare stream.
     *
     * @param primarySsrc the primary SSRC
     * @param fecSsrc     the FEC SSRC
     * @param oobNackSsrc the out of band NACK SSRC
     * @return this media object
     */
    public CallParticipantMedia screenShareSsrcs(int primarySsrc, int fecSsrc, int oobNackSsrc) {
        this.screenSharePrimarySsrc = primarySsrc;
        this.screenShareFecSsrc = fecSsrc;
        this.screenShareOobNackSsrc = oobNackSsrc;
        return this;
    }

    /**
     * Returns the subscribed encoded video stream id.
     *
     * @return the subscribed video stream id, or {@link OptionalInt#empty()} if the
     *         participant is not subscribed to any encoded video stream
     */
    public OptionalInt subscribedVideoStreamId() {
        return subscribedVideoStreamId == NO_SUBSCRIBED_STREAM
                ? OptionalInt.empty()
                : OptionalInt.of(subscribedVideoStreamId);
    }

    /**
     * Sets the subscribed encoded video stream id.
     *
     * @param subscribedVideoStreamId the encoded stream id, or {@value #NO_SUBSCRIBED_STREAM}
     *                                to clear the subscription
     * @return this media object
     */
    public CallParticipantMedia subscribedVideoStreamId(int subscribedVideoStreamId) {
        this.subscribedVideoStreamId = subscribedVideoStreamId;
        return this;
    }

    /**
     * Returns the subscribed encoded screenshare stream id.
     *
     * @return the subscribed screenshare stream id, or {@link OptionalInt#empty()} if the
     *         participant is not subscribed to any encoded screenshare stream
     */
    public OptionalInt subscribedScreenShareStreamId() {
        return subscribedScreenShareStreamId == NO_SUBSCRIBED_STREAM
                ? OptionalInt.empty()
                : OptionalInt.of(subscribedScreenShareStreamId);
    }

    /**
     * Sets the subscribed encoded screenshare stream id.
     *
     * @param subscribedScreenShareStreamId the encoded stream id, or
     *                                       {@value #NO_SUBSCRIBED_STREAM} to clear the
     *                                       subscription
     * @return this media object
     */
    public CallParticipantMedia subscribedScreenShareStreamId(int subscribedScreenShareStreamId) {
        this.subscribedScreenShareStreamId = subscribedScreenShareStreamId;
        return this;
    }

    /**
     * Returns whether the participant is currently subscribed to an encoded video stream.
     *
     * @return {@code true} if a video stream subscription is set
     */
    public boolean hasVideoSubscription() {
        return subscribedVideoStreamId != NO_SUBSCRIBED_STREAM;
    }

    /**
     * Returns the SSRC at the given simulcast stream index, or empty when unassigned.
     *
     * @param array    the per stream SSRC array
     * @param streamId the simulcast stream id
     * @return the SSRC, or {@link OptionalInt#empty()} if unassigned
     * @throws IndexOutOfBoundsException if {@code streamId} is not a valid simulcast stream
     *                                   id
     */
    private static OptionalInt ssrcAt(int[] array, int streamId) {
        Objects.checkIndex(streamId, SIMULCAST_STREAM_COUNT);
        var value = array[streamId];
        return value == UNASSIGNED_SSRC ? OptionalInt.empty() : OptionalInt.of(value);
    }

    /**
     * Returns a fresh per stream SSRC array initialized to the unassigned sentinel.
     *
     * @return a new array of length {@value #SIMULCAST_STREAM_COUNT}
     */
    private static int[] newUnassignedStreamArray() {
        var array = new int[SIMULCAST_STREAM_COUNT];
        Arrays.fill(array, UNASSIGNED_SSRC);
        return array;
    }

    /**
     * Returns a diagnostic representation of this media object.
     *
     * <p>Reports whether the audio SSRC has been assigned and the two subscribed stream ids;
     * the individual video and screenshare SSRC triples are omitted to keep the line short.
     *
     * @return a string describing the assignment and subscription state
     */
    @Override
    public String toString() {
        return "CallParticipantMedia[audioSsrcAssigned=" + (audioSsrc != UNASSIGNED_SSRC)
                + ", subscribedVideoStreamId=" + subscribedVideoStreamId
                + ", subscribedScreenShareStreamId=" + subscribedScreenShareStreamId
                + ']';
    }
}
