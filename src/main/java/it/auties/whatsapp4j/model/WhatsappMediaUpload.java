package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.binary.BinaryArray;
import jakarta.validation.constraints.NotNull;

public record WhatsappMediaUpload(@NotNull String url, @NotNull String directPath, @NotNull BinaryArray mediaKey, byte @NotNull [] file, byte @NotNull [] fileSha256, byte @NotNull [] fileEncSha256, byte @NotNull [] sidecar, @NotNull WhatsappMediaMessageType mediaType) {

}
