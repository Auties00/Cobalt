package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A message that groups multiple images and/or videos together into a single
 * visual album.
 *
 * <p>Album messages act as a parent envelope advertising how many image and
 * video items the recipient should expect as children of the album. The
 * individual {@link ImageMessage} and {@link VideoMessage} payloads are sent
 * separately and associated with the album via their context. Clients use
 * the announced counts to render a placeholder grid while the children are
 * still arriving and to know when the album has been fully delivered.
 */
@ProtobufMessage(name = "Message.AlbumMessage")
public final class AlbumMessage implements ContextualMessage {
    /**
     * Advertised number of {@link ImageMessage} items that belong to this
     * album. Set at send time from the album's children and used by the
     * recipient to render a placeholder grid before the individual images
     * arrive.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer expectedImageCount;

    /**
     * Advertised number of {@link VideoMessage} items that belong to this
     * album. Set at send time from the album's children and used by the
     * recipient to render a placeholder grid before the individual videos
     * arrive.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer expectedVideoCount;

    /**
     * Contextual information attached to the album, such as a quoted message,
     * forwarding score, mentioned participants or ephemeral-message hints.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new album message with the given counts and context.
     *
     * @param expectedImageCount the number of image items the album should contain, or {@code null}
     * @param expectedVideoCount the number of video items the album should contain, or {@code null}
     * @param contextInfo        the context information, or {@code null} if none
     */
    AlbumMessage(Integer expectedImageCount, Integer expectedVideoCount, ContextInfo contextInfo) {
        this.expectedImageCount = expectedImageCount;
        this.expectedVideoCount = expectedVideoCount;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the number of image items expected to be part of this album.
     *
     * @return the image count wrapped in {@link OptionalInt}, or empty if unset
     */
    public OptionalInt expectedImageCount() {
        return expectedImageCount == null ? OptionalInt.empty() : OptionalInt.of(expectedImageCount);
    }

    /**
     * Returns the number of video items expected to be part of this album.
     *
     * @return the video count wrapped in {@link OptionalInt}, or empty if unset
     */
    public OptionalInt expectedVideoCount() {
        return expectedVideoCount == null ? OptionalInt.empty() : OptionalInt.of(expectedVideoCount);
    }

    /**
     * Returns the context information attached to this album.
     *
     * @return the context info, or an empty {@link Optional} if none is attached
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Sets the expected image count.
     *
     * @param expectedImageCount the new image count, or {@code null} to unset
     */
    public void setExpectedImageCount(Integer expectedImageCount) {
        this.expectedImageCount = expectedImageCount;
    }

    /**
     * Sets the expected video count.
     *
     * @param expectedVideoCount the new video count, or {@code null} to unset
     */
    public void setExpectedVideoCount(Integer expectedVideoCount) {
        this.expectedVideoCount = expectedVideoCount;
    }

    /**
     * Sets the context information attached to this album.
     *
     * @param contextInfo the new context info, or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}
