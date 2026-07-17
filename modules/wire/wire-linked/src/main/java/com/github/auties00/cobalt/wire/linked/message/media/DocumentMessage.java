package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveHeader;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A message whose payload is an encrypted document file.
 *
 * <p>Documents are arbitrary file attachments such as PDFs, office files, archives
 * or contact cards. This message carries the metadata required to download and
 * decrypt the file from WhatsApp's media servers plus optional presentation metadata
 * like a page count, a JPEG preview thumbnail, a caption and a display file name.
 *
 * <p>Document messages can also be used as headers of interactive messages and as
 * titles of template messages, hence the multiple implemented interfaces.
 */
@ProtobufMessage(name = "Message.DocumentMessage")
public final class DocumentMessage implements InteractiveHeader, InteractiveMessage.MediaSpec, TemplateMessage.Title, TemplateMessage.TitleSpec, MediaMessage {
    /**
     * URL of the encrypted document file on WhatsApp's media servers.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    /**
     * MIME type of the document, for example {@code application/pdf}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String mimetype;

    /**
     * Display title of the document, used by the UI when the file name is absent or
     * unhelpful.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String title;

    /**
     * SHA-256 digest of the decrypted document bytes, used to verify integrity.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    /**
     * Size in bytes of the decrypted document payload.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * Number of pages in the document, when the document type has pages (such as
     * PDFs).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer pageCount;

    /**
     * Symmetric key used to decrypt the document payload once downloaded.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * Original file name of the document as shown to the user on download.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String fileName;

    /**
     * SHA-256 digest of the encrypted document bytes as stored on the server.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * Server-relative path used to locate the encrypted document on WhatsApp's CDN.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * Moment at which the {@link #mediaKey} was generated.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * Whether this document is a shared contact vCard rather than a regular file
     * attachment.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    Boolean contactVcard;

    /**
     * CDN direct path to the encrypted preview thumbnail.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String thumbnailDirectPath;

    /**
     * SHA-256 digest of the decrypted thumbnail bytes.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BYTES)
    byte[] thumbnailSha256;

    /**
     * SHA-256 digest of the encrypted thumbnail bytes.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BYTES)
    byte[] thumbnailEncSha256;

    /**
     * Inline JPEG thumbnail bytes included directly in the message for fast preview
     * without a separate download.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    byte[] jpegThumbnail;

    /**
     * Contextual information attached to the document, such as a quoted message or
     * forwarding metadata.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Height in pixels of the thumbnail preview.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.UINT32)
    Integer thumbnailHeight;

    /**
     * Width in pixels of the thumbnail preview.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.UINT32)
    Integer thumbnailWidth;

    /**
     * Optional text caption displayed with the document attachment.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    String caption;

    /**
     * Accessibility label describing the document for screen readers.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.STRING)
    String accessibilityLabel;

    /**
     * Domain identifier that scopes how the {@link #mediaKey} was derived.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.ENUM)
    MediaMessageKeyDomain mediaKeyDomain;


    /**
     * Constructs a new document message with the given metadata.
     *
     * @param mediaUrl             the CDN URL of the encrypted payload
     * @param mimetype             the MIME type of the document
     * @param title                the display title
     * @param mediaSha256          the hash of the decrypted bytes
     * @param mediaSize            the size of the decrypted payload
     * @param pageCount            the number of pages when applicable
     * @param mediaKey             the decryption key
     * @param fileName             the original file name
     * @param mediaEncryptedSha256 the hash of the encrypted bytes
     * @param mediaDirectPath      the CDN direct path
     * @param mediaKeyTimestamp    when the key was generated
     * @param contactVcard         whether the payload is a contact card
     * @param thumbnailDirectPath  the CDN path of the preview thumbnail
     * @param thumbnailSha256      the hash of the decrypted thumbnail
     * @param thumbnailEncSha256   the hash of the encrypted thumbnail
     * @param jpegThumbnail        inline JPEG preview bytes
     * @param contextInfo          the context information
     * @param thumbnailHeight      the thumbnail height in pixels
     * @param thumbnailWidth       the thumbnail width in pixels
     * @param caption              a caption shown with the document
     * @param accessibilityLabel   the accessibility description
     * @param mediaKeyDomain       the key derivation domain
     */
    DocumentMessage(String mediaUrl, String mimetype, String title, byte[] mediaSha256, Long mediaSize, Integer pageCount, byte[] mediaKey, String fileName, byte[] mediaEncryptedSha256, String mediaDirectPath, Instant mediaKeyTimestamp, Boolean contactVcard, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, byte[] jpegThumbnail, ContextInfo contextInfo, Integer thumbnailHeight, Integer thumbnailWidth, String caption, String accessibilityLabel, MediaMessageKeyDomain mediaKeyDomain) {
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.title = title;
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.pageCount = pageCount;
        this.mediaKey = mediaKey;
        this.fileName = fileName;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.contactVcard = contactVcard;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.jpegThumbnail = jpegThumbnail;
        this.contextInfo = contextInfo;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailWidth = thumbnailWidth;
        this.caption = caption;
        this.accessibilityLabel = accessibilityLabel;
        this.mediaKeyDomain = mediaKeyDomain;
    }

    /**
     * Returns the CDN URL of the encrypted document payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    public Optional<String> url() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the CDN URL of the encrypted document payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the MIME type of the document.
     *
     * @return the MIME type, or empty if unknown
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the display title of the document.
     *
     * @return the title, or empty if none was provided
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the SHA-256 digest of the decrypted document.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the SHA-256 digest of the decrypted document.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the size in bytes of the decrypted document.
     *
     * @return the size, or empty if unknown
     */
    public OptionalLong fileLength() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the size in bytes of the decrypted document.
     *
     * @return the size, or empty if unknown
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the number of pages in the document.
     *
     * @return the page count, or empty if not applicable
     */
    public OptionalInt pageCount() {
        return pageCount == null ? OptionalInt.empty() : OptionalInt.of(pageCount);
    }

    /**
     * Returns the decryption key for the document payload.
     *
     * @return the key, or empty if unknown
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the original file name of the document.
     *
     * @return the file name, or empty if not provided
     */
    public Optional<String> fileName() {
        return Optional.ofNullable(fileName);
    }

    /**
     * Returns the SHA-256 digest of the encrypted document.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted document.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the CDN direct path used to download the encrypted document.
     *
     * @return the path, or empty if not yet uploaded
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the CDN direct path used to download the encrypted document.
     *
     * @return the path, or empty if not yet uploaded
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the moment at which the media key was generated.
     *
     * @return the key timestamp, or empty if unknown
     */
    public Optional<Instant> mediaKeyTimestamp() {
        return Optional.ofNullable(mediaKeyTimestamp);
    }

    /**
     * Returns whether this document is a shared contact vCard.
     *
     * @return {@code true} if the payload is a contact card, {@code false} otherwise
     */
    public boolean contactVcard() {
        return contactVcard != null && contactVcard;
    }

    /**
     * Returns the CDN path to the encrypted thumbnail.
     *
     * @return the path, or empty if no remote thumbnail is available
     */
    public Optional<String> thumbnailDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    /**
     * Returns the SHA-256 digest of the decrypted thumbnail.
     *
     * @return the hash, or empty if not present
     */
    public Optional<byte[]> thumbnailSha256() {
        return Optional.ofNullable(thumbnailSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted thumbnail.
     *
     * @return the hash, or empty if not present
     */
    public Optional<byte[]> thumbnailEncSha256() {
        return Optional.ofNullable(thumbnailEncSha256);
    }

    /**
     * Returns the inline JPEG thumbnail bytes shipped alongside the message.
     *
     * @return the thumbnail bytes, or empty if not present
     */
    public Optional<byte[]> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }

    /**
     * Returns the context information attached to this document message.
     *
     * @return the context info, or empty if none is attached
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the thumbnail height in pixels.
     *
     * @return the height, or empty if unknown
     */
    public OptionalInt thumbnailHeight() {
        return thumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbnailHeight);
    }

    /**
     * Returns the thumbnail width in pixels.
     *
     * @return the width, or empty if unknown
     */
    public OptionalInt thumbnailWidth() {
        return thumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbnailWidth);
    }

    /**
     * Returns the caption shown together with the document.
     *
     * @return the caption, or empty if none was provided
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the accessibility label describing the document.
     *
     * @return the label, or empty if not provided
     */
    public Optional<String> accessibilityLabel() {
        return Optional.ofNullable(accessibilityLabel);
    }

    /**
     * Returns the domain identifier that scopes how the media key was derived.
     *
     * @return the key domain, or empty if unset
     */
    public Optional<MediaMessageKeyDomain> mediaKeyDomain() {
        return Optional.ofNullable(mediaKeyDomain);
    }

    /**
     * Returns the {@link MediaPath} classification for document media, used to select
     * the correct CDN upload/download endpoint.
     *
     * @return {@link MediaPath#DOCUMENT}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.DOCUMENT;
    }

    /**
     * Updates the CDN URL of the encrypted document payload.
     *
     * @param mediaUrl the new URL, or {@code null} to clear
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Updates the document MIME type.
     *
     * @param mimetype the new MIME type, or {@code null} to clear
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Updates the display title.
     *
     * @param title the new title, or {@code null} to clear
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the SHA-256 digest of the decrypted document.
     *
     * @param mediaSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaSha256(byte[] mediaSha256) {
        this.mediaSha256 = mediaSha256;
    }

    /**
     * Updates the size in bytes of the decrypted document.
     *
     * @param mediaSize the new size
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Updates the page count.
     *
     * @param pageCount the new page count, or {@code null} to clear
     */
    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * Updates the symmetric key used to decrypt the document payload.
     *
     * @param mediaKey the new key, or {@code null} to clear
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Updates the original file name.
     *
     * @param fileName the new file name, or {@code null} to clear
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Updates the SHA-256 digest of the encrypted document.
     *
     * @param mediaEncryptedSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Updates the CDN direct path.
     *
     * @param mediaDirectPath the new path, or {@code null} to clear
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    /**
     * Updates the moment at which the media key was generated.
     *
     * @param mediaKeyTimestamp the new timestamp, or {@code null} to clear
     */
    @Override
    public void setMediaKeyTimestamp(Instant mediaKeyTimestamp) {
        this.mediaKeyTimestamp = mediaKeyTimestamp;
    }

    /**
     * Updates the contact vCard flag.
     *
     * @param contactVcard {@code true} if the payload is a contact card, {@code false} or {@code null} otherwise
     */
    public void setContactVcard(Boolean contactVcard) {
        this.contactVcard = contactVcard;
    }

    /**
     * Updates the CDN path of the encrypted thumbnail.
     *
     * @param thumbnailDirectPath the new path, or {@code null} to clear
     */
    public void setThumbnailDirectPath(String thumbnailDirectPath) {
        this.thumbnailDirectPath = thumbnailDirectPath;
    }

    /**
     * Updates the SHA-256 digest of the decrypted thumbnail.
     *
     * @param thumbnailSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailSha256(byte[] thumbnailSha256) {
        this.thumbnailSha256 = thumbnailSha256;
    }

    /**
     * Updates the SHA-256 digest of the encrypted thumbnail.
     *
     * @param thumbnailEncSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailEncSha256(byte[] thumbnailEncSha256) {
        this.thumbnailEncSha256 = thumbnailEncSha256;
    }

    /**
     * Updates the inline JPEG thumbnail bytes.
     *
     * @param jpegThumbnail the new thumbnail, or {@code null} to clear
     */
    public void setJpegThumbnail(byte[] jpegThumbnail) {
        this.jpegThumbnail = jpegThumbnail;
    }

    /**
     * Updates the context information attached to this document message.
     *
     * @param contextInfo the new context info, or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Updates the thumbnail height in pixels.
     *
     * @param thumbnailHeight the new height, or {@code null} to clear
     */
    public void setThumbnailHeight(Integer thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    /**
     * Updates the thumbnail width in pixels.
     *
     * @param thumbnailWidth the new width, or {@code null} to clear
     */
    public void setThumbnailWidth(Integer thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    /**
     * Updates the caption.
     *
     * @param caption the new caption, or {@code null} to clear
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Updates the accessibility label.
     *
     * @param accessibilityLabel the new label, or {@code null} to clear
     */
    public void setAccessibilityLabel(String accessibilityLabel) {
        this.accessibilityLabel = accessibilityLabel;
    }

    /**
     * Updates the key derivation domain.
     *
     * @param mediaKeyDomain the new domain, or {@code null} to clear
     */
    public void setMediaKeyDomain(MediaMessageKeyDomain mediaKeyDomain) {
        this.mediaKeyDomain = mediaKeyDomain;
    }
}
