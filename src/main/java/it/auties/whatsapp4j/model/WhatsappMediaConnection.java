package it.auties.whatsapp4j.model;

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
public final class WhatsappMediaConnection {
    private final @NotNull String auth;
    private final int ttl;
}