package it.auties.whatsapp4j.request.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.websocket.Session;
import lombok.SneakyThrows;
import jakarta.validation.constraints.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a json request made from the client to the server
 *
 * @param <M>
 */
public abstract non-sealed class JsonRequest<M extends JsonResponseModel> extends Request<M> {
    /**
     * An instance of Jackson used to serialize objects as JSON
     */
    private static final ObjectWriter JACKSON = new ObjectMapper().writerWithDefaultPrettyPrinter();

    /**
     * Constructs a new instance of a JsonRequest using a custom non null request tag
     *
     * @param tag the custom non null tag to assign to this request
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected JsonRequest(@NotNull String tag, @NotNull WhatsappConfiguration configuration) {
        super(tag, configuration);
    }

    /**
     * Constructs a new instance of a JsonRequest using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected JsonRequest(@NotNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    /**
     * Sends a json request to the WebSocket linked to {@code session}.
     * This message is serialized using {@link JsonRequest#JACKSON}.
     *
     */
    @SneakyThrows
    public CompletableFuture<M> send(@NotNull Session session) {
        var body = buildBody();
        var json = JACKSON.writeValueAsString(body);
        var request = "%s,%s".formatted(tag, json);
        if (configuration.async()) {
            session.getAsyncRemote().sendObject(request, __ -> MANAGER.pendingRequests().add(this));
            return future();
        }

        session.getBasicRemote().sendObject(request);
        MANAGER.pendingRequests().add(this);
        return future();
    }
}
