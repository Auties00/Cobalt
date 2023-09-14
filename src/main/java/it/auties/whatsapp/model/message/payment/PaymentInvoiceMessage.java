package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;


/**
 * A model class that represents a message to notify the invoice about a successful payment.
 */
public final class PaymentInvoiceMessage implements PaymentMessage, MediaMessage<PaymentInvoiceMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    @Nullable
    private final String note;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    @NonNull
    private final String token;

    @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
    @Nullable
    private final PaymentAttachmentType paymentAttachmentType;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    @Nullable
    private final String mimeType;

    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    private byte @Nullable [] mediaKey;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT64)
    @Nullable
    private final Long mediaKeyTimestampSeconds;

    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    private byte @Nullable [] mediaSha256;

    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    private byte @Nullable [] mediaEncryptedSha256;

    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    @Nullable
    private String mediaDirectPath;

    @ProtobufProperty(index = 10, type = ProtobufType.BYTES)
    private final byte @Nullable [] thumbnail;

    @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
    @Nullable
    private final ContextInfo contextInfo;

    public PaymentInvoiceMessage(@Nullable String note, @NonNull String token, @Nullable PaymentAttachmentType paymentAttachmentType, @Nullable String mimeType, byte @Nullable [] mediaKey, @Nullable Long mediaKeyTimestampSeconds, byte @Nullable [] mediaSha256, byte @Nullable [] mediaEncryptedSha256, @Nullable String mediaDirectPath, byte @Nullable [] thumbnail, @Nullable ContextInfo contextInfo) {
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
}