package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.MediaContent;
import com.github.auties00.cobalt.wire.core.message.MediaType;

import java.util.Optional;

/**
 * The Cloud transport's media content body (image, video, audio, document, or sticker).
 */
public final class CloudMediaContent implements MediaContent {
    /**
     * The kind of media; never {@code null}.
     */
    private final MediaType type;

    /**
     * The opaque Cloud media id or hosted link, or {@code null} when unset.
     */
    private final String mediaUrl;

    /**
     * The MIME type, or {@code null} when unset.
     */
    private final String mimetype;

    /**
     * The caption, or {@code null} when unset.
     */
    private final String caption;

    /**
     * Constructs a Cloud media body.
     *
     * @param type     the kind of media; never {@code null}
     * @param mediaUrl the opaque Cloud media id or hosted link, or {@code null} when unset
     * @param mimetype the MIME type, or {@code null} when unset
     * @param caption  the caption, or {@code null} when unset
     */
    public CloudMediaContent(MediaType type, String mediaUrl, String mimetype, String caption) {
        this.type = type;
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.caption = caption;
    }

    @Override
    public MediaType type() {
        return type;
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    @Override
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    @Override
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }
}
