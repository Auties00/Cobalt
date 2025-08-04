package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;


/**
 * A model class that represents a message to notify the invoice about a successful payment.
 */
@ProtobufMessage
public final class PaymentInvoiceMessage extends MediaMessage implements PaymentMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String note;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String token;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final PaymentAttachmentType paymentAttachmentType;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String mimeType;

    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] mediaKey;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT64)
    Long mediaKeyTimestampSeconds;

    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String mediaDirectPath;

    @ProtobufProperty(index = 10, type = ProtobufType.BYTES)
    final byte[] thumbnail;

    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    PaymentInvoiceMessage(String note, String token, PaymentAttachmentType paymentAttachmentType, String mimeType, byte[] mediaKey, Long mediaKeyTimestampSeconds, byte[] mediaSha256, byte[] mediaEncryptedSha256, String mediaDirectPath, byte[] thumbnail, ContextInfo contextInfo) {
        this.note = note;
        this.token = token;
        this.paymentAttachmentType = paymentAttachmentType;
        this.mimeType = mimeType;
        this.mediaKey = mediaKey;
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        this.mediaSha256 = mediaSha256;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.thumbnail = thumbnail;
        this.contextInfo = contextInfo;
    }

    public Optional<String> note() {
        return Optional.ofNullable(note);
    }

    public String token() {
        return this.token;
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public Optional<PaymentAttachmentType> paymentAttachmentType() {
        return Optional.ofNullable(paymentAttachmentType);
    }

    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeType);
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public void setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
    }

    @Override
    public void setMediaKeyTimestamp(Long timestamp) {
        this.mediaKeyTimestampSeconds = timestamp;
    }

    @Override
    public OptionalLong mediaKeyTimestampSeconds() {
        return Clock.parseTimestamp(mediaKeyTimestampSeconds);
    }

    @Override
    public Optional<ZonedDateTime> mediaKeyTimestamp() {
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public void setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public void setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.empty();
    }

    @Override
    public void setMediaUrl(String mediaUrl) {
    }

    @Override
    public OptionalLong mediaSize() {
        return OptionalLong.empty();
    }

    @Override
    public void setMediaSize(long mediaSize) {
    }

    @Override
    public Message.Type type() {
        return Message.Type.PAYMENT_INVOICE;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.IMAGE;
    }

    @Override
    public MediaMessage.Type mediaType() {
        return switch (paymentAttachmentType) {
            case null -> MediaMessage.Type.NONE;
            case IMAGE -> MediaMessage.Type.IMAGE;
            case PDF -> MediaMessage.Type.DOCUMENT;
        };
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public Category category() {
        return Category.PAYMENT;
    }

    @Override
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * The constants of this enumerated type describe the various types of attachment that an invoice
     * can wrap
     */
    @ProtobufEnum
    public enum PaymentAttachmentType {
        /**
         * Image
         */
        IMAGE(0),
        /**
         * PDF
         */
        PDF(1);

        final int index;

        PaymentAttachmentType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}