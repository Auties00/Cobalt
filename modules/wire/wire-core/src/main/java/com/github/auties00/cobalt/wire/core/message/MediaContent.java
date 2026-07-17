package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;

/**
 * Transport-agnostic contract for a media message body (image, video, audio, document, or sticker).
 *
 * <p>This is the shared surface both transports expose for media content. The Cloud representation is
 * intentionally thin: an opaque media reference plus a MIME type and an optional caption. The Linked
 * transport's own media descriptors additionally carry encryption keys, SHA hashes, direct paths, and
 * CDN metadata that the Cloud wire never delivers; those stay on the Linked media types and are not
 * part of this contract.
 */
public interface MediaContent extends MessageContent {
    /**
     * Returns the kind of media this body carries.
     *
     * @return the media kind, never {@code null}
     */
    MediaType type();

    /**
     * Returns the reference by which the media is fetched: a hosted {@code http(s)} link or an opaque
     * Cloud media id, depending on the transport.
     *
     * @return an {@link Optional} holding the media reference, or empty when none is set
     */
    Optional<String> mediaUrl();

    /**
     * Returns the MIME type of the media, when known.
     *
     * @return an {@link Optional} holding the MIME type, or empty when unset
     */
    Optional<String> mimetype();

    /**
     * Returns the caption accompanying the media, when present.
     *
     * @return an {@link Optional} holding the caption, or empty when the media carries none
     */
    Optional<String> caption();
}
