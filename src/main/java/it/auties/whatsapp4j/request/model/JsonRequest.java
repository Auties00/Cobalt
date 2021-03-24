package it.auties.whatsapp4j.request.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.websocket.Session;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @param session the WhatsappWeb's WebSocket session
     * @param callback a callback to execute after the request has been successfully been sent
     * @return this request
     */
    @SneakyThrows
    public JsonRequest<M> send(@NotNull Session session, @Nullable Runnable callback) {
        var body = buildBody();
        var json = JACKSON.writeValueAsString(body);
        var request = "%s,%s".formatted(tag, json);
        if (configuration.async()) {
            session.getAsyncRemote().sendObject(request, __ -> {
                if (callback != null) callback.run();
                if (isCompletable()) {
                    MANAGER.pendingRequests().add(this);
                    return;
                }

                future.complete(null);
            });

            return this;
        }

        session.getBasicRemote().sendObject(request);
        if (callback != null) callback.run();
        if (isCompletable()) {
            MANAGER.pendingRequests().add(this);
            return this;
        }

        future.complete(null);
        return this;
    }

    /**
     * Sends a json request to the WebSocket linked to {@code session}.
     * This message is serialized using {@link JsonRequest#JACKSON}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    @SneakyThrows
    public JsonRequest<M> send(@NotNull Session session) {
        return send(session, null);
    }
}
