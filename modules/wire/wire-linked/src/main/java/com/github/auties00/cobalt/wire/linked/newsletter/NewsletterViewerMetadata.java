package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes the current viewer's relationship with a newsletter.
 *
 * <p>This bundle contains the viewer-specific state that the server tracks
 * per session:
 * <ul>
 *   <li>whether the viewer has muted notifications for this newsletter</li>
 *   <li>the {@link NewsletterViewerRole} the viewer holds (owner,
 *       subscriber, admin, or guest)</li>
 *   <li>the viewer's {@link NewsletterWamoSubStatus}, describing whether
 *       any WAMO subscription tied to the newsletter is active</li>
 * </ul>
 *
 * <p>Viewer metadata is always scoped to the local device; it is not
 * shared with other viewers.
 */
@ProtobufMessage
public final class NewsletterViewerMetadata {
    /**
     * Whether the current viewer has muted notifications for this
     * newsletter.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    boolean mute;

    /**
     * The role the current viewer holds within this newsletter. Defaults
     * to {@link NewsletterViewerRole#UNKNOWN} when not reported.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    NewsletterViewerRole role;

    /**
     * The WAMO subscription status the current viewer holds for this
     * newsletter, or {@code null} if no subscription exists.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    NewsletterWamoSubStatus wamoSubStatus;

    /**
     * Constructs a new {@code NewsletterViewerMetadata}. Invoked by the
     * generated protobuf deserializer.
     *
     * @param mute          whether the viewer has muted the newsletter
     * @param role          the viewer's role, defaulted to
     *                      {@link NewsletterViewerRole#UNKNOWN} when
     *                      {@code null}
     * @param wamoSubStatus the viewer's WAMO subscription status, may be
     *                      {@code null}
     */
    NewsletterViewerMetadata(boolean mute, NewsletterViewerRole role, NewsletterWamoSubStatus wamoSubStatus) {
        this.mute = mute;
        this.role = Objects.requireNonNullElse(role, NewsletterViewerRole.UNKNOWN);
        this.wamoSubStatus = wamoSubStatus;
    }

    /**
     * Returns whether the current viewer has muted notifications for this
     * newsletter.
     *
     * @return {@code true} when the newsletter is muted
     */
    public boolean mute() {
        return mute;
    }

    /**
     * Returns the role the current viewer holds within this newsletter.
     *
     * @return the viewer role, never {@code null}
     */
    public NewsletterViewerRole role() {
        return role;
    }

    /**
     * Returns the WAMO subscription status held by the current viewer.
     *
     * @return an {@link Optional} holding the WAMO status, or empty when
     *         no subscription exists
     */
    public Optional<NewsletterWamoSubStatus> wamoSubStatus() {
        return Optional.ofNullable(wamoSubStatus);
    }

    /**
     * Sets whether the current viewer has muted notifications for this
     * newsletter.
     *
     * @param mute the new mute flag
     */
    public void setMute(boolean mute) {
        this.mute = mute;
    }

    /**
     * Sets the role the current viewer holds within this newsletter.
     *
     * @param role the new role, defaulted to
     *             {@link NewsletterViewerRole#UNKNOWN} when {@code null}
     */
    public void setRole(NewsletterViewerRole role) {
        this.role = Objects.requireNonNullElse(role, NewsletterViewerRole.UNKNOWN);
    }

    /**
     * Sets the WAMO subscription status held by the current viewer.
     *
     * @param wamoSubStatus the new WAMO status, or {@code null}
     */
    public void setWamoSubStatus(NewsletterWamoSubStatus wamoSubStatus) {
        this.wamoSubStatus = wamoSubStatus;
    }

    /**
     * Returns whether this metadata equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterViewerMetadata} whose fields are all equal
     *         to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterViewerMetadata that
                && mute == that.mute
                && role == that.role
                && wamoSubStatus == that.wamoSubStatus;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this metadata
     */
    @Override
    public int hashCode() {
        return Objects.hash(mute, role, wamoSubStatus);
    }
}
