package com.github.auties00.cobalt.wire.stanza.iq.profilepicture;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.SizedInputStream;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound {@code <iq xmlns="w:profile:picture" type="set">} stanza that sets or
 * clears a profile picture.
 *
 * <p>A single request shape covers the four operations the relay accepts, selected by the
 * combination of {@link #groupTarget()} and {@link #picture()}: self-set, self-clear,
 * group-set, and group-clear. When {@link #groupTarget()} is present the picture belongs to
 * that group, otherwise it belongs to the calling user. When {@link #picture()} is present the
 * supplied stream becomes the new picture, otherwise the existing picture is cleared.
 *
 * <p>The picture is carried as a {@link SizedInputStream} so the payload is streamed straight to
 * the wire at serialisation time rather than buffered into a {@code byte[]} first; the relay
 * echoes the new picture id in its success reply. A self-set may additionally attach a
 * {@link #preview()} thumbnail streamed into a second {@code <picture type="preview">} child.
 */
@WhatsAppWebModule(moduleName = "WAWebSendProfilePictureJob")
public final class IqSendProfilePictureRequest implements IqStanza.Request {
    /**
     * Holds the target group JID when updating a group profile picture, or {@code null} when
     * updating the calling user's own picture.
     *
     * <p>When present this value is stamped into the {@code target} attribute on the
     * {@code <iq>} envelope; when {@code null} the attribute is omitted entirely.
     */
    private final Jid groupTarget;

    /**
     * Holds the new JPEG profile-picture stream, or {@code null} to clear the existing picture.
     *
     * <p>A non-{@code null} value is streamed into a {@code <picture type="image">} child of the
     * {@code <iq>} envelope; a {@code null} value produces an envelope with no
     * {@code <picture/>} child, which the relay interprets as a clear request.
     */
    private final SizedInputStream picture;

    /**
     * Holds the optional preview thumbnail stream, or {@code null} when no preview is supplied.
     *
     * <p>A non-{@code null} value is streamed into a second {@code <picture type="preview">} child
     * appended after the full {@code type="image"} picture; the self-picture set path supplies it so
     * the relay caches a downscaled thumbnail alongside the full image.
     */
    private final SizedInputStream preview;

    /**
     * Constructs a send-profile-picture request from a nullable group target and nullable
     * picture stream.
     *
     * <p>Both nullable arguments let one constructor express all four (set or clear) by (self or
     * group) call shapes. Delegates to {@link #IqSendProfilePictureRequest(Jid, SizedInputStream,
     * SizedInputStream)} with no preview thumbnail.
     *
     * @param groupTarget the group JID for a group-picture update, or {@code null} for a
     *                    self-picture update
     * @param picture     the new JPEG picture, or {@code null} to clear the existing picture
     */
    public IqSendProfilePictureRequest(Jid groupTarget, SizedInputStream picture) {
        this(groupTarget, picture, null);
    }

    /**
     * Constructs a send-profile-picture request that additionally carries a preview thumbnail.
     *
     * <p>When {@code preview} is present a second {@code <picture type="preview">} child is appended
     * after the full {@code type="image"} picture; when it is {@code null} the request behaves
     * exactly like {@link #IqSendProfilePictureRequest(Jid, SizedInputStream)}.
     *
     * @param groupTarget the group JID for a group-picture update, or {@code null} for a
     *                    self-picture update
     * @param picture     the new JPEG picture, or {@code null} to clear the existing picture
     * @param preview     the preview thumbnail, or {@code null} to omit it
     */
    public IqSendProfilePictureRequest(Jid groupTarget, SizedInputStream picture, SizedInputStream preview) {
        this.groupTarget = groupTarget;
        this.picture = picture;
        this.preview = preview;
    }

    /**
     * Returns the optional group JID target of this update.
     *
     * @return an {@link Optional} carrying the group JID, or {@link Optional#empty()} when this
     *         is a self-picture update
     */
    public Optional<Jid> groupTarget() {
        return Optional.ofNullable(groupTarget);
    }

    /**
     * Returns the optional new picture stream of this update.
     *
     * @return an {@link Optional} carrying the picture stream, or {@link Optional#empty()} when
     *         the picture is being cleared
     */
    public Optional<SizedInputStream> picture() {
        return Optional.ofNullable(picture);
    }

    /**
     * Returns the optional preview thumbnail stream of this update.
     *
     * @return an {@link Optional} carrying the preview stream, or {@link Optional#empty()} when no
     *         preview is supplied
     */
    public Optional<SizedInputStream> preview() {
        return Optional.ofNullable(preview);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq xmlns="w:profile:picture" type="set">} envelope addressed to
     * {@link JidServer#user()}. The {@code target} attribute is stamped only when
     * {@link #groupTarget()} is present, a {@code <picture type="image">} child is appended only
     * when {@link #picture()} is present, and a second {@code <picture type="preview">} child is
     * appended only when {@link #preview()} is present, streaming each payload at serialisation time.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the optional
     *         {@code <picture>} payloads
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSendProfilePictureJob",
            exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var iqBuilder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:profile:picture")
                .attribute("to", JidServer.user())
                .attribute("type", "set");
        if (groupTarget != null) {
            iqBuilder.attribute("target", groupTarget);
        }
        if (picture != null) {
            var pictureNode = new StanzaBuilder()
                    .description("picture")
                    .attribute("type", "image")
                    .content(picture)
                    .build();
            iqBuilder.content(pictureNode);
        }
        if (preview != null) {
            var previewNode = new StanzaBuilder()
                    .description("picture")
                    .attribute("type", "preview")
                    .content(preview)
                    .build();
            iqBuilder.content(previewNode);
        }
        return iqBuilder;
    }

    /**
     * Compares this request with another object for value equality.
     *
     * <p>Two requests are equal when their group targets are equal and they carry the same
     * picture and preview stream instances (a sized stream has no value equality).
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal {@link IqSendProfilePictureRequest},
     *         otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSendProfilePictureRequest) obj;
        return Objects.equals(this.groupTarget, that.groupTarget)
                && this.picture == that.picture
                && this.preview == that.preview;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the combined hash of the group target and the advertised picture and preview lengths
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupTarget,
                picture == null ? -1L : picture.length(),
                preview == null ? -1L : preview.length());
    }

    /**
     * Returns a debug representation of this request.
     *
     * <p>The picture and preview are summarised as their advertised lengths rather than read, and a
     * length of {@code -1} denotes an absent stream.
     *
     * @return a string carrying the group target and the picture and preview lengths
     */
    @Override
    public String toString() {
        return "IqSendProfilePictureRequest[groupTarget=" + groupTarget
                + ", pictureLength=" + (picture == null ? -1 : picture.length())
                + ", previewLength=" + (preview == null ? -1 : preview.length()) + ']';
    }
}
