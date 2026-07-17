package com.github.auties00.cobalt.wire.linked.chat.group;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Objects;

/**
 * Represents the profile picture metadata returned when querying the picture
 * attached to a WhatsApp group invite link.
 *
 * <p>When a user opens a group-invite link before joining the group, the
 * client fetches the group's profile picture so it can be displayed in the
 * preview screen. The server replies with the descriptors needed both to
 * render the picture immediately (a directly-usable download URL) and to
 * refetch or cache the picture later (the media-server identifier and direct
 * path). This value class bundles those four descriptors together and is
 * immutable once constructed.
 */
@ProtobufMessage
public final class GroupInvitePicture {
    /**
     * The picture identifier on the WhatsApp media server. Combined with the
     * direct path to address the picture bytes when refetching or caching.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The resolution variant of the picture. {@code "image"} indicates the
     * full-size profile picture, {@code "preview"} indicates the smaller
     * thumbnail used for list views.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String type;

    /**
     * The directly-usable download URL for the picture. Stored as a
     * {@link String} for protobuf wire compatibility and exposed as a
     * {@link URI} through {@link #url()}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String url;

    /**
     * The media-server direct path used to fetch the picture bytes. Combined
     * with the picture identifier when reissuing the download.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String directPath;

    /**
     * Constructs a new {@code GroupInvitePicture} with the on-the-wire field
     * types. All arguments are required and must be non-{@code null}.
     *
     * @param id         the picture identifier on the media server
     * @param type       the resolution variant ({@code "image"} or {@code "preview"})
     * @param url        the download URL as a string
     * @param directPath the media-server direct path
     * @throws NullPointerException if any argument is {@code null}
     */
    GroupInvitePicture(String id, String type, String url, String directPath) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.url = Objects.requireNonNull(url, "url cannot be null");
        this.directPath = Objects.requireNonNull(directPath, "directPath cannot be null");
    }

    /**
     * Constructs a new {@code GroupInvitePicture} accepting a {@link URI} for
     * the download URL. The {@link URI} is converted to its string form before
     * delegation to the canonical constructor.
     *
     * @param id         the picture identifier on the media server
     * @param type       the resolution variant ({@code "image"} or {@code "preview"})
     * @param url        the download URL
     * @param directPath the media-server direct path
     * @throws NullPointerException if any argument is {@code null}
     */
    public GroupInvitePicture(String id, String type, URI url, String directPath) {
        this(id, type, Objects.requireNonNull(url, "url cannot be null").toString(), directPath);
    }

    /**
     * Returns the picture identifier on the WhatsApp media server.
     *
     * @return the identifier; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the resolution variant of the picture, either {@code "image"}
     * for the full-size picture or {@code "preview"} for the thumbnail.
     *
     * @return the resolution variant; never {@code null}
     */
    public String type() {
        return type;
    }

    /**
     * Returns the directly-usable download URL for the picture, parsed from
     * the on-the-wire string form into a {@link URI}.
     *
     * @return the download URL; never {@code null}
     */
    public URI url() {
        return URI.create(url);
    }

    /**
     * Returns the media-server direct path used when fetching the picture
     * bytes from the CDN.
     *
     * @return the direct path; never {@code null}
     */
    public String directPath() {
        return directPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupInvitePicture) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.type, that.type) &&
               Objects.equals(this.url, that.url) &&
               Objects.equals(this.directPath, that.directPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, url, directPath);
    }

    @Override
    public String toString() {
        return "GroupInvitePicture[" +
               "id=" + id + ", " +
               "type=" + type + ", " +
               "url=" + url + ", " +
               "directPath=" + directPath + ']';
    }
}
