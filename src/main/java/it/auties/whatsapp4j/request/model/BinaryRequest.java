package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.common.ResponseModel;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a binary request made from the client to the server
 *
 * @param <M> the type of the model
 */
@Accessors(fluent = true, chain = true)
public abstract non-sealed class BinaryRequest<M extends ResponseModel> extends Request<WhatsappNode, M>{
    private final @NotNull @Getter WhatsappNode node;
    private final @NotNull @Getter BinaryFlag flag;
    private final @NotNull @Getter BinaryMetric[] tags;

    /**
     * Constructs a new instance of a BinaryRequest using a custom non null request tag
     *
     * @param tag the custom non null tag to assign to this request
     * @param configuration the configuration used for {@link WhatsappAPI}
     * @param flag the flag of this request
     * @param tags the tags for this request
     */
    protected BinaryRequest(@NotNull WhatsappConfiguration configuration, @NotNull String tag, @NotNull WhatsappNode node, @NotNull BinaryFlag flag, @NotNull BinaryMetric... tags) {
        super(tag, configuration);
        this.node = node;
        this.flag = flag;
        this.tags = tags;
    }

    /**
     * Constructs a new instance of a BinaryRequest using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     * @param flag the flag of this request
     * @param tags the tags for this request
     */
    protected BinaryRequest(@NotNull WhatsappConfiguration configuration, @NotNull WhatsappNode node, @NotNull BinaryFlag flag, @NotNull BinaryMetric... tags) {
        super(configuration);
        this.node = node;
        this.flag = flag;
        this.tags = tags;
    }

    /**
     * Returns the body of this request
     *
     * @return an object to send to WhatsappWeb's WebSocket
     */
    @Override
    public WhatsappNode buildBody() {
        return node;
    }

    /**
     * Sends a binary request to the WebSocket linked to {@code session}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    @SneakyThrows
    public CompletableFuture<M> send(@NotNull Session session) {
        if (configuration.async()) {
            session.getAsyncRemote().sendObject(this, __ -> MANAGER.pendingRequests().add(this));
            if(noResponse()) future.complete(null);
            return future;
        }

        session.getBasicRemote().sendObject(this);
        MANAGER.pendingRequests().add(this);
        if(noResponse()) future.complete(null);
        return future;
    }
}
