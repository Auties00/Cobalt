package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.Session;
import lombok.SneakyThrows;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a json request made from the client to the server
 *
 * @param <M> the type of the model
 */
public abstract non-sealed class JsonRequest<M extends JsonResponseModel> extends Request<List<Object>, M> {
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
     * Sends a json request to the WebSocket linked to {@code session}
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     **/
    @SneakyThrows
    public CompletableFuture<M> send(@NotNull Session session) {
        if (configuration.async()) {
            session.getAsyncRemote().sendObject(this, __ -> MANAGER.pendingRequests().add(this));
            if(noResponse()) future.complete(null);
            return future();
        }

        session.getBasicRemote().sendObject(this);
        MANAGER.pendingRequests().add(this);
        if(noResponse()) future.complete(null);
        return future();
    }
}
