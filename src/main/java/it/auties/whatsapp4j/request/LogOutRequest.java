package it.auties.whatsapp4j.request;


import it.auties.whatsapp4j.request.Request;
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
