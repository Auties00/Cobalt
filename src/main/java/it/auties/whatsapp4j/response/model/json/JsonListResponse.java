package it.auties.whatsapp4j.response.model.json;

import it.auties.whatsapp4j.response.model.shared.Response;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record JsonListResponse(@NotNull List<Object> data) implements Response {
}
