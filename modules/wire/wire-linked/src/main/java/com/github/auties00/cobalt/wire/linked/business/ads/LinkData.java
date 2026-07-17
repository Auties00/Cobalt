package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

/**
 * Link-format creative content of a Click-to-WhatsApp ad's object story spec.
 *
 * <p>A single-image or carousel ad renders its creative through link data. This model carries the
 * fields WhatsApp Web populates: the {@link #callToAction() call-to-action}, the
 * {@link #childAttachments() carousel cards}, the {@link #description() description},
 * {@link #eventId() event}, {@link #imageCrops() image crops}, {@link #imageHash() image},
 * {@link #link() link}, {@link #message() message}, {@link #name() name}, {@link #picture() picture},
 * {@link #retailerItemIds() retailer item ids}, and the
 * {@link #useFlexibleImageAspectRatio() flexible-aspect-ratio} flag.
 */
@ProtobufMessage(name = "LinkData")
public final class LinkData {
    /**
     * Call-to-action button of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CallToAction callToAction;

    /**
     * Carousel cards of the creative, in the order they are shown. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<ChildAttachment> childAttachments;

    /**
     * Description shown under the creative. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String description;

    /**
     * Identifier of the linked event. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String eventId;

    /**
     * Serialized image-crop specification. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String imageCrops;

    /**
     * Hash of the creative image. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String imageHash;

    /**
     * Destination link the creative opens. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String link;

    /**
     * Primary text of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String message;

    /**
     * Headline name of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String name;

    /**
     * Picture URL of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String picture;

    /**
     * Retailer item identifiers referenced by the creative, in the order they are sent. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    final List<String> retailerItemIds;

    /**
     * Whether the creative may render its image at a flexible aspect ratio.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    final boolean useFlexibleImageAspectRatio;

    /**
     * Constructs a new {@code LinkData}. A {@code null} list argument is coerced to an empty list; every
     * other object argument may be {@code null} to leave the corresponding field unset.
     *
     * @param callToAction                the call-to-action button, or {@code null}
     * @param childAttachments            the carousel cards; {@code null} treated as empty
     * @param description                 the description, or {@code null}
     * @param eventId                     the linked-event identifier, or {@code null}
     * @param imageCrops                  the serialized image-crop specification, or {@code null}
     * @param imageHash                   the image hash, or {@code null}
     * @param link                        the destination link, or {@code null}
     * @param message                     the primary text, or {@code null}
     * @param name                        the headline name, or {@code null}
     * @param picture                     the picture URL, or {@code null}
     * @param retailerItemIds             the retailer item identifiers; {@code null} treated as empty
     * @param useFlexibleImageAspectRatio whether flexible image aspect ratio is allowed
     */
    LinkData(CallToAction callToAction, List<ChildAttachment> childAttachments, String description, String eventId,
             String imageCrops, String imageHash, String link, String message, String name, String picture,
             List<String> retailerItemIds, boolean useFlexibleImageAspectRatio) {
        this.callToAction = callToAction;
        this.childAttachments = childAttachments == null ? List.of() : List.copyOf(childAttachments);
        this.description = description;
        this.eventId = eventId;
        this.imageCrops = imageCrops;
        this.imageHash = imageHash;
        this.link = link;
        this.message = message;
        this.name = name;
        this.picture = picture;
        this.retailerItemIds = retailerItemIds == null ? List.of() : List.copyOf(retailerItemIds);
        this.useFlexibleImageAspectRatio = useFlexibleImageAspectRatio;
    }

    /**
     * Returns the call-to-action button of the creative.
     *
     * @return an {@link Optional} carrying the call-to-action, or empty when unset
     */
    public Optional<CallToAction> callToAction() {
        return Optional.ofNullable(callToAction);
    }

    /**
     * Returns the carousel cards of the creative.
     *
     * @return an unmodifiable view of the carousel cards; never {@code null}, possibly empty
     */
    public List<ChildAttachment> childAttachments() {
        return childAttachments;
    }

    /**
     * Returns the description shown under the creative.
     *
     * @return an {@link Optional} carrying the description, or empty when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the identifier of the linked event.
     *
     * @return an {@link Optional} carrying the event identifier, or empty when unset
     */
    public Optional<String> eventId() {
        return Optional.ofNullable(eventId);
    }

    /**
     * Returns the serialized image-crop specification.
     *
     * @return an {@link Optional} carrying the image crops, or empty when unset
     */
    public Optional<String> imageCrops() {
        return Optional.ofNullable(imageCrops);
    }

    /**
     * Returns the hash of the creative image.
     *
     * @return an {@link Optional} carrying the image hash, or empty when unset
     */
    public Optional<String> imageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * Returns the destination link the creative opens.
     *
     * @return an {@link Optional} carrying the link, or empty when unset
     */
    public Optional<String> link() {
        return Optional.ofNullable(link);
    }

    /**
     * Returns the primary text of the creative.
     *
     * @return an {@link Optional} carrying the message, or empty when unset
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns the headline name of the creative.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the picture URL of the creative.
     *
     * @return an {@link Optional} carrying the picture URL, or empty when unset
     */
    public Optional<String> picture() {
        return Optional.ofNullable(picture);
    }

    /**
     * Returns the retailer item identifiers referenced by the creative.
     *
     * @return an unmodifiable view of the retailer item identifiers; never {@code null}, possibly empty
     */
    public List<String> retailerItemIds() {
        return retailerItemIds;
    }

    /**
     * Returns whether the creative may render its image at a flexible aspect ratio.
     *
     * @return {@code true} when flexible image aspect ratio is allowed, {@code false} otherwise
     */
    public boolean useFlexibleImageAspectRatio() {
        return useFlexibleImageAspectRatio;
    }
}
