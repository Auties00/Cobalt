package it.auties.whatsapp.model.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;


/**
 * A model class that represents a message to notify the invoice about a successful payment.
 */
@ProtobufMessage
public final class PaymentInvoiceMessage extends MediaMessage<PaymentInvoiceMessage> implements PaymentMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String note;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String token;
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    private final PaymentAttachmentType paymentAttachmentType;
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private final String mimeType;
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    private byte[] mediaKey;
    @ProtobufProperty(index = 6, type = ProtobufType.UINT64)
    private Long mediaKeyTimestampSeconds;
    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    private byte[] mediaSha256;
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    private byte[] mediaEncryptedSha256;
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    private String mediaDirectPath;
    @ProtobufProperty(index = 10, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PaymentInvoiceMessage(String note, String token, PaymentAttachmentType paymentAttachmentType, String mimeType, byte[] mediaKey, Long mediaKeyTimestampSeconds, byte[] mediaSha256, byte[] mediaEncryptedSha256, String mediaDirectPath, byte[] thumbnail, ContextInfo contextInfo) {
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
    public PaymentInvoiceMessage setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public PaymentInvoiceMessage setMediaKeyTimestamp(Long timestamp) {
        this.mediaKeyTimestampSeconds = timestamp;
        return this;
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
    public PaymentInvoiceMessage setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public PaymentInvoiceMessage setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public PaymentInvoiceMessage setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.empty();
    }

    @Override
    public PaymentInvoiceMessage setMediaUrl(String mediaUrl) {
        return this;
    }

    @Override
    public OptionalLong mediaSize() {
        return OptionalLong.empty();
    }

    @Override
    public PaymentInvoiceMessage setMediaSize(long mediaSize) {
        return this;
    }

    @Override
    public MessageType type() {
        return MessageType.PAYMENT_INVOICE;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.IMAGE;
    }

    @Override
    public MediaMessageType mediaType() {
        return paymentAttachmentType().map(PaymentAttachmentType::toMediaType)
                .orElse(MediaMessageType.NONE);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.PAYMENT;
    }

    @Override
    public PaymentInvoiceMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
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

        public int index() {
            return index;
        }

        public MediaMessageType toMediaType() {
            return switch (this) {
                case IMAGE -> MediaMessageType.IMAGE;
                case PDF -> MediaMessageType.DOCUMENT;
            };
        }
    }
}