package com.github.auties00.cobalt.wire.linked.message.location;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents a live location share sent in a WhatsApp chat.
 *
 * <p>A live location message allows a user to continuously share their
 * real-time position with a contact or group for a bounded amount of time.
 * Unlike a static {@link LocationMessage}, a live location is periodically
 * updated by the sender's device so that recipients can follow movement
 * on a map until the share expires or is stopped manually.
 *
 * <p>In addition to the geographic coordinates, a live location carries
 * optional motion data such as speed, heading, and positional accuracy,
 * as well as a caption, a JPEG thumbnail preview and a sequence number
 * used by clients to order successive updates of the same share.
 */
@ProtobufMessage(name = "Message.LiveLocationMessage")
public final class LiveLocationMessage implements ContextualMessage {
    /**
     * The latitude of the sender's current position, expressed in decimal
     * degrees. Positive values are north of the equator, negative values
     * are south.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    Double degreesLatitude;

    /**
     * The longitude of the sender's current position, expressed in decimal
     * degrees. Positive values are east of the prime meridian, negative
     * values are west.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    Double degreesLongitude;

    /**
     * The horizontal accuracy of the reported position, expressed in
     * meters. Smaller values indicate a more precise fix from the
     * underlying location service.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer accuracyInMeters;

    /**
     * The instantaneous speed of the sender at the time of the update,
     * expressed in meters per second.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.FLOAT)
    Float speedInMps;

    /**
     * The direction the sender is facing, expressed in degrees measured
     * clockwise from magnetic north. A value of zero indicates magnetic
     * north, ninety indicates east, and so on.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer degreesClockwiseFromMagneticNorth;

    /**
     * An optional free-form text caption displayed alongside the live
     * location share in the chat preview.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String caption;

    /**
     * A monotonically increasing sequence number used by clients to
     * determine the ordering of successive updates that belong to the
     * same live location share and discard stale updates.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64)
    Long sequenceNumber;

    /**
     * The number of seconds that have elapsed since the start of the
     * live location share, used by receivers to render the remaining
     * share duration.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.UINT32)
    Integer timeOffset;

    /**
     * A JPEG-encoded map thumbnail displayed as a preview of the live
     * location in the chat list.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    byte[] jpegThumbnail;

    /**
     * The contextual metadata attached to this message, including
     * information about quoted messages, mentions and forwarding.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new live location message with the provided payload.
     *
     * <p>This constructor is package-private because instances are
     * expected to be produced either by the generated protobuf builder
     * {@code LiveLocationMessageBuilder} or by the protobuf deserializer.
     *
     * @param degreesLatitude the latitude in decimal degrees, may be {@code null}
     * @param degreesLongitude the longitude in decimal degrees, may be {@code null}
     * @param accuracyInMeters the horizontal accuracy in meters, may be {@code null}
     * @param speedInMps the speed in meters per second, may be {@code null}
     * @param degreesClockwiseFromMagneticNorth the heading in degrees from magnetic north, may be {@code null}
     * @param caption the caption displayed with the share, may be {@code null}
     * @param sequenceNumber the update sequence number, may be {@code null}
     * @param timeOffset the seconds elapsed since the share started, may be {@code null}
     * @param jpegThumbnail the JPEG thumbnail bytes, may be {@code null}
     * @param contextInfo the contextual metadata for this message, may be {@code null}
     */
    LiveLocationMessage(Double degreesLatitude, Double degreesLongitude, Integer accuracyInMeters, Float speedInMps, Integer degreesClockwiseFromMagneticNorth, String caption, Long sequenceNumber, Integer timeOffset, byte[] jpegThumbnail, ContextInfo contextInfo) {
        this.degreesLatitude = degreesLatitude;
        this.degreesLongitude = degreesLongitude;
        this.accuracyInMeters = accuracyInMeters;
        this.speedInMps = speedInMps;
        this.degreesClockwiseFromMagneticNorth = degreesClockwiseFromMagneticNorth;
        this.caption = caption;
        this.sequenceNumber = sequenceNumber;
        this.timeOffset = timeOffset;
        this.jpegThumbnail = jpegThumbnail;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the latitude of the sender's current position.
     *
     * @return an {@link OptionalDouble} containing the latitude in decimal degrees,
     *         or {@link OptionalDouble#empty()} if no latitude was reported
     */
    public OptionalDouble degreesLatitude() {
        return degreesLatitude == null ? OptionalDouble.empty() : OptionalDouble.of(degreesLatitude);
    }

    /**
     * Returns the longitude of the sender's current position.
     *
     * @return an {@link OptionalDouble} containing the longitude in decimal degrees,
     *         or {@link OptionalDouble#empty()} if no longitude was reported
     */
    public OptionalDouble degreesLongitude() {
        return degreesLongitude == null ? OptionalDouble.empty() : OptionalDouble.of(degreesLongitude);
    }

    /**
     * Returns the horizontal accuracy of the reported position.
     *
     * @return an {@link OptionalInt} containing the accuracy in meters,
     *         or {@link OptionalInt#empty()} if no accuracy was reported
     */
    public OptionalInt accuracyInMeters() {
        return accuracyInMeters == null ? OptionalInt.empty() : OptionalInt.of(accuracyInMeters);
    }

    /**
     * Returns the instantaneous speed of the sender.
     *
     * @return an {@link OptionalDouble} containing the speed in meters per second,
     *         or {@link OptionalDouble#empty()} if no speed was reported
     */
    public OptionalDouble speedInMps() {
        return speedInMps == null ? OptionalDouble.empty() : OptionalDouble.of(speedInMps);
    }

    /**
     * Returns the direction the sender is facing.
     *
     * @return an {@link OptionalInt} containing the heading in degrees
     *         measured clockwise from magnetic north,
     *         or {@link OptionalInt#empty()} if no heading was reported
     */
    public OptionalInt degreesClockwiseFromMagneticNorth() {
        return degreesClockwiseFromMagneticNorth == null ? OptionalInt.empty() : OptionalInt.of(degreesClockwiseFromMagneticNorth);
    }

    /**
     * Returns the free-form text caption associated with the share.
     *
     * @return an {@link Optional} containing the caption,
     *         or {@link Optional#empty()} if no caption was set
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the sequence number used to order updates of this live
     * location share.
     *
     * @return an {@link OptionalLong} containing the sequence number,
     *         or {@link OptionalLong#empty()} if no sequence number was provided
     */
    public OptionalLong sequenceNumber() {
        return sequenceNumber == null ? OptionalLong.empty() : OptionalLong.of(sequenceNumber);
    }

    /**
     * Returns the number of seconds that have elapsed since the start of
     * the live location share.
     *
     * @return an {@link OptionalInt} containing the elapsed seconds,
     *         or {@link OptionalInt#empty()} if no time offset was provided
     */
    public OptionalInt timeOffset() {
        return timeOffset == null ? OptionalInt.empty() : OptionalInt.of(timeOffset);
    }

    /**
     * Returns the JPEG-encoded map thumbnail used as a preview.
     *
     * @return an {@link Optional} containing the thumbnail bytes,
     *         or {@link Optional#empty()} if no thumbnail is attached
     */
    public Optional<byte[]> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }

    /**
     * Returns the contextual metadata attached to this message.
     *
     * @return an {@link Optional} containing the {@link ContextInfo},
     *         or {@link Optional#empty()} if no contextual metadata is present
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Sets the latitude of the reported position.
     *
     * @param degreesLatitude the latitude in decimal degrees, or {@code null} to clear
     */
    public void setDegreesLatitude(Double degreesLatitude) {
        this.degreesLatitude = degreesLatitude;
    }

    /**
     * Sets the longitude of the reported position.
     *
     * @param degreesLongitude the longitude in decimal degrees, or {@code null} to clear
     */
    public void setDegreesLongitude(Double degreesLongitude) {
        this.degreesLongitude = degreesLongitude;
    }

    /**
     * Sets the horizontal accuracy of the reported position.
     *
     * @param accuracyInMeters the accuracy in meters, or {@code null} to clear
     */
    public void setAccuracyInMeters(Integer accuracyInMeters) {
        this.accuracyInMeters = accuracyInMeters;
    }

    /**
     * Sets the instantaneous speed of the sender.
     *
     * @param speedInMps the speed in meters per second, or {@code null} to clear
     */
    public void setSpeedInMps(Float speedInMps) {
        this.speedInMps = speedInMps;
    }

    /**
     * Sets the direction the sender is facing.
     *
     * @param degreesClockwiseFromMagneticNorth the heading in degrees measured clockwise
     *                                          from magnetic north, or {@code null} to clear
     */
    public void setDegreesClockwiseFromMagneticNorth(Integer degreesClockwiseFromMagneticNorth) {
        this.degreesClockwiseFromMagneticNorth = degreesClockwiseFromMagneticNorth;
    }

    /**
     * Sets the free-form text caption displayed with the share.
     *
     * @param caption the caption, or {@code null} to clear
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Sets the sequence number used to order updates of this live
     * location share.
     *
     * @param sequenceNumber the sequence number, or {@code null} to clear
     */
    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Sets the number of seconds that have elapsed since the start of
     * the live location share.
     *
     * @param timeOffset the elapsed seconds, or {@code null} to clear
     */
    public void setTimeOffset(Integer timeOffset) {
        this.timeOffset = timeOffset;
    }

    /**
     * Sets the JPEG-encoded map thumbnail used as a preview.
     *
     * @param jpegThumbnail the thumbnail bytes, or {@code null} to clear
     */
    public void setJpegThumbnail(byte[] jpegThumbnail) {
        this.jpegThumbnail = jpegThumbnail;
    }

    /**
     * Sets the contextual metadata attached to this message.
     *
     * @param contextInfo the {@link ContextInfo} to associate with this message,
     *                    or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}
