package it.auties.whatsapp4j.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Request {
    @NotNull String tag();
    @NotNull List<Object> buildBody();

    @SneakyThrows
    default String toJson() {
        final var mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
        return "%s,%s".formatted(tag(), mapper.writeValueAsString(buildBody()));
    }
}
