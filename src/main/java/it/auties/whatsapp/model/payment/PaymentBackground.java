package it.auties.whatsapp.model.payment;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage(name = "PaymentBackground")
public final class PaymentBackground {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    final long mediaSize;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    final int width;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    final int height;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String mimetype;

    @ProtobufProperty(index = 6, type = ProtobufType.FIXED32)
    final int placeholderArgb;

    @ProtobufProperty(index = 7, type = ProtobufType.FIXED32)
    final int textArgb;

    @ProtobufProperty(index = 8, type = ProtobufType.FIXED32)
    final int subtextArgb;

    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    final PaymentMediaData mediaData;

    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    final PaymentBackgroundType type;

    PaymentBackground(String id, long mediaSize, int width, int height, String mimetype, int placeholderArgb, int textArgb, int subtextArgb, PaymentMediaData mediaData, PaymentBackgroundType type) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.mediaSize = mediaSize;
        this.width = width;
        this.height = height;
        this.mimetype = Objects.requireNonNull(mimetype, "mimetype cannot be null");
        this.placeholderArgb = placeholderArgb;
        this.textArgb = textArgb;
        this.subtextArgb = subtextArgb;
        this.mediaData = mediaData;
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    public String id() {
        return id;
    }

    public long mediaSize() {
        return mediaSize;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String mimetype() {
        return mimetype;
    }

    public int placeholderArgb() {
        return placeholderArgb;
    }

    public int textArgb() {
        return textArgb;
    }

    public int subtextArgb() {
        return subtextArgb;
    }

    public Optional<PaymentMediaData> mediaData() {
        return Optional.ofNullable(mediaData);
    }

    public PaymentBackgroundType type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PaymentBackground that
                && Objects.equals(id, that.id)
                && mediaSize == that.mediaSize
                && width == that.width
                && height == that.height
                && Objects.equals(mimetype, that.mimetype)
                && placeholderArgb == that.placeholderArgb
                && textArgb == that.textArgb
                && subtextArgb == that.subtextArgb
                && Objects.equals(mediaData, that.mediaData)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mediaSize, width, height, mimetype, placeholderArgb, textArgb, subtextArgb, mediaData, type);
    }

    @Override
    public String toString() {
        return "PaymentBackground[" +
                "id=" + id +
                ", mediaSize=" + mediaSize +
                ", width=" + width +
                ", height=" + height +
                ", mimetype=" + mimetype +
                ", placeholderArgb=" + placeholderArgb +
                ", textArgb=" + textArgb +
                ", subtextArgb=" + subtextArgb +
                ", mediaData=" + mediaData +
                ", type=" + type +
                ']';
    }

    @ProtobufEnum
    public enum PaymentBackgroundType {
        UNKNOWN(0),
        DEFAULT(1);

        final int index;

        PaymentBackgroundType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

    }
}