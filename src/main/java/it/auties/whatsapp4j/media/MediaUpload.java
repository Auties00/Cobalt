package it.auties.whatsapp4j.media;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.protobuf.message.MediaMessageType;
import jakarta.validation.constraints.NotNull;

public record MediaUpload(@NotNull String url, @NotNull String directPath,
                          @NotNull BinaryArray mediaKey, byte[] file,
                          byte[] fileSha256, byte[] fileEncSha256, byte[] sidecar,
                          @NotNull MediaMessageType mediaType) {
}
