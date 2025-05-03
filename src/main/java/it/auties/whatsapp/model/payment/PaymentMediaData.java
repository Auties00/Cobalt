package it.auties.whatsapp.model.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;

@ProtobufMessage(name = "PaymentBackground.MediaData")
public final class PaymentMediaData {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] mediaKey;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long mediaKeyTimestamp;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] mediaSha256;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final byte[] mediaEncryptedSha256;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String mediaDirectPath;

    PaymentMediaData(byte[] mediaKey, long mediaKeyTimestamp, byte[] mediaSha256, byte[] mediaEncryptedSha256, String mediaDirectPath) {
        this.mediaKey = Objects.requireNonNull(mediaKey, "mediaKey cannot be null");
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.mediaSha256 = Objects.requireNonNull(mediaSha256, "mediaSha256 cannot be null");
        this.mediaEncryptedSha256 = Objects.requireNonNull(mediaEncryptedSha256, "mediaEncryptedSha256 cannot be null");
        this.mediaDirectPath = Objects.requireNonNull(mediaDirectPath, "mediaDirectPath cannot be null");
    }

    public byte[] mediaKey() {
        return mediaKey;
    }

    public long mediaKeyTimestamp() {
        return mediaKeyTimestamp;
    }

    public byte[] mediaSha256() {
        return mediaSha256;
    }

    public byte[] mediaEncryptedSha256() {
        return mediaEncryptedSha256;
    }

    public String mediaDirectPath() {
        return mediaDirectPath;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PaymentMediaData that
                && Arrays.equals(mediaKey, that.mediaKey)
                && mediaKeyTimestamp == that.mediaKeyTimestamp
                && Arrays.equals(mediaSha256, that.mediaSha256)
                && Arrays.equals(mediaEncryptedSha256, that.mediaEncryptedSha256)
                && Objects.equals(mediaDirectPath, that.mediaDirectPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(mediaKey), mediaKeyTimestamp, Arrays.hashCode(mediaSha256), Arrays.hashCode(mediaEncryptedSha256), mediaDirectPath);
    }

    @Override
    public String toString() {
        return "PaymentMediaData[" +
                "mediaKey=" + Arrays.toString(mediaKey) +
                ", mediaKeyTimestamp=" + mediaKeyTimestamp +
                ", mediaSha256=" + Arrays.toString(mediaSha256) +
                ", mediaEncryptedSha256=" + Arrays.toString(mediaEncryptedSha256) +
                ", mediaDirectPath=" + mediaDirectPath +
                ']';
    }
}