package it.auties.whatsapp4j.common.request;

import it.auties.whatsapp4j.common.api.AbstractWhatsappAPI;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.response.ResponseModel;
import it.auties.whatsapp4j.common.utils.WhatsappUtils;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a binary request made from the client to the server
 *
 * @param <M> the type of the model
 */
@Accessors(fluent = true, chain = true)
public abstract non-sealed class AbstractBinaryRequest<M extends ResponseModel> extends Request<Node, M>{
    private final @NonNull @Getter Node node;
    private final @NonNull WhatsappKeysManager keys;

    /**
     * Constructs a new instance of a BinaryRequest using a custom non-null request tag
     *
     * @param tag the custom non-null tag to assign to this request
     * @param node the node of this request
     * @param configuration the configuration used for {@link AbstractWhatsappAPI}
     * @param keys the keys of this request
     */
    protected AbstractBinaryRequest(@NonNull String tag, @NonNull Node node, @NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager keys){
        super(tag, configuration, new CompletableFuture<>());
        this.node = node;
        this.keys = keys;
    }

    /**
     * Constructs a new instance of a BinaryRequest using the default request tag built using {@code configuration}
     *
     * @param node the node of this request
     * @param configuration the configuration used for {@link AbstractWhatsappAPI}
     * @param keys the keys of this request
     */
    protected AbstractBinaryRequest(@NonNull Node node, @NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager keys){
        super(WhatsappUtils.buildRequestTag(configuration), configuration);
        this.node = node;
        this.keys = keys;
    }

    /**
     * Returns the body of this request
     *
     * @return an object to send to WhatsappWeb's WebSocket
     */
    @Override
    public @NonNull Node buildBody() {
        return node;
    }

    /**
     * Sends a binary request to the WebSocket linked to {@code session}.
     * This message is encoded using {@link WhatsappKeysManager#serializer()} and then encrypted using {@code whatsappKeys}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<M> send(@NonNull Session session) {
        try{
            var binaryMessage = keys.serializer().serialize(this);
            if (configuration.async()) {
                session.getAsyncRemote().sendBinary(binaryMessage, __ -> addRequest());
                return future;
            }

            session.getBasicRemote().sendBinary(binaryMessage);
            addRequest();
            return future;
        }catch (IOException exception){
            throw new RuntimeException("An exception occurred while sending a binary message", exception);
        }
    }
}
