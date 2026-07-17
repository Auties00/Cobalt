package com.github.auties00.cobalt.wire.linked.message.location;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveHeader;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Represents a static location share sent in a WhatsApp chat.
 *
 * <p>A location message encapsulates a single point on the map along
 * with optional metadata such as a place name, an address, a website,
 * a heading, a speed and a JPEG thumbnail preview. Unlike a
 * {@link LiveLocationMessage}, a {@code LocationMessage} describes a
 * position captured at a single moment in time and is not updated
 * after it has been sent.
 *
 * <p>This message can be used as the header of an interactive or
 * template message, in which case it is rendered as a map preview at
 * the top of the interactive bubble. The {@link #isLive()} flag is set
 * by the server when the static snapshot was produced from a live
 * location share that has already stopped.
 */
@ProtobufMessage(name = "Message.LocationMessage")
public final class LocationMessage implements InteractiveHeader, InteractiveMessage.MediaSpec, TemplateMessage.Title, TemplateMessage.TitleSpec, ContextualMessage {
    /**
     * The latitude of the shared position, expressed in decimal degrees.
     * Positive values are north of the equator, negative values are south.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    Double degreesLatitude;

    /**
     * The longitude of the shared position, expressed in decimal degrees.
     * Positive values are east of the prime meridian, negative values are
     * west.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    Double degreesLongitude;

    /**
     * The display name of the place associated with the shared position,
     * such as a business name or point of interest.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String name;

    /**
     * The postal address or human-readable description of the shared
     * position, typically rendered underneath the place name.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String address;

    /**
     * An optional website associated with the shared place, such as the
     * page of the business or point of interest.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String url;

    /**
     * Whether this message represents a snapshot of a live location
     * share rather than a plain static location. Set by the server when
     * forwarding the final state of a live location that has ended.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean isLive;

    /**
     * The horizontal accuracy of the reported position, expressed in
     * meters. Smaller values indicate a more precise fix from the
     * underlying location service.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    Integer accuracyInMeters;

    /**
     * The instantaneous speed of the sender at the time the position
     * was captured, expressed in meters per second.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.FLOAT)
    Float speedInMps;

    /**
     * The direction the sender was facing when the position was
     * captured, expressed in degrees measured clockwise from magnetic
     * north. A value of zero indicates magnetic north, ninety indicates
     * east, and so on.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    Integer degreesClockwiseFromMagneticNorth;

    /**
     * An optional free-form text comment displayed alongside the
     * location in the chat preview.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String comment;

    /**
     * A JPEG-encoded map thumbnail displayed as a preview of the
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
     * Constructs a new location message with the provided payload.
     *
     * <p>This constructor is package-private because instances are
     * expected to be produced either by the generated protobuf builder
     * {@code LocationMessageBuilder} or by the protobuf deserializer.
     *
     * @param degreesLatitude the latitude in decimal degrees, may be {@code null}
     * @param degreesLongitude the longitude in decimal degrees, may be {@code null}
     * @param name the display name of the place, may be {@code null}
     * @param address the postal address of the place, may be {@code null}
     * @param url the associated website, may be {@code null}
     * @param isLive {@code true} if this is a snapshot of a live location share, may be {@code null}
     * @param accuracyInMeters the horizontal accuracy in meters, may be {@code null}
     * @param speedInMps the speed in meters per second, may be {@code null}
     * @param degreesClockwiseFromMagneticNorth the heading in degrees from magnetic north, may be {@code null}
     * @param comment the free-form comment, may be {@code null}
     * @param jpegThumbnail the JPEG thumbnail bytes, may be {@code null}
     * @param contextInfo the contextual metadata for this message, may be {@code null}
     */
    LocationMessage(Double degreesLatitude, Double degreesLongitude, String name, String address, String url, Boolean isLive, Integer accuracyInMeters, Float speedInMps, Integer degreesClockwiseFromMagneticNorth, String comment, byte[] jpegThumbnail, ContextInfo contextInfo) {
        this.degreesLatitude = degreesLatitude;
        this.degreesLongitude = degreesLongitude;
        this.name = name;
        this.address = address;
        this.url = url;
        this.isLive = isLive;
        this.accuracyInMeters = accuracyInMeters;
        this.speedInMps = speedInMps;
        this.degreesClockwiseFromMagneticNorth = degreesClockwiseFromMagneticNorth;
        this.comment = comment;
        this.jpegThumbnail = jpegThumbnail;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the latitude of the shared position.
     *
     * @return an {@link OptionalDouble} containing the latitude in decimal degrees,
     *         or {@link OptionalDouble#empty()} if no latitude was provided
     */
    public OptionalDouble degreesLatitude() {
        return degreesLatitude == null ? OptionalDouble.empty() : OptionalDouble.of(degreesLatitude);
    }

    /**
     * Returns the longitude of the shared position.
     *
     * @return an {@link OptionalDouble} containing the longitude in decimal degrees,
     *         or {@link OptionalDouble#empty()} if no longitude was provided
     */
    public OptionalDouble degreesLongitude() {
        return degreesLongitude == null ? OptionalDouble.empty() : OptionalDouble.of(degreesLongitude);
    }

    /**
     * Returns the display name of the place associated with the shared
     * position.
     *
     * @return an {@link Optional} containing the place name,
     *         or {@link Optional#empty()} if no name was provided
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the postal address or human-readable description of the
     * shared position.
     *
     * @return an {@link Optional} containing the address,
     *         or {@link Optional#empty()} if no address was provided
     */
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /**
     * Returns the website associated with the shared place.
     *
     * @return an {@link Optional} containing the URL,
     *         or {@link Optional#empty()} if no URL was provided
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns whether this message represents a snapshot of a live
     * location share.
     *
     * @return {@code true} if this is a snapshot of a live share,
     *         {@code false} if it is a plain static location or the flag was not set
     */
    public boolean isLive() {
        return isLive != null && isLive;
    }

    /**
     * Returns the horizontal accuracy of the reported position.
     *
     * @return an {@link OptionalInt} containing the accuracy in meters,
     *         or {@link OptionalInt#empty()} if no accuracy was provided
     */
    public OptionalInt accuracyInMeters() {
        return accuracyInMeters == null ? OptionalInt.empty() : OptionalInt.of(accuracyInMeters);
    }

    /**
     * Returns the instantaneous speed of the sender.
     *
     * @return an {@link OptionalDouble} containing the speed in meters per second,
     *         or {@link OptionalDouble#empty()} if no speed was provided
     */
    public OptionalDouble speedInMps() {
        return speedInMps == null ? OptionalDouble.empty() : OptionalDouble.of(speedInMps);
    }

    /**
     * Returns the direction the sender was facing when the position was
     * captured.
     *
     * @return an {@link OptionalInt} containing the heading in degrees
     *         measured clockwise from magnetic north,
     *         or {@link OptionalInt#empty()} if no heading was provided
     */
    public OptionalInt degreesClockwiseFromMagneticNorth() {
        return degreesClockwiseFromMagneticNorth == null ? OptionalInt.empty() : OptionalInt.of(degreesClockwiseFromMagneticNorth);
    }

    /**
     * Returns the free-form text comment associated with the location.
     *
     * @return an {@link Optional} containing the comment,
     *         or {@link Optional#empty()} if no comment was set
     */
    public Optional<String> comment() {
        return Optional.ofNullable(comment);
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
     * Sets the latitude of the shared position.
     *
     * @param degreesLatitude the latitude in decimal degrees, or {@code null} to clear
     */
    public void setDegreesLatitude(Double degreesLatitude) {
        this.degreesLatitude = degreesLatitude;
    }

    /**
     * Sets the longitude of the shared position.
     *
     * @param degreesLongitude the longitude in decimal degrees, or {@code null} to clear
     */
    public void setDegreesLongitude(Double degreesLongitude) {
        this.degreesLongitude = degreesLongitude;
    }

    /**
     * Sets the display name of the place associated with the shared
     * position.
     *
     * @param name the place name, or {@code null} to clear
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the postal address of the shared place.
     *
     * @param address the address, or {@code null} to clear
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Sets the website associated with the shared place.
     *
     * @param url the URL, or {@code null} to clear
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets whether this message represents a snapshot of a live location
     * share.
     *
     * @param isLive {@code true} if this is a snapshot of a live share,
     *               {@code false} for a plain static location,
     *               or {@code null} to clear the flag
     */
    public void setLive(Boolean isLive) {
        this.isLive = isLive;
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
     * Sets the direction the sender was facing when the position was
     * captured.
     *
     * @param degreesClockwiseFromMagneticNorth the heading in degrees measured clockwise
     *                                          from magnetic north, or {@code null} to clear
     */
    public void setDegreesClockwiseFromMagneticNorth(Integer degreesClockwiseFromMagneticNorth) {
        this.degreesClockwiseFromMagneticNorth = degreesClockwiseFromMagneticNorth;
    }

    /**
     * Sets the free-form text comment associated with the location.
     *
     * @param comment the comment, or {@code null} to clear
     */
    public void setComment(String comment) {
        this.comment = comment;
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
