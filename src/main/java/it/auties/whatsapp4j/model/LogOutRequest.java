package it.auties.whatsapp4j.model;


import org.jetbrains.annotations.NotNull;

import java.util.List;

public record LogOutRequest() implements Request {
    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "Conn", "disconnect");
    }

    @Override
    public @NotNull String tag() {
        return "goodbye,";
    }
}
