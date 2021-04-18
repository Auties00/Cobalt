package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.binary.BinaryArray;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class WhatsappMediaUpload {
    private final @NotNull String url;
    private final @NotNull String directPath;
    private final @NotNull BinaryArray mediaKey;
    private final byte @NotNull [] file;
    private final byte @NotNull [] fileSha256;
    private final byte @NotNull [] fileEncSha256;
    private final byte @NotNull [] sidecar;
    private final @NotNull WhatsappMediaMessageType mediaType;
}
