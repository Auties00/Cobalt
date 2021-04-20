package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.binary.BinaryArray;
import jakarta.validation.constraints.NotNull;

public record WhatsappMediaUpload(@NotNull String url, @NotNull String directPath,
                                  @NotNull BinaryArray mediaKey, byte[] file,
                                  byte[] fileSha256, byte[] fileEncSha256, byte[] sidecar,
                                  @NotNull WhatsappMediaMessageType mediaType) {
}
