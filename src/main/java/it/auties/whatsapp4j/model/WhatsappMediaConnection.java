package it.auties.whatsapp4j.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Objects;

public record WhatsappMediaConnection(@NotNull String auth, int ttl) {
}