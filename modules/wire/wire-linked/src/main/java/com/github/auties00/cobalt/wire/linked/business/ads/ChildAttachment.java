package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One card of a Click-to-WhatsApp ad carousel creative.
 *
 * <p>A carousel ad shows several swipeable cards, each with its own image or video and destination.
 * This model is one such card: its {@link #callToAction() call-to-action}, {@link #description()
 * description}, {@link #imageHash() image} or {@link #videoId() video} reference, {@link #link() link},
 * {@link #name() name}, and {@link #picture() picture}.
 */
@ProtobufMessage(name = "ChildAttachment")
public final class ChildAttachment {
    /**
     * Call-to-action button of the card. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CallToAction callToAction;

    /**
     * Description shown on the card. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Hash of the card's image. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String imageHash;

    /**
     * Destination link the card opens. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String link;

    /**
     * Headline name of the card. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String name;

    /**
     * Picture URL of the card. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String picture;

    /**
     * Identifier of the card's video. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String videoId;

    /**
     * Constructs a new {@code ChildAttachment}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param callToAction the call-to-action button, or {@code null}
     * @param description  the card description, or {@code null}
     * @param imageHash    the image hash, or {@code null}
     * @param link         the destination link, or {@code null}
     * @param name         the headline name, or {@code null}
     * @param picture      the picture URL, or {@code null}
     * @param videoId      the video identifier, or {@code null}
     */
    ChildAttachment(CallToAction callToAction, String description, String imageHash, String link,
                    String name, String picture, String videoId) {
        this.callToAction = callToAction;
        this.description = description;
        this.imageHash = imageHash;
        this.link = link;
        this.name = name;
        this.picture = picture;
        this.videoId = videoId;
    }

    /**
     * Returns the call-to-action button of the card.
     *
     * @return an {@link Optional} carrying the call-to-action, or empty when unset
     */
    public Optional<CallToAction> callToAction() {
        return Optional.ofNullable(callToAction);
    }

    /**
     * Returns the description shown on the card.
     *
     * @return an {@link Optional} carrying the description, or empty when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the hash of the card's image.
     *
     * @return an {@link Optional} carrying the image hash, or empty when unset
     */
    public Optional<String> imageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * Returns the destination link the card opens.
     *
     * @return an {@link Optional} carrying the link, or empty when unset
     */
    public Optional<String> link() {
        return Optional.ofNullable(link);
    }

    /**
     * Returns the headline name of the card.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the picture URL of the card.
     *
     * @return an {@link Optional} carrying the picture URL, or empty when unset
     */
    public Optional<String> picture() {
        return Optional.ofNullable(picture);
    }

    /**
     * Returns the identifier of the card's video.
     *
     * @return an {@link Optional} carrying the video identifier, or empty when unset
     */
    public Optional<String> videoId() {
        return Optional.ofNullable(videoId);
    }
}
