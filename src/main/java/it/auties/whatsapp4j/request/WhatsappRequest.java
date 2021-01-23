package it.auties.whatsapp4j.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AllArgsConstructor
public abstract class WhatsappRequest {
    private static final ObjectWriter JACKSON = new ObjectMapper().writerWithDefaultPrettyPrinter();
    public final @NotNull WhatsappKeysManager keysManager;
    public final @NotNull WhatsappConfiguration options;

    public abstract @NotNull String tag();
    public abstract @NotNull List<Object> buildBody();

    @SneakyThrows
    public String toJson() {
        return "%s,%s".formatted(tag(), JACKSON.writeValueAsString(buildBody()));
    }
}
