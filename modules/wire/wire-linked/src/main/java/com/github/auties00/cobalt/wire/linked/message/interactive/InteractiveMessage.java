package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.commerce.ProductMessage;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.AudioMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents an interactive message, a rich message type used by business accounts to
 * drive structured conversations.
 *
 * <p>An interactive message is composed of three presentational sections (header, body,
 * footer) and exactly one {@link InteractiveMessageContent} variant that holds the
 * structured payload. The content variant selects one of the following experiences:
 * <ul>
 *   <li>{@link ShopMessage} opens a business storefront on Facebook, Instagram or WhatsApp</li>
 *   <li>{@link CollectionMessage} links to a product collection owned by a business</li>
 *   <li>{@link NativeFlowMessage} embeds native flow buttons that trigger JSON payload
 *       actions on the client</li>
 *   <li>{@link CarouselMessage} lays out multiple interactive cards as a horizontally
 *       scrollable carousel</li>
 * </ul>
 *
 * <p>Because interactive messages are contextual, they can quote another message and carry
 * an optional {@link UrlTrackingMap} to apply consent-aware URL rewrites. They also
 * participate in the {@link TemplateFormat} hierarchy so that they can be embedded as the
 * body of a {@link TemplateMessage}.
 */
@ProtobufMessage(name = "Message.InteractiveMessage")
public final class InteractiveMessage implements TemplateFormat, ContextualMessage {
    /**
     * The optional header section displayed above the body.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    InteractiveHeader header;

    /**
     * The optional body section rendered as the main text of the message.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    Body body;

    /**
     * The optional footer section displayed below the body.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    Footer footer;

    /**
     * Contextual information such as quoted messages or mentions.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Mapping used to apply consent-aware rewrites to URLs carried by this message.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    UrlTrackingMap urlTrackingMap;

    /**
     * Shop storefront content variant.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    ShopMessage shopStorefrontMessage;

    /**
     * Product collection content variant.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    CollectionMessage collectionMessage;

    /**
     * Native flow content variant.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    NativeFlowMessage nativeFlowMessage;

    /**
     * Carousel of cards content variant.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    CarouselMessage carouselMessage;


    /**
     * Constructs a new interactive message with the supplied sections, context and content
     * variants.
     *
     * <p>At most one of {@code shopStorefrontMessage}, {@code collectionMessage},
     * {@code nativeFlowMessage} and {@code carouselMessage} should be non-null to keep the
     * oneof semantics well defined.
     *
     * @param header                the header section, possibly {@code null}
     * @param body                  the body section, possibly {@code null}
     * @param footer                the footer section, possibly {@code null}
     * @param contextInfo           contextual information, possibly {@code null}
     * @param urlTrackingMap        the URL tracking map, possibly {@code null}
     * @param shopStorefrontMessage the shop storefront variant, possibly {@code null}
     * @param collectionMessage     the collection variant, possibly {@code null}
     * @param nativeFlowMessage     the native flow variant, possibly {@code null}
     * @param carouselMessage       the carousel variant, possibly {@code null}
     */
    InteractiveMessage(InteractiveHeader header, Body body, Footer footer, ContextInfo contextInfo, UrlTrackingMap urlTrackingMap, ShopMessage shopStorefrontMessage, CollectionMessage collectionMessage, NativeFlowMessage nativeFlowMessage, CarouselMessage carouselMessage) {
        this.header = header;
        this.body = body;
        this.footer = footer;
        this.contextInfo = contextInfo;
        this.urlTrackingMap = urlTrackingMap;
        this.shopStorefrontMessage = shopStorefrontMessage;
        this.collectionMessage = collectionMessage;
        this.nativeFlowMessage = nativeFlowMessage;
        this.carouselMessage = carouselMessage;
    }

    /**
     * Returns the header section displayed above the body.
     *
     * @return an {@code Optional} with the header, or empty if not set
     */
    public Optional<InteractiveHeader> header() {
        return Optional.ofNullable(header);
    }

    /**
     * Returns the body section rendered as the main text of the message.
     *
     * @return an {@code Optional} with the body, or empty if not set
     */
    public Optional<Body> body() {
        return Optional.ofNullable(body);
    }

    /**
     * Returns the footer section displayed below the body.
     *
     * @return an {@code Optional} with the footer, or empty if not set
     */
    public Optional<Footer> footer() {
        return Optional.ofNullable(footer);
    }

    /**
     * Returns the contextual information attached to this message.
     *
     * @return an {@code Optional} with the context, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the URL tracking map used to apply consent-aware URL rewrites.
     *
     * @return an {@code Optional} with the tracking map, or empty if not set
     */
    public Optional<UrlTrackingMap> urlTrackingMap() {
        return Optional.ofNullable(urlTrackingMap);
    }

    /**
     * Returns the concrete content variant embedded in this interactive message.
     *
     * <p>Variants are checked in a fixed order: shop storefront, collection, native flow,
     * carousel. If multiple variants are present on the same instance, only the first
     * non-null one is returned.
     *
     * @return an {@code Optional} containing the content variant, or empty if none is set
     */
    public Optional<? extends InteractiveMessageContent> content() {
        if (shopStorefrontMessage != null) return Optional.of(shopStorefrontMessage);
        if (collectionMessage != null) return Optional.of(collectionMessage);
        if (nativeFlowMessage != null) return Optional.of(nativeFlowMessage);
        if (carouselMessage != null) return Optional.of(carouselMessage);
        return Optional.empty();
    }

    /**
     * Updates the header section displayed above the body.
     *
     * @param header the new header, or {@code null} to clear the field
     */
    public void setHeader(InteractiveHeader header) {
        this.header = header;
    }

    /**
     * Updates the body section of this interactive message.
     *
     * @param body the new body, or {@code null} to clear the field
     */
    public void setBody(Body body) {
        this.body = body;
    }

    /**
     * Updates the footer section of this interactive message.
     *
     * @param footer the new footer, or {@code null} to clear the field
     */
    public void setFooter(Footer footer) {
        this.footer = footer;
    }

    /**
     * Updates the contextual information attached to this message.
     *
     * @param contextInfo the new context, or {@code null} to clear the field
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Updates the URL tracking map associated with this message.
     *
     * @param urlTrackingMap the new tracking map, or {@code null} to clear the field
     */
    public void setUrlTrackingMap(UrlTrackingMap urlTrackingMap) {
        this.urlTrackingMap = urlTrackingMap;
    }

    /**
     * Sets the shop storefront content variant, clearing the other content fields is the
     * caller's responsibility.
     *
     * @param shopStorefrontMessage the new variant, or {@code null} to clear the field
     */
    public void setShopStorefrontMessage(ShopMessage shopStorefrontMessage) {
        this.shopStorefrontMessage = shopStorefrontMessage;
    }

    /**
     * Sets the collection content variant, clearing the other content fields is the caller's
     * responsibility.
     *
     * @param collectionMessage the new variant, or {@code null} to clear the field
     */
    public void setCollectionMessage(CollectionMessage collectionMessage) {
        this.collectionMessage = collectionMessage;
    }

    /**
     * Sets the native flow content variant, clearing the other content fields is the
     * caller's responsibility.
     *
     * @param nativeFlowMessage the new variant, or {@code null} to clear the field
     */
    public void setNativeFlowMessage(NativeFlowMessage nativeFlowMessage) {
        this.nativeFlowMessage = nativeFlowMessage;
    }

    /**
     * Sets the carousel content variant, clearing the other content fields is the caller's
     * responsibility.
     *
     * @param carouselMessage the new variant, or {@code null} to clear the field
     */
    public void setCarouselMessage(CarouselMessage carouselMessage) {
        this.carouselMessage = carouselMessage;
    }

    /**
     * Enumerates the media types that can be embedded in the footer of an interactive
     * message.
     *
     * <p>Currently only {@link AudioMessage} is supported, which lets businesses append a
     * voice note below the textual footer.
     */
    public sealed interface Media permits AudioMessage {
    }

    /**
     * Enumerates the media variants that can appear in the header of an interactive
     * message.
     *
     * <p>An interactive header can carry a document preview, an image, a video, a map
     * snippet, a product card, or a simple JPEG thumbnail wrapped in
     * {@link JpegThumbnail}. Only one variant is allowed per header instance.
     */
    public sealed interface MediaSpec permits DocumentMessage, ImageMessage, MediaSpec.JpegThumbnail, VideoMessage, LocationMessage, ProductMessage {

        /**
         * Wraps a raw JPEG byte array used as the header thumbnail.
         *
         * <p>This variant is serialized as a single {@code bytes} field by the protobuf
         * layer and is used when the sender only has a rendered preview image instead of a
         * full media attachment.
         */
        final class JpegThumbnail implements MediaSpec {
            /**
             * The raw JPEG-encoded thumbnail bytes.
             */
            byte[] jpegThumbnail;

            /**
             * Constructs a new JPEG thumbnail wrapper with the supplied bytes.
             *
             * @param jpegThumbnail the raw JPEG bytes
             */
            JpegThumbnail(byte[] jpegThumbnail) {
                this.jpegThumbnail = jpegThumbnail;
            }

            /**
             * Returns the raw JPEG-encoded thumbnail bytes.
             *
             * @return the thumbnail bytes
             */
            @ProtobufSerializer
            public byte[] jpegThumbnail() {
                return jpegThumbnail;
            }

            /**
             * Creates a new JPEG thumbnail wrapper from the supplied bytes.
             *
             * <p>This factory is invoked by the protobuf deserialization layer when
             * materializing an interactive header with a raw JPEG thumbnail.
             *
             * @param jpegThumbnail the raw JPEG bytes
             * @return a new wrapper around the thumbnail
             */
            @ProtobufDeserializer
            public static JpegThumbnail of(byte[] jpegThumbnail) {
                return new JpegThumbnail(jpegThumbnail);
            }
        }
    }

    /**
     * Represents the body section of an interactive message.
     *
     * <p>The body holds the main textual content shown between the header and the footer.
     */
    @ProtobufMessage(name = "Message.InteractiveMessage.Body")
    public static final class Body {
        /**
         * The literal body text.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String text;


        /**
         * Constructs a new body with the supplied text.
         *
         * @param text the body text, possibly {@code null}
         */
        Body(String text) {
            this.text = text;
        }

        /**
         * Returns the literal body text.
         *
         * @return an {@code Optional} with the text, or empty if not set
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        /**
         * Updates the body text.
         *
         * @param text the new text, or {@code null} to clear the field
         */
        public void setText(String text) {
            this.text = text;
    }
    }

    /**
     * Represents a carousel of interactive cards.
     *
     * <p>A carousel lays out multiple {@link InteractiveMessage} instances as cards that the
     * recipient can scroll horizontally. Each entry in {@link #cards()} is itself a fully
     * formed interactive message with its own header, body, footer and content variant. The
     * {@link #carouselCardType()} selects the visual layout of the individual cards.
     */
    @ProtobufMessage(name = "Message.InteractiveMessage.CarouselMessage")
    public static final class CarouselMessage implements InteractiveMessageContent {
        /**
         * The interactive messages displayed as cards in the carousel.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<InteractiveMessage> cards;

        /**
         * Protocol-level schema version of this carousel message.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
        Integer messageVersion;

        /**
         * The visual layout style used to render the cards.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
        CarouselMessage.CarouselCardType carouselCardType;


        /**
         * Constructs a new carousel message with the supplied cards and layout metadata.
         *
         * @param cards            the list of cards, possibly {@code null}
         * @param messageVersion   the protocol schema version, possibly {@code null}
         * @param carouselCardType the visual layout style, possibly {@code null}
         */
        CarouselMessage(List<InteractiveMessage> cards, Integer messageVersion, CarouselCardType carouselCardType) {
            this.cards = cards;
            this.messageVersion = messageVersion;
            this.carouselCardType = carouselCardType;
        }

        /**
         * Returns an unmodifiable view of the interactive messages displayed as cards.
         *
         * @return the cards, or an empty list if none were set
         */
        public List<InteractiveMessage> cards() {
            return cards == null ? List.of() : Collections.unmodifiableList(cards);
        }

        /**
         * Returns the protocol-level schema version of this carousel message.
         *
         * @return an {@code OptionalInt} with the version, or empty if not set
         */
        public OptionalInt messageVersion() {
            return messageVersion == null ? OptionalInt.empty() : OptionalInt.of(messageVersion);
        }

        /**
         * Returns the visual layout style used to render the cards.
         *
         * @return an {@code Optional} with the layout style, or empty if not set
         */
        public Optional<CarouselCardType> carouselCardType() {
            return Optional.ofNullable(carouselCardType);
        }

        /**
         * Updates the list of cards displayed in the carousel.
         *
         * @param cards the new list, or {@code null} to clear the field
         */
        public void setCards(List<InteractiveMessage> cards) {
            this.cards = cards;
    }

        /**
         * Updates the protocol schema version of this carousel message.
         *
         * @param messageVersion the new version, or {@code null} to clear the field
         */
        public void setMessageVersion(Integer messageVersion) {
            this.messageVersion = messageVersion;
    }

        /**
         * Updates the visual layout style used to render the cards.
         *
         * @param carouselCardType the new style, or {@code null} to clear the field
         */
        public void setCarouselCardType(CarouselCardType carouselCardType) {
            this.carouselCardType = carouselCardType;
    }

        /**
         * Enumerates the visual layouts supported for the cards of a carousel.
         */
        @ProtobufEnum(name = "Message.InteractiveMessage.CarouselMessage.CarouselCardType")
        public static enum CarouselCardType {
            /**
             * Unspecified layout, used when the sender did not declare a preference.
             */
            UNKNOWN(0),
            /**
             * Horizontally scrollable row of cards.
             */
            HSCROLL_CARDS(1),
            /**
             * Album-style image grid layout.
             */
            ALBUM_IMAGE(2);

            /**
             * Constructs a new enum constant with the supplied protobuf index.
             *
             * @param index the numeric wire-format index
             */
            CarouselCardType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The numeric wire-format index of this constant.
             */
            final int index;

            /**
             * Returns the numeric wire-format index of this constant.
             *
             * @return the protobuf enum index
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Represents a reference to a product collection published by a WhatsApp business
     * account.
     *
     * <p>Tapping this content variant opens the collection page owned by the business
     * identified by {@link #bizJid()}, filtered by the supplied {@link #id()}.
     */
    @ProtobufMessage(name = "Message.InteractiveMessage.CollectionMessage")
    public static final class CollectionMessage implements InteractiveMessageContent {
        /**
         * The JID of the business that owns the referenced collection.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid bizJid;

        /**
         * The identifier of the referenced collection.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id;

        /**
         * Protocol-level schema version of this collection reference.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        Integer messageVersion;


        /**
         * Constructs a new collection reference with the supplied business JID, collection
         * identifier and schema version.
         *
         * @param bizJid         the business JID, possibly {@code null}
         * @param id             the collection identifier, possibly {@code null}
         * @param messageVersion the schema version, possibly {@code null}
         */
        CollectionMessage(Jid bizJid, String id, Integer messageVersion) {
            this.bizJid = bizJid;
            this.id = id;
            this.messageVersion = messageVersion;
        }

        /**
         * Returns the JID of the business that owns the referenced collection.
         *
         * @return an {@code Optional} with the business JID, or empty if not set
         */
        public Optional<Jid> bizJid() {
            return Optional.ofNullable(bizJid);
        }

        /**
         * Returns the identifier of the referenced collection.
         *
         * @return an {@code Optional} with the collection identifier, or empty if not set
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the protocol-level schema version of this collection reference.
         *
         * @return an {@code OptionalInt} with the version, or empty if not set
         */
        public OptionalInt messageVersion() {
            return messageVersion == null ? OptionalInt.empty() : OptionalInt.of(messageVersion);
        }

        /**
         * Updates the JID of the business that owns the referenced collection.
         *
         * @param bizJid the new business JID, or {@code null} to clear the field
         */
        public void setBizJid(Jid bizJid) {
            this.bizJid = bizJid;
    }

        /**
         * Updates the identifier of the referenced collection.
         *
         * @param id the new collection identifier, or {@code null} to clear the field
         */
        public void setId(String id) {
            this.id = id;
    }

        /**
         * Updates the protocol schema version of this collection reference.
         *
         * @param messageVersion the new version, or {@code null} to clear the field
         */
        public void setMessageVersion(Integer messageVersion) {
            this.messageVersion = messageVersion;
    }
    }

    /**
     * Represents the footer section of an interactive message.
     *
     * <p>The footer combines a small piece of text with an optional media attachment such as
     * a voice note. The {@link #hasMediaAttachment()} flag advertises whether the footer
     * actually ships with media, even when the wire-level field is omitted.
     */
    @ProtobufMessage(name = "Message.InteractiveMessage.Footer")
    public static final class Footer {
        /**
         * The literal footer text.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String text;

        /**
         * Flag advertising that the footer ships with a media attachment.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        Boolean hasMediaAttachment;

        /**
         * The audio message attached to the footer, when present.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        AudioMessage audioMessage;


        /**
         * Constructs a new footer with the supplied text, attachment flag and audio media.
         *
         * @param text               the footer text, possibly {@code null}
         * @param hasMediaAttachment the media attachment flag, possibly {@code null}
         * @param audioMessage       the audio attachment, possibly {@code null}
         */
        Footer(String text, Boolean hasMediaAttachment, AudioMessage audioMessage) {
            this.text = text;
            this.hasMediaAttachment = hasMediaAttachment;
            this.audioMessage = audioMessage;
        }

        /**
         * Returns the literal footer text.
         *
         * @return an {@code Optional} with the text, or empty if not set
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        /**
         * Returns whether this footer advertises a media attachment.
         *
         * <p>A missing value is treated as {@code false}.
         *
         * @return {@code true} if the footer ships with media, {@code false} otherwise
         */
        public boolean hasMediaAttachment() {
            return hasMediaAttachment != null && hasMediaAttachment;
        }

        /**
         * Returns the media attachment embedded in the footer.
         *
         * <p>Only audio attachments are currently supported.
         *
         * @return an {@code Optional} with the media attachment, or empty if none is set
         */
        public Optional<? extends Media> media() {
            if (audioMessage != null) return Optional.of(audioMessage);
            return Optional.empty();
        }

        /**
         * Updates the footer text.
         *
         * @param text the new text, or {@code null} to clear the field
         */
        public void setText(String text) {
            this.text = text;
    }

        /**
         * Updates the media attachment flag.
         *
         * @param hasMediaAttachment the new flag value, or {@code null} to clear the field
         */
        public void setHasMediaAttachment(Boolean hasMediaAttachment) {
            this.hasMediaAttachment = hasMediaAttachment;
    }

        /**
         * Updates the audio message attached to the footer.
         *
         * @param audioMessage the new attachment, or {@code null} to clear the field
         */
        public void setAudioMessage(AudioMessage audioMessage) {
            this.audioMessage = audioMessage;
    }
    }

    /**
     * Represents the header section of an interactive message.
     *
     * <p>The header combines a short title and subtitle with an optional rich media preview.
     * The supported media previews include documents, images, videos, map snippets, product
     * cards and raw JPEG thumbnails. The {@link #hasMediaAttachment()} flag advertises the
     * presence of media, even when the concrete media field is omitted from the wire.
     */
    @ProtobufMessage(name = "Message.InteractiveMessage.Header")
    public static final class InteractiveHeader {
        /**
         * The header title.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title;

        /**
         * The header subtitle.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String subtitle;

        /**
         * Flag advertising that the header ships with a media attachment.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
        Boolean hasMediaAttachment;

        /**
         * Document media variant.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        DocumentMessage documentMessage;

        /**
         * Image media variant.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        ImageMessage imageMessage;

        /**
         * Raw JPEG thumbnail bytes variant.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
        byte[] jpegThumbnail;

        /**
         * Video media variant.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
        VideoMessage videoMessage;

        /**
         * Location media variant.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        LocationMessage locationMessage;

        /**
         * Product media variant.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
        ProductMessage productMessage;


        /**
         * Constructs a new header with the supplied title, subtitle, attachment flag and
         * media variants.
         *
         * <p>At most one of the media variants should be non-null to keep the oneof
         * semantics well defined.
         *
         * @param title              the title, possibly {@code null}
         * @param subtitle           the subtitle, possibly {@code null}
         * @param hasMediaAttachment the media attachment flag, possibly {@code null}
         * @param documentMessage    the document variant, possibly {@code null}
         * @param imageMessage       the image variant, possibly {@code null}
         * @param jpegThumbnail      the raw JPEG thumbnail variant, possibly {@code null}
         * @param videoMessage       the video variant, possibly {@code null}
         * @param locationMessage    the location variant, possibly {@code null}
         * @param productMessage     the product variant, possibly {@code null}
         */
        InteractiveHeader(String title, String subtitle, Boolean hasMediaAttachment, DocumentMessage documentMessage, ImageMessage imageMessage, byte[] jpegThumbnail, VideoMessage videoMessage, LocationMessage locationMessage, ProductMessage productMessage) {
            this.title = title;
            this.subtitle = subtitle;
            this.hasMediaAttachment = hasMediaAttachment;
            this.documentMessage = documentMessage;
            this.imageMessage = imageMessage;
            this.jpegThumbnail = jpegThumbnail;
            this.videoMessage = videoMessage;
            this.locationMessage = locationMessage;
            this.productMessage = productMessage;
        }

        /**
         * Returns the header title.
         *
         * @return an {@code Optional} with the title, or empty if not set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the header subtitle.
         *
         * @return an {@code Optional} with the subtitle, or empty if not set
         */
        public Optional<String> subtitle() {
            return Optional.ofNullable(subtitle);
        }

        /**
         * Returns whether this header advertises a media attachment.
         *
         * <p>A missing value is treated as {@code false}.
         *
         * @return {@code true} if the header ships with media, {@code false} otherwise
         */
        public boolean hasMediaAttachment() {
            return hasMediaAttachment != null && hasMediaAttachment;
        }

        /**
         * Returns the media variant attached to the header.
         *
         * <p>Variants are checked in a fixed order: document, image, JPEG thumbnail, video,
         * location, product. If multiple variants are present on the same instance, only the
         * first non-null one is returned. The raw JPEG bytes are wrapped into a dedicated
         * {@link MediaSpec.JpegThumbnail} instance on the fly.
         *
         * @return an {@code Optional} containing the media variant, or empty if none is set
         */
        public Optional<? extends MediaSpec> media() {
            if (documentMessage != null) return Optional.of(documentMessage);
            if (imageMessage != null) return Optional.of(imageMessage);
            if (jpegThumbnail != null) return Optional.of(MediaSpec.JpegThumbnail.of(jpegThumbnail));
            if (videoMessage != null) return Optional.of(videoMessage);
            if (locationMessage != null) return Optional.of(locationMessage);
            if (productMessage != null) return Optional.of(productMessage);
            return Optional.empty();
        }

        /**
         * Updates the header title.
         *
         * @param title the new title, or {@code null} to clear the field
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Updates the header subtitle.
         *
         * @param subtitle the new subtitle, or {@code null} to clear the field
         */
        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
    }

        /**
         * Updates the media attachment flag.
         *
         * @param hasMediaAttachment the new flag value, or {@code null} to clear the field
         */
        public void setHasMediaAttachment(Boolean hasMediaAttachment) {
            this.hasMediaAttachment = hasMediaAttachment;
    }

        /**
         * Sets the document media variant, clearing the other media fields is the caller's
         * responsibility.
         *
         * @param documentMessage the new variant, or {@code null} to clear the field
         */
        public void setDocumentMessage(DocumentMessage documentMessage) {
            this.documentMessage = documentMessage;
    }

        /**
         * Sets the image media variant, clearing the other media fields is the caller's
         * responsibility.
         *
         * @param imageMessage the new variant, or {@code null} to clear the field
         */
        public void setImageMessage(ImageMessage imageMessage) {
            this.imageMessage = imageMessage;
    }

        /**
         * Sets the raw JPEG thumbnail bytes, clearing the other media fields is the caller's
         * responsibility.
         *
         * @param jpegThumbnail the new bytes, or {@code null} to clear the field
         */
        public void setJpegThumbnail(byte[] jpegThumbnail) {
            this.jpegThumbnail = jpegThumbnail;
    }

        /**
         * Sets the video media variant, clearing the other media fields is the caller's
         * responsibility.
         *
         * @param videoMessage the new variant, or {@code null} to clear the field
         */
        public void setVideoMessage(VideoMessage videoMessage) {
            this.videoMessage = videoMessage;
    }

        /**
         * Sets the location media variant, clearing the other media fields is the caller's
         * responsibility.
         *
         * @param locationMessage the new variant, or {@code null} to clear the field
         */
        public void setLocationMessage(LocationMessage locationMessage) {
            this.locationMessage = locationMessage;
    }

        /**
         * Sets the product media variant, clearing the other media fields is the caller's
         * responsibility.
         *
         * @param productMessage the new variant, or {@code null} to clear the field
         */
        public void setProductMessage(ProductMessage productMessage) {
            this.productMessage = productMessage;
    }
    }

    /**
     * Represents a native flow content variant.
     *
     * <p>Native flows embed a set of {@link NativeFlowButton} entries that, when tapped by
     * the recipient, trigger client-side actions described by opaque JSON parameters.
     * {@link #messageParamsJson()} carries shared parameters at the message level, while
     * each button stores its own per-action parameters in
     * {@link NativeFlowButton#buttonParamsJson()}.
     */
    @ProtobufMessage(name = "Message.InteractiveMessage.NativeFlowMessage")
    public static final class NativeFlowMessage implements InteractiveMessageContent {
        /**
         * The native flow buttons offered to the recipient.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<NativeFlowButton> buttons;

        /**
         * JSON string carrying message-level native flow parameters.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String messageParamsJson;

        /**
         * Protocol-level schema version of this native flow message.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        Integer messageVersion;


        /**
         * Constructs a new native flow content with the supplied buttons, parameters and
         * schema version.
         *
         * @param buttons           the list of flow buttons, possibly {@code null}
         * @param messageParamsJson the JSON-encoded message parameters, possibly {@code null}
         * @param messageVersion    the schema version, possibly {@code null}
         */
        NativeFlowMessage(List<NativeFlowButton> buttons, String messageParamsJson, Integer messageVersion) {
            this.buttons = buttons;
            this.messageParamsJson = messageParamsJson;
            this.messageVersion = messageVersion;
        }

        /**
         * Returns an unmodifiable view of the native flow buttons offered to the recipient.
         *
         * @return the list of buttons, or an empty list if none were set
         */
        public List<NativeFlowButton> buttons() {
            return buttons == null ? List.of() : Collections.unmodifiableList(buttons);
        }

        /**
         * Returns the JSON-encoded message-level native flow parameters.
         *
         * @return an {@code Optional} with the JSON payload, or empty if not set
         */
        public Optional<String> messageParamsJson() {
            return Optional.ofNullable(messageParamsJson);
        }

        /**
         * Returns the protocol-level schema version of this native flow message.
         *
         * @return an {@code OptionalInt} with the version, or empty if not set
         */
        public OptionalInt messageVersion() {
            return messageVersion == null ? OptionalInt.empty() : OptionalInt.of(messageVersion);
        }

        /**
         * Updates the list of native flow buttons offered to the recipient.
         *
         * @param buttons the new list, or {@code null} to clear the field
         */
        public void setButtons(List<NativeFlowButton> buttons) {
            this.buttons = buttons;
    }

        /**
         * Updates the JSON-encoded message-level native flow parameters.
         *
         * @param messageParamsJson the new JSON payload, or {@code null} to clear the field
         */
        public void setMessageParamsJson(String messageParamsJson) {
            this.messageParamsJson = messageParamsJson;
    }

        /**
         * Updates the protocol schema version of this native flow message.
         *
         * @param messageVersion the new version, or {@code null} to clear the field
         */
        public void setMessageVersion(Integer messageVersion) {
            this.messageVersion = messageVersion;
    }

        /**
         * Represents a single button inside a {@link NativeFlowMessage}.
         *
         * <p>Each button has a machine-readable {@link #name()} and an optional
         * {@link #buttonParamsJson()} describing the parameters that will be evaluated when
         * the recipient taps the button.
         */
        @ProtobufMessage(name = "Message.InteractiveMessage.NativeFlowMessage.NativeFlowButton")
        public static final class NativeFlowButton {
            /**
             * The machine-readable name of the button.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String name;

            /**
             * JSON string carrying the parameters evaluated when the button is tapped.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String buttonParamsJson;


            /**
             * Constructs a new native flow button with the supplied name and JSON parameters.
             *
             * @param name             the button name, possibly {@code null}
             * @param buttonParamsJson the JSON-encoded parameters, possibly {@code null}
             */
            NativeFlowButton(String name, String buttonParamsJson) {
                this.name = name;
                this.buttonParamsJson = buttonParamsJson;
            }

            /**
             * Returns the machine-readable name of this button.
             *
             * @return an {@code Optional} with the name, or empty if not set
             */
            public Optional<String> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the JSON-encoded parameters evaluated when the button is tapped.
             *
             * @return an {@code Optional} with the JSON payload, or empty if not set
             */
            public Optional<String> buttonParamsJson() {
                return Optional.ofNullable(buttonParamsJson);
            }

            /**
             * Updates the machine-readable name of this button.
             *
             * @param name the new name, or {@code null} to clear the field
             */
            public void setName(String name) {
                this.name = name;
    }

            /**
             * Updates the JSON-encoded parameters evaluated when the button is tapped.
             *
             * @param buttonParamsJson the new JSON payload, or {@code null} to clear the
             *                         field
             */
            public void setButtonParamsJson(String buttonParamsJson) {
                this.buttonParamsJson = buttonParamsJson;
    }
        }
    }

    /**
     * Represents a shop storefront content variant.
     *
     * <p>Tapping this content variant opens the business storefront on the surface selected
     * by {@link #surface()}, identified by {@link #id()}. The storefront shows the business
     * catalog in the surface's native shopping experience.
     */
    @ProtobufMessage(name = "Message.InteractiveMessage.ShopMessage")
    public static final class ShopMessage implements InteractiveMessageContent {
        /**
         * The identifier of the referenced storefront.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id;

        /**
         * The platform surface hosting the storefront.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        ShopMessage.Surface surface;

        /**
         * Protocol-level schema version of this shop reference.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        Integer messageVersion;


        /**
         * Constructs a new shop reference with the supplied identifier, surface and schema
         * version.
         *
         * @param id             the storefront identifier, possibly {@code null}
         * @param surface        the platform surface, possibly {@code null}
         * @param messageVersion the schema version, possibly {@code null}
         */
        ShopMessage(String id, Surface surface, Integer messageVersion) {
            this.id = id;
            this.surface = surface;
            this.messageVersion = messageVersion;
        }

        /**
         * Returns the identifier of the referenced storefront.
         *
         * @return an {@code Optional} with the identifier, or empty if not set
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the platform surface hosting the storefront.
         *
         * @return an {@code Optional} with the surface, or empty if not set
         */
        public Optional<Surface> surface() {
            return Optional.ofNullable(surface);
        }

        /**
         * Returns the protocol-level schema version of this shop reference.
         *
         * @return an {@code OptionalInt} with the version, or empty if not set
         */
        public OptionalInt messageVersion() {
            return messageVersion == null ? OptionalInt.empty() : OptionalInt.of(messageVersion);
        }

        /**
         * Updates the identifier of the referenced storefront.
         *
         * @param id the new identifier, or {@code null} to clear the field
         */
        public void setId(String id) {
            this.id = id;
    }

        /**
         * Updates the platform surface hosting the storefront.
         *
         * @param surface the new surface, or {@code null} to clear the field
         */
        public void setSurface(Surface surface) {
            this.surface = surface;
    }

        /**
         * Updates the protocol schema version of this shop reference.
         *
         * @param messageVersion the new version, or {@code null} to clear the field
         */
        public void setMessageVersion(Integer messageVersion) {
            this.messageVersion = messageVersion;
    }

        /**
         * Enumerates the platform surfaces that can host a shop storefront referenced by a
         * {@link ShopMessage}.
         *
         * <p>The chosen surface determines which Meta app opens the storefront when the
         * recipient taps the message.
         */
        @ProtobufEnum(name = "Message.InteractiveMessage.ShopMessage.Surface")
        public static enum Surface {
            /**
             * Unspecified surface, used when no preference has been declared.
             */
            UNKNOWN_SURFACE(0),
            /**
             * The storefront is hosted on Facebook.
             */
            FB(1),
            /**
             * The storefront is hosted on Instagram.
             */
            IG(2),
            /**
             * The storefront is hosted on WhatsApp.
             */
            WA(3);

            /**
             * Constructs a new enum constant with the supplied protobuf index.
             *
             * @param index the numeric wire-format index
             */
            Surface(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The numeric wire-format index of this constant.
             */
            final int index;

            /**
             * Returns the numeric wire-format index of this constant.
             *
             * @return the protobuf enum index
             */
            public int index() {
                return this.index;
            }
        }
    }
}
