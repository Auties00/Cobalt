package com.github.auties00.cobalt.wire.linked.message.text;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.media.EmbeddedMusic;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentExtendedMetadata;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentLinkMetadata;
import com.github.auties00.cobalt.wire.linked.message.media.MessageLinkPreviewMetadata;
import com.github.auties00.cobalt.wire.linked.message.media.MessageMMSThumbnailMetadata;
import com.github.auties00.cobalt.wire.linked.message.media.MessageVideoEndCard;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A text message enriched with additional rendering or metadata features
 * beyond a plain conversation string.
 *
 * <p>This type is the message body used whenever a text message carries
 * any of the following:
 * <ul>
 *   <li>a link with its preview (title, description, thumbnail, URL)</li>
 *   <li>a coloured or styled text bubble (background colour, text colour,
 *       custom font)</li>
 *   <li>a group invite link with its resolved group metadata</li>
 *   <li>a payment or video link with their extended metadata</li>
 *   <li>a music card, video end-card tiles, or a favicon thumbnail</li>
 * </ul>
 *
 * <p>When none of these features are used, a plain {@code String}
 * conversation body is sent instead. Extended text messages are
 * contextual, meaning they may carry a {@link ContextInfo} describing
 * a quoted message, forwarding score, mentions, and related metadata.
 */
@ProtobufMessage(name = "Message.ExtendedTextMessage")
public final class ExtendedTextMessage implements ContextualMessage, MediaProvider {
    /**
     * The raw text content shown in the message bubble.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String text;

    /**
     * The exact substring of {@link #text} detected as a link and used to
     * fetch the attached preview metadata.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String matchedText;

    /**
     * The description field of the attached link preview, typically taken
     * from the target page's Open Graph metadata.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String description;

    /**
     * The title field of the attached link preview, typically taken from
     * the target page's Open Graph metadata.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String title;

    /**
     * The text foreground colour encoded as an ARGB integer, used when
     * the bubble is rendered as a styled text card.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.FIXED32)
    Integer textArgb;

    /**
     * The bubble background colour encoded as an ARGB integer, used when
     * the bubble is rendered as a styled text card.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.FIXED32)
    Integer backgroundArgb;

    /**
     * The font family applied to the rendered text.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    FontType font;

    /**
     * The kind of preview attached to the message, distinguishing between
     * image previews, video previews, profile previews and others.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    PreviewType previewType;

    /**
     * The low-resolution JPEG thumbnail displayed inline while the
     * full-resolution preview media is being fetched.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    byte[] jpegThumbnail;

    /**
     * Contextual metadata describing the quoted message, mentions,
     * forwarding score and related flags.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Flag requesting that the attached video preview not be auto-played
     * inline inside the chat bubble.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    Boolean doNotPlayInline;

    /**
     * The direct CDN path from which the encrypted thumbnail can be
     * downloaded.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    String thumbnailDirectPath;

    /**
     * The SHA-256 digest of the plaintext thumbnail bytes, used to verify
     * integrity after decryption.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BYTES)
    byte[] thumbnailSha256;

    /**
     * The SHA-256 digest of the encrypted thumbnail bytes, used to verify
     * integrity of the downloaded ciphertext.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    byte[] thumbnailEncSha256;

    /**
     * The symmetric media key used to decrypt the attached thumbnail.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * The moment the {@link #mediaKey} was generated, used as part of
     * media key expiry tracking.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * The height of the attached thumbnail in pixels.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.UINT32)
    Integer thumbnailHeight;

    /**
     * The width of the attached thumbnail in pixels.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.UINT32)
    Integer thumbnailWidth;

    /**
     * The type of group invite link attached, distinguishing between
     * default, parent and sub-group links.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.ENUM)
    InviteLinkGroupType inviteLinkGroupType;

    /**
     * The subject of the parent community group referenced by the invite
     * link, when the link points to a sub-group.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.STRING)
    String inviteLinkParentGroupSubjectV2;

    /**
     * The thumbnail image of the parent community group referenced by the
     * invite link, when the link points to a sub-group.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BYTES)
    byte[] inviteLinkParentGroupThumbnailV2;

    /**
     * The V2 version of the group invite type classification, superseding
     * {@link #inviteLinkGroupType}.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.ENUM)
    InviteLinkGroupType inviteLinkGroupTypeV2;

    /**
     * Flag indicating that the attached preview should be shown in
     * view-once mode and discarded after a single open.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    Boolean viewOnce;

    /**
     * The height in pixels of the attached video preview, if any.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.UINT32)
    Integer videoHeight;

    /**
     * The width in pixels of the attached video preview, if any.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.UINT32)
    Integer videoWidth;

    /**
     * Metadata for the link's favicon image, bundled via WhatsApp's MMS
     * thumbnail delivery system.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.MESSAGE)
    MessageMMSThumbnailMetadata faviconMMSMetadata;

    /**
     * Extended link-preview metadata such as the canonical URL and
     * preview-specific extensions.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.MESSAGE)
    MessageLinkPreviewMetadata linkPreviewMetadata;

    /**
     * Payment-specific metadata for links that resolve to a WhatsApp
     * Payments flow.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.MESSAGE)
    PaymentLinkMetadata paymentLinkMetadata;

    /**
     * The list of end-card tiles shown after a video preview finishes
     * playing, each pointing to another piece of content.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.MESSAGE)
    List<MessageVideoEndCard> endCardTiles;

    /**
     * The canonical video URL for the attached preview, used to open the
     * video source.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    String videoContentUrl;

    /**
     * Metadata describing an embedded music clip, such as title, artist
     * and artwork, when the link resolves to a music track.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    EmbeddedMusic musicMetadata;

    /**
     * Extra payment-related metadata providing details beyond
     * {@link #paymentLinkMetadata}.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.MESSAGE)
    PaymentExtendedMetadata paymentExtendedMetadata;


    /**
     * Constructs a new extended text message populated with every supported
     * field. Intended for use by the generated protobuf deserializer and
     * builder; callers should prefer the generated builder.
     *
     * @param text the message text
     * @param matchedText the detected link substring inside {@code text}
     * @param description the description of the attached link preview
     * @param title the title of the attached link preview
     * @param textArgb the text colour as an ARGB integer
     * @param backgroundArgb the bubble background colour as an ARGB integer
     * @param font the font type applied to the text
     * @param previewType the kind of preview attached
     * @param jpegThumbnail the low-resolution inline thumbnail bytes
     * @param contextInfo the contextual metadata
     * @param doNotPlayInline flag requesting inline playback be disabled
     * @param thumbnailDirectPath the CDN path for the encrypted thumbnail
     * @param thumbnailSha256 the digest of the plaintext thumbnail
     * @param thumbnailEncSha256 the digest of the encrypted thumbnail
     * @param mediaKey the symmetric key used to decrypt the thumbnail
     * @param mediaKeyTimestamp the moment the media key was generated
     * @param thumbnailHeight the thumbnail height in pixels
     * @param thumbnailWidth the thumbnail width in pixels
     * @param inviteLinkGroupType the legacy invite group classification
     * @param inviteLinkParentGroupSubjectV2 the parent community subject
     * @param inviteLinkParentGroupThumbnailV2 the parent community thumbnail
     * @param inviteLinkGroupTypeV2 the V2 invite group classification
     * @param viewOnce flag marking the preview as view-once
     * @param videoHeight the video preview height in pixels
     * @param videoWidth the video preview width in pixels
     * @param faviconMMSMetadata the favicon MMS metadata
     * @param linkPreviewMetadata the extended link preview metadata
     * @param paymentLinkMetadata the payment link metadata
     * @param endCardTiles the list of video end-card tiles
     * @param videoContentUrl the canonical URL of the attached video
     * @param musicMetadata the embedded music metadata
     * @param paymentExtendedMetadata extra payment-related metadata
     */
    ExtendedTextMessage(String text, String matchedText, String description, String title, Integer textArgb, Integer backgroundArgb, FontType font, PreviewType previewType, byte[] jpegThumbnail, ContextInfo contextInfo, Boolean doNotPlayInline, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, byte[] mediaKey, Instant mediaKeyTimestamp, Integer thumbnailHeight, Integer thumbnailWidth, InviteLinkGroupType inviteLinkGroupType, String inviteLinkParentGroupSubjectV2, byte[] inviteLinkParentGroupThumbnailV2, InviteLinkGroupType inviteLinkGroupTypeV2, Boolean viewOnce, Integer videoHeight, Integer videoWidth, MessageMMSThumbnailMetadata faviconMMSMetadata, MessageLinkPreviewMetadata linkPreviewMetadata, PaymentLinkMetadata paymentLinkMetadata, List<MessageVideoEndCard> endCardTiles, String videoContentUrl, EmbeddedMusic musicMetadata, PaymentExtendedMetadata paymentExtendedMetadata) {
        this.text = text;
        this.matchedText = matchedText;
        this.description = description;
        this.title = title;
        this.textArgb = textArgb;
        this.backgroundArgb = backgroundArgb;
        this.font = font;
        this.previewType = previewType;
        this.jpegThumbnail = jpegThumbnail;
        this.contextInfo = contextInfo;
        this.doNotPlayInline = doNotPlayInline;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.mediaKey = mediaKey;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailWidth = thumbnailWidth;
        this.inviteLinkGroupType = inviteLinkGroupType;
        this.inviteLinkParentGroupSubjectV2 = inviteLinkParentGroupSubjectV2;
        this.inviteLinkParentGroupThumbnailV2 = inviteLinkParentGroupThumbnailV2;
        this.inviteLinkGroupTypeV2 = inviteLinkGroupTypeV2;
        this.viewOnce = viewOnce;
        this.videoHeight = videoHeight;
        this.videoWidth = videoWidth;
        this.faviconMMSMetadata = faviconMMSMetadata;
        this.linkPreviewMetadata = linkPreviewMetadata;
        this.paymentLinkMetadata = paymentLinkMetadata;
        this.endCardTiles = endCardTiles;
        this.videoContentUrl = videoContentUrl;
        this.musicMetadata = musicMetadata;
        this.paymentExtendedMetadata = paymentExtendedMetadata;
    }

    /**
     * Returns the raw text content of the message, if set.
     *
     * @return an {@link Optional} containing the text body,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the link substring of {@link #text} that was matched and
     * used to generate the preview, if any.
     *
     * @return an {@link Optional} containing the matched link text,
     *         or {@link Optional#empty()} if no link was detected
     */
    public Optional<String> matchedText() {
        return Optional.ofNullable(matchedText);
    }

    /**
     * Returns the attached link preview description, if any.
     *
     * @return an {@link Optional} containing the description,
     *         or {@link Optional#empty()} if no preview is attached
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the attached link preview title, if any.
     *
     * @return an {@link Optional} containing the title,
     *         or {@link Optional#empty()} if no preview is attached
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the text colour as a packed ARGB integer, if set.
     *
     * @return an {@link OptionalInt} holding the ARGB value,
     *         or {@link OptionalInt#empty()} if no styled colour applies
     */
    public OptionalInt textArgb() {
        return textArgb == null ? OptionalInt.empty() : OptionalInt.of(textArgb);
    }

    /**
     * Returns the bubble background colour as a packed ARGB integer, if set.
     *
     * @return an {@link OptionalInt} holding the ARGB value,
     *         or {@link OptionalInt#empty()} if no styled background applies
     */
    public OptionalInt backgroundArgb() {
        return backgroundArgb == null ? OptionalInt.empty() : OptionalInt.of(backgroundArgb);
    }

    /**
     * Returns the font applied to the rendered text, if set.
     *
     * @return an {@link Optional} containing the {@link FontType},
     *         or {@link Optional#empty()} if the default font applies
     */
    public Optional<FontType> font() {
        return Optional.ofNullable(font);
    }

    /**
     * Returns the kind of preview attached, if any.
     *
     * @return an {@link Optional} containing the {@link PreviewType},
     *         or {@link Optional#empty()} if no preview is attached
     */
    public Optional<PreviewType> previewType() {
        return Optional.ofNullable(previewType);
    }

    /**
     * Returns the inline low-resolution JPEG thumbnail bytes, if any.
     *
     * @return an {@link Optional} containing the thumbnail bytes,
     *         or {@link Optional#empty()} if none is set
     */
    public Optional<byte[]> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }

    /**
     * Returns the contextual metadata (quoted message, mentions,
     * forwarding score, etc.) attached to the message.
     *
     * @return an {@link Optional} containing the {@link ContextInfo},
     *         or {@link Optional#empty()} if no context applies
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns whether the attached preview should avoid inline autoplay.
     *
     * @return {@code true} if inline autoplay is disabled, {@code false} otherwise
     */
    public boolean doNotPlayInline() {
        return doNotPlayInline != null && doNotPlayInline;
    }

    /**
     * Returns the CDN direct path used to download the encrypted
     * thumbnail, if set.
     *
     * @return an {@link Optional} containing the direct path,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> thumbnailDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    /**
     * Returns the SHA-256 digest of the plaintext thumbnail, if set.
     *
     * @return an {@link Optional} containing the digest,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<byte[]> thumbnailSha256() {
        return Optional.ofNullable(thumbnailSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted thumbnail, if set.
     *
     * @return an {@link Optional} containing the digest,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<byte[]> thumbnailEncSha256() {
        return Optional.ofNullable(thumbnailEncSha256);
    }

    /**
     * Returns the symmetric media key used to decrypt the thumbnail, if set.
     *
     * @return an {@link Optional} containing the media key bytes,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the moment the media key was generated, if set.
     *
     * @return an {@link Optional} containing the {@link Instant},
     *         or {@link Optional#empty()} if unset
     */
    public Optional<Instant> mediaKeyTimestamp() {
        return Optional.ofNullable(mediaKeyTimestamp);
    }

    /**
     * Returns the thumbnail height in pixels, if set.
     *
     * @return an {@link OptionalInt} containing the height,
     *         or {@link OptionalInt#empty()} if unset
     */
    public OptionalInt thumbnailHeight() {
        return thumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbnailHeight);
    }

    /**
     * Returns the thumbnail width in pixels, if set.
     *
     * @return an {@link OptionalInt} containing the width,
     *         or {@link OptionalInt#empty()} if unset
     */
    public OptionalInt thumbnailWidth() {
        return thumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbnailWidth);
    }

    /**
     * Returns {@link Optional#empty()}; link previews do not carry a CDN
     * URL on the wire, only the direct path.
     *
     * @return always {@link Optional#empty()}
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.empty();
    }

    /**
     * No-op; link previews do not carry a CDN URL on the wire.
     *
     * @param mediaUrl ignored
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
        // link previews carry no media URL on the wire
    }

    /**
     * Returns the CDN direct path of the encrypted HQ thumbnail, bridging
     * to {@link #thumbnailDirectPath()} for the {@link MediaProvider}
     * contract.
     *
     * @return an {@link Optional} containing the direct path,
     *         or {@link Optional#empty()} if unset
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    /**
     * Sets the CDN direct path of the encrypted HQ thumbnail, bridging to
     * {@link #setThumbnailDirectPath(String)} for the
     * {@link MediaProvider} contract.
     *
     * @param mediaDirectPath the direct path, or {@code null} to clear
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.thumbnailDirectPath = mediaDirectPath;
    }

    /**
     * Returns the SHA-256 digest of the plaintext HQ thumbnail, bridging
     * to {@link #thumbnailSha256()} for the {@link MediaProvider}
     * contract.
     *
     * @return an {@link Optional} containing the digest,
     *         or {@link Optional#empty()} if unset
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(thumbnailSha256);
    }

    /**
     * Sets the SHA-256 digest of the plaintext HQ thumbnail, bridging to
     * {@link #setThumbnailSha256(byte[])} for the {@link MediaProvider}
     * contract.
     *
     * @param bytes the digest, or {@code null} to clear
     */
    @Override
    public void setMediaSha256(byte[] bytes) {
        this.thumbnailSha256 = bytes;
    }

    /**
     * Returns the SHA-256 digest of the encrypted HQ thumbnail, bridging
     * to {@link #thumbnailEncSha256()} for the {@link MediaProvider}
     * contract.
     *
     * @return an {@link Optional} containing the digest,
     *         or {@link Optional#empty()} if unset
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(thumbnailEncSha256);
    }

    /**
     * Sets the SHA-256 digest of the encrypted HQ thumbnail, bridging to
     * {@link #setThumbnailEncSha256(byte[])} for the
     * {@link MediaProvider} contract.
     *
     * @param bytes the digest, or {@code null} to clear
     */
    @Override
    public void setMediaEncryptedSha256(byte[] bytes) {
        this.thumbnailEncSha256 = bytes;
    }

    /**
     * Returns {@link OptionalLong#empty()}; link previews do not carry a
     * media-size field on the wire.
     *
     * @return always {@link OptionalLong#empty()}
     */
    @Override
    public OptionalLong mediaSize() {
        return OptionalLong.empty();
    }

    /**
     * No-op; link previews do not carry a media-size field on the wire.
     *
     * @param mediaSize ignored
     */
    @Override
    public void setMediaSize(long mediaSize) {
        // link previews carry no media size on the wire
    }

    /**
     * Returns the media-path category used to route the HQ thumbnail
     * upload to the link-thumbnail CDN bucket.
     *
     * @return {@link MediaPath#THUMBNAIL_LINK}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.THUMBNAIL_LINK;
    }

    /**
     * Returns the legacy classification for the attached group invite link.
     *
     * @return an {@link Optional} containing the {@link InviteLinkGroupType},
     *         or {@link Optional#empty()} if no invite link is attached
     */
    public Optional<InviteLinkGroupType> inviteLinkGroupType() {
        return Optional.ofNullable(inviteLinkGroupType);
    }

    /**
     * Returns the subject of the parent community group referenced by the
     * attached invite link, if any.
     *
     * @return an {@link Optional} containing the parent subject,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> inviteLinkParentGroupSubjectV2() {
        return Optional.ofNullable(inviteLinkParentGroupSubjectV2);
    }

    /**
     * Returns the thumbnail of the parent community group referenced by
     * the attached invite link, if any.
     *
     * @return an {@link Optional} containing the parent thumbnail bytes,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<byte[]> inviteLinkParentGroupThumbnailV2() {
        return Optional.ofNullable(inviteLinkParentGroupThumbnailV2);
    }

    /**
     * Returns the V2 classification for the attached group invite link.
     *
     * @return an {@link Optional} containing the {@link InviteLinkGroupType},
     *         or {@link Optional#empty()} if no invite link is attached
     */
    public Optional<InviteLinkGroupType> inviteLinkGroupTypeV2() {
        return Optional.ofNullable(inviteLinkGroupTypeV2);
    }

    /**
     * Returns whether the attached preview is flagged as view-once.
     *
     * @return {@code true} if the preview is view-once, {@code false} otherwise
     */
    public boolean viewOnce() {
        return viewOnce != null && viewOnce;
    }

    /**
     * Returns the video preview height in pixels, if set.
     *
     * @return an {@link OptionalInt} containing the height,
     *         or {@link OptionalInt#empty()} if unset
     */
    public OptionalInt videoHeight() {
        return videoHeight == null ? OptionalInt.empty() : OptionalInt.of(videoHeight);
    }

    /**
     * Returns the video preview width in pixels, if set.
     *
     * @return an {@link OptionalInt} containing the width,
     *         or {@link OptionalInt#empty()} if unset
     */
    public OptionalInt videoWidth() {
        return videoWidth == null ? OptionalInt.empty() : OptionalInt.of(videoWidth);
    }

    /**
     * Returns the MMS-delivered favicon metadata for the attached link,
     * if any.
     *
     * @return an {@link Optional} containing the favicon metadata,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<MessageMMSThumbnailMetadata> faviconMMSMetadata() {
        return Optional.ofNullable(faviconMMSMetadata);
    }

    /**
     * Returns the extended link preview metadata attached to the message,
     * if any.
     *
     * @return an {@link Optional} containing the metadata,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<MessageLinkPreviewMetadata> linkPreviewMetadata() {
        return Optional.ofNullable(linkPreviewMetadata);
    }

    /**
     * Returns the payment-link metadata attached to the message, if any.
     *
     * @return an {@link Optional} containing the payment metadata,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<PaymentLinkMetadata> paymentLinkMetadata() {
        return Optional.ofNullable(paymentLinkMetadata);
    }

    /**
     * Returns the list of video end-card tiles shown after the attached
     * video finishes playing.
     *
     * @return an unmodifiable list of {@link MessageVideoEndCard} entries;
     *         an empty list if none are attached
     */
    public List<MessageVideoEndCard> endCardTiles() {
        return endCardTiles == null ? List.of() : Collections.unmodifiableList(endCardTiles);
    }

    /**
     * Returns the canonical URL of the attached video, if any.
     *
     * @return an {@link Optional} containing the video URL,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<String> videoContentUrl() {
        return Optional.ofNullable(videoContentUrl);
    }

    /**
     * Returns the embedded music metadata attached to the message, if any.
     *
     * @return an {@link Optional} containing the {@link EmbeddedMusic},
     *         or {@link Optional#empty()} if unset
     */
    public Optional<EmbeddedMusic> musicMetadata() {
        return Optional.ofNullable(musicMetadata);
    }

    /**
     * Returns the extra payment metadata attached to the message, if any.
     *
     * @return an {@link Optional} containing the metadata,
     *         or {@link Optional#empty()} if unset
     */
    public Optional<PaymentExtendedMetadata> paymentExtendedMetadata() {
        return Optional.ofNullable(paymentExtendedMetadata);
    }

    /**
     * Sets the raw text content of the message.
     *
     * @param text the text body, or {@code null} to clear
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the link substring of the text body that was matched for
     * preview generation.
     *
     * @param matchedText the matched link text, or {@code null} to clear
     */
    public void setMatchedText(String matchedText) {
        this.matchedText = matchedText;
    }

    /**
     * Sets the attached link preview description.
     *
     * @param description the description, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the attached link preview title.
     *
     * @param title the title, or {@code null} to clear
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the text colour as a packed ARGB integer.
     *
     * @param textArgb the ARGB value, or {@code null} to clear
     */
    public void setTextArgb(Integer textArgb) {
        this.textArgb = textArgb;
    }

    /**
     * Sets the bubble background colour as a packed ARGB integer.
     *
     * @param backgroundArgb the ARGB value, or {@code null} to clear
     */
    public void setBackgroundArgb(Integer backgroundArgb) {
        this.backgroundArgb = backgroundArgb;
    }

    /**
     * Sets the font applied to the rendered text.
     *
     * @param font the font type, or {@code null} to use the default
     */
    public void setFont(FontType font) {
        this.font = font;
    }

    /**
     * Sets the kind of preview attached to the message.
     *
     * @param previewType the preview type, or {@code null} to clear
     */
    public void setPreviewType(PreviewType previewType) {
        this.previewType = previewType;
    }

    /**
     * Sets the inline JPEG thumbnail bytes displayed while the full
     * preview is being fetched.
     *
     * @param jpegThumbnail the thumbnail bytes, or {@code null} to clear
     */
    public void setJpegThumbnail(byte[] jpegThumbnail) {
        this.jpegThumbnail = jpegThumbnail;
    }

    /**
     * Sets the contextual metadata attached to the message.
     *
     * @param contextInfo the context info, or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the inline-autoplay opt-out flag.
     *
     * @param doNotPlayInline {@code true} to disable inline autoplay,
     *                        {@code false} to enable it, {@code null} to clear
     */
    public void setDoNotPlayInline(Boolean doNotPlayInline) {
        this.doNotPlayInline = doNotPlayInline;
    }

    /**
     * Sets the CDN direct path for the encrypted thumbnail.
     *
     * @param thumbnailDirectPath the direct path, or {@code null} to clear
     */
    public void setThumbnailDirectPath(String thumbnailDirectPath) {
        this.thumbnailDirectPath = thumbnailDirectPath;
    }

    /**
     * Sets the SHA-256 digest of the plaintext thumbnail.
     *
     * @param thumbnailSha256 the digest, or {@code null} to clear
     */
    public void setThumbnailSha256(byte[] thumbnailSha256) {
        this.thumbnailSha256 = thumbnailSha256;
    }

    /**
     * Sets the SHA-256 digest of the encrypted thumbnail.
     *
     * @param thumbnailEncSha256 the digest, or {@code null} to clear
     */
    public void setThumbnailEncSha256(byte[] thumbnailEncSha256) {
        this.thumbnailEncSha256 = thumbnailEncSha256;
    }

    /**
     * Sets the symmetric media key used to decrypt the thumbnail.
     *
     * @param mediaKey the media key bytes, or {@code null} to clear
     */
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Sets the moment at which the media key was generated.
     *
     * @param mediaKeyTimestamp the timestamp, or {@code null} to clear
     */
    public void setMediaKeyTimestamp(Instant mediaKeyTimestamp) {
        this.mediaKeyTimestamp = mediaKeyTimestamp;
    }

    /**
     * Sets the thumbnail height in pixels.
     *
     * @param thumbnailHeight the height, or {@code null} to clear
     */
    public void setThumbnailHeight(Integer thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    /**
     * Sets the thumbnail width in pixels.
     *
     * @param thumbnailWidth the width, or {@code null} to clear
     */
    public void setThumbnailWidth(Integer thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    /**
     * Sets the legacy group invite-link classification.
     *
     * @param inviteLinkGroupType the classification, or {@code null} to clear
     */
    public void setInviteLinkGroupType(InviteLinkGroupType inviteLinkGroupType) {
        this.inviteLinkGroupType = inviteLinkGroupType;
    }

    /**
     * Sets the subject of the parent community group referenced by the
     * attached invite link.
     *
     * @param inviteLinkParentGroupSubjectV2 the parent subject, or {@code null} to clear
     */
    public void setInviteLinkParentGroupSubjectV2(String inviteLinkParentGroupSubjectV2) {
        this.inviteLinkParentGroupSubjectV2 = inviteLinkParentGroupSubjectV2;
    }

    /**
     * Sets the thumbnail of the parent community group referenced by the
     * attached invite link.
     *
     * @param inviteLinkParentGroupThumbnailV2 the parent thumbnail bytes,
     *                                         or {@code null} to clear
     */
    public void setInviteLinkParentGroupThumbnailV2(byte[] inviteLinkParentGroupThumbnailV2) {
        this.inviteLinkParentGroupThumbnailV2 = inviteLinkParentGroupThumbnailV2;
    }

    /**
     * Sets the V2 group invite-link classification.
     *
     * @param inviteLinkGroupTypeV2 the classification, or {@code null} to clear
     */
    public void setInviteLinkGroupTypeV2(InviteLinkGroupType inviteLinkGroupTypeV2) {
        this.inviteLinkGroupTypeV2 = inviteLinkGroupTypeV2;
    }

    /**
     * Sets the view-once flag on the attached preview.
     *
     * @param viewOnce {@code true} to mark as view-once,
     *                 {@code false} otherwise, {@code null} to clear
     */
    public void setViewOnce(Boolean viewOnce) {
        this.viewOnce = viewOnce;
    }

    /**
     * Sets the video preview height in pixels.
     *
     * @param videoHeight the height, or {@code null} to clear
     */
    public void setVideoHeight(Integer videoHeight) {
        this.videoHeight = videoHeight;
    }

    /**
     * Sets the video preview width in pixels.
     *
     * @param videoWidth the width, or {@code null} to clear
     */
    public void setVideoWidth(Integer videoWidth) {
        this.videoWidth = videoWidth;
    }

    /**
     * Sets the MMS-delivered favicon metadata for the attached link.
     *
     * @param faviconMMSMetadata the favicon metadata, or {@code null} to clear
     */
    public void setFaviconMMSMetadata(MessageMMSThumbnailMetadata faviconMMSMetadata) {
        this.faviconMMSMetadata = faviconMMSMetadata;
    }

    /**
     * Sets the extended link preview metadata.
     *
     * @param linkPreviewMetadata the metadata, or {@code null} to clear
     */
    public void setLinkPreviewMetadata(MessageLinkPreviewMetadata linkPreviewMetadata) {
        this.linkPreviewMetadata = linkPreviewMetadata;
    }

    /**
     * Sets the payment-link metadata.
     *
     * @param paymentLinkMetadata the metadata, or {@code null} to clear
     */
    public void setPaymentLinkMetadata(PaymentLinkMetadata paymentLinkMetadata) {
        this.paymentLinkMetadata = paymentLinkMetadata;
    }

    /**
     * Sets the list of video end-card tiles.
     *
     * @param endCardTiles the tile list, or {@code null} to clear
     */
    public void setEndCardTiles(List<MessageVideoEndCard> endCardTiles) {
        this.endCardTiles = endCardTiles;
    }

    /**
     * Sets the canonical URL of the attached video.
     *
     * @param videoContentUrl the video URL, or {@code null} to clear
     */
    public void setVideoContentUrl(String videoContentUrl) {
        this.videoContentUrl = videoContentUrl;
    }

    /**
     * Sets the embedded music metadata attached to the message.
     *
     * @param musicMetadata the metadata, or {@code null} to clear
     */
    public void setMusicMetadata(EmbeddedMusic musicMetadata) {
        this.musicMetadata = musicMetadata;
    }

    /**
     * Sets the extra payment metadata attached to the message.
     *
     * @param paymentExtendedMetadata the metadata, or {@code null} to clear
     */
    public void setPaymentExtendedMetadata(PaymentExtendedMetadata paymentExtendedMetadata) {
        this.paymentExtendedMetadata = paymentExtendedMetadata;
    }

    /**
     * The font families supported for styled text bubbles.
     *
     * <p>Each constant is an opaque identifier whose visual appearance is
     * controlled by the official WhatsApp clients. Third-party clients
     * should map unknown values to a reasonable default.
     */
    @ProtobufEnum(name = "Message.ExtendedTextMessage.FontType")
    public static enum FontType {
        /**
         * The platform default font used when no explicit font is chosen.
         */
        SYSTEM(0),
        /**
         * The platform default text-only font variant.
         */
        SYSTEM_TEXT(1),
        /**
         * Facebook's legacy script-style font.
         */
        FB_SCRIPT(2),
        /**
         * A bolder version of the platform system font.
         */
        SYSTEM_BOLD(6),
        /**
         * The "Morning Breeze" regular display font.
         */
        MORNINGBREEZE_REGULAR(7),
        /**
         * The "Calistoga" regular display font.
         */
        CALISTOGA_REGULAR(8),
        /**
         * The "Exo 2" extra-bold display font.
         */
        EXO2_EXTRABOLD(9),
        /**
         * The "Courier Prime" bold monospace font.
         */
        COURIERPRIME_BOLD(10);

        /**
         * Constructs a font constant with the supplied protobuf index.
         *
         * @param index the wire-format index of the constant
         */
        FontType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The wire-format index corresponding to this constant.
         */
        final int index;

        /**
         * Returns the wire-format index of this constant.
         *
         * @return the numeric protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * The classification of a WhatsApp group invite link, distinguishing
     * plain groups, parent community groups and sub-groups.
     */
    @ProtobufEnum(name = "Message.ExtendedTextMessage.InviteLinkGroupType")
    public static enum InviteLinkGroupType {
        /**
         * A regular group invite with no community hierarchy.
         */
        DEFAULT(0),
        /**
         * An invite to a parent community group.
         */
        PARENT(1),
        /**
         * An invite to a sub-group inside a community.
         */
        SUB(2),
        /**
         * An invite to the default sub-group of a community (e.g. the
         * announcements channel).
         */
        DEFAULT_SUB(3);

        /**
         * Constructs an invite-type constant with the supplied protobuf
         * index.
         *
         * @param index the wire-format index of the constant
         */
        InviteLinkGroupType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The wire-format index corresponding to this constant.
         */
        final int index;

        /**
         * Returns the wire-format index of this constant.
         *
         * @return the numeric protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * The kind of preview rendered inside an extended text bubble.
     */
    @ProtobufEnum(name = "Message.ExtendedTextMessage.PreviewType")
    public static enum PreviewType {
        /**
         * No preview; the message is rendered as plain text.
         */
        NONE(0),
        /**
         * A video preview with thumbnail and inline playback controls.
         */
        VIDEO(1),
        /**
         * A placeholder preview shown while the real preview is being
         * fetched or failed to load.
         */
        PLACEHOLDER(4),
        /**
         * A still-image preview.
         */
        IMAGE(5),
        /**
         * A preview for a WhatsApp Payments link.
         */
        PAYMENT_LINKS(6),
        /**
         * A preview for a user or business profile link.
         */
        PROFILE(7);

        /**
         * Constructs a preview-type constant with the supplied protobuf
         * index.
         *
         * @param index the wire-format index of the constant
         */
        PreviewType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The wire-format index corresponding to this constant.
         */
        final int index;

        /**
         * Returns the wire-format index of this constant.
         *
         * @return the numeric protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
