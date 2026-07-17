package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the profile picture descriptor returned for a single group when
 * batch-querying community pictures.
 *
 * <p>WhatsApp's relay returns one of two projections per requested group: a
 * URL projection carrying a directly-usable download link plus the media
 * server's direct path, or a blob projection carrying the picture bytes
 * inline. This class unifies both projections — exactly one of
 * {@link #url()} and {@link #blob()} is present on a successful entry, while
 * both are empty when the relay reported an unchanged or not-found state for
 * the requested group.
 *
 * <p>The {@link #groupJid()} accessor returns the group the picture belongs
 * to, irrespective of whether the entry came back as a parent-group or
 * sub-group projection on the wire.
 */
@ProtobufMessage(name = "GroupProfilePicture")
public final class GroupProfilePicture {
    /**
     * The group this picture descriptor belongs to. Required, never
     * {@code null} after construction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid groupJid;

    /**
     * The picture identifier on the WhatsApp media server, or {@code null}
     * if the relay did not surface one (typical for not-found / unchanged
     * entries).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String pictureId;

    /**
     * The picture resolution variant ({@code "image"} for full-size or
     * {@code "preview"} for the thumbnail), or {@code null} if the relay
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String pictureType;

    /**
     * The directly-usable download URL for the picture, or {@code null}
     * when the relay returned the blob projection or no picture at all.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String url;

    /**
     * The media-server direct path used to fetch the picture bytes, or
     * {@code null} when the relay returned the blob projection or no
     * picture at all.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String directPath;

    /**
     * The inline picture bytes, populated only on the blob projection and
     * {@code null} otherwise.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] blob;

    /**
     * Constructs a new picture descriptor.
     *
     * @param groupJid    the group JID; must not be {@code null}
     * @param pictureId   the optional picture identifier
     * @param pictureType the optional resolution variant
     * @param url         the optional download URL
     * @param directPath  the optional media-server direct path
     * @param blob        the optional inline blob bytes
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    GroupProfilePicture(Jid groupJid, String pictureId, String pictureType,
                        String url, String directPath, byte[] blob) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.pictureId = pictureId;
        this.pictureType = pictureType;
        this.url = url;
        this.directPath = directPath;
        this.blob = blob;
    }

    /**
     * Returns the group this picture descriptor belongs to.
     *
     * @return the non-{@code null} group JID
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the picture identifier on the media server when the relay
     * supplied one.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         the relay omitted it
     */
    public Optional<String> pictureId() {
        return Optional.ofNullable(pictureId);
    }

    /**
     * Returns the resolution variant of the picture
     * ({@code "image"} or {@code "preview"}) when the relay supplied one.
     *
     * @return an {@link Optional} carrying the variant, or empty when the
     *         relay omitted it
     */
    public Optional<String> pictureType() {
        return Optional.ofNullable(pictureType);
    }

    /**
     * Returns the directly-usable download URL when the relay returned the
     * URL projection.
     *
     * @return an {@link Optional} carrying the URL, or empty for the
     *         blob projection or for not-found / unchanged entries
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the media-server direct path when the relay returned the URL
     * projection.
     *
     * @return an {@link Optional} carrying the direct path, or empty for
     *         the blob projection or for not-found / unchanged entries
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(directPath);
    }

    /**
     * Returns the inline picture bytes when the relay returned the blob
     * projection.
     *
     * @return an {@link Optional} carrying the blob, or empty for the URL
     *         projection or for not-found / unchanged entries
     */
    public Optional<byte[]> blob() {
        return Optional.ofNullable(blob);
    }
}
