package it.auties.whatsapp4j.response.model;

import it.auties.whatsapp4j.response.model.Response;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record JsonListResponse(@NotNull List<Object> data) implements Response {
}
