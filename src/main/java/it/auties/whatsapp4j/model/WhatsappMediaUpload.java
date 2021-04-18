package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.binary.BinaryArray;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public final class WhatsappMediaUpload {
    private final @NotNull String url;
    private final @NotNull String directPath;
    private final @NotNull BinaryArray mediaKey;
    private final byte @NotNull [] file;
    private final byte @NotNull [] fileSha256;
    private final byte @NotNull [] fileEncSha256;
    private final byte @NotNull [] sidecar;
    private final @NotNull WhatsappMediaMessageType mediaType;

    public WhatsappMediaUpload(@NotNull String url, @NotNull String directPath, @NotNull BinaryArray mediaKey, byte @NotNull [] file, byte @NotNull [] fileSha256, byte @NotNull [] fileEncSha256, byte @NotNull [] sidecar, @NotNull WhatsappMediaMessageType mediaType) {
        this.url = url;
        this.directPath = directPath;
        this.mediaKey = mediaKey;
        this.file = file;
        this.fileSha256 = fileSha256;
        this.fileEncSha256 = fileEncSha256;
        this.sidecar = sidecar;
        this.mediaType = mediaType;
    }

    public @NotNull String url() {
        return url;
    }

    public @NotNull String directPath() {
        return directPath;
    }

    public @NotNull BinaryArray mediaKey() {
        return mediaKey;
    }

    public byte @NotNull [] file() {
        return file;
    }

    public byte @NotNull [] fileSha256() {
        return fileSha256;
    }

    public byte @NotNull [] fileEncSha256() {
        return fileEncSha256;
    }

    public byte @NotNull [] sidecar() {
        return sidecar;
    }

    public @NotNull WhatsappMediaMessageType mediaType() {
        return mediaType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WhatsappMediaUpload) obj;
        return Objects.equals(this.url, that.url) &&
                Objects.equals(this.directPath, that.directPath) &&
                Objects.equals(this.mediaKey, that.mediaKey) &&
                Objects.equals(this.file, that.file) &&
                Objects.equals(this.fileSha256, that.fileSha256) &&
                Objects.equals(this.fileEncSha256, that.fileEncSha256) &&
                Objects.equals(this.sidecar, that.sidecar) &&
                Objects.equals(this.mediaType, that.mediaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, directPath, mediaKey, file, fileSha256, fileEncSha256, sidecar, mediaType);
    }

    @Override
    public String toString() {
        return "WhatsappMediaUpload[" +
                "url=" + url + ", " +
                "directPath=" + directPath + ", " +
                "mediaKey=" + mediaKey + ", " +
                "file=" + file + ", " +
                "fileSha256=" + fileSha256 + ", " +
                "fileEncSha256=" + fileEncSha256 + ", " +
                "sidecar=" + sidecar + ", " +
                "mediaType=" + mediaType + ']';
    }


}
