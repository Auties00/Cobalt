package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.binary.constant.BinaryFlag;
import it.auties.whatsapp4j.binary.constant.BinaryMetric;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.protobuf.model.misc.Node;
import it.auties.whatsapp4j.response.model.common.ResponseModel;
import it.auties.whatsapp4j.serialization.StandardWhatsappSerializer;
import it.auties.whatsapp4j.serialization.WhatsappSerializer;
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
public abstract non-sealed class BinaryRequest<M extends ResponseModel> extends Request<Node, M>{
    private final @NonNull @Getter Node node;
    private final @NonNull WhatsappSerializer serializer;
    private final @NonNull @Getter BinaryFlag flag;
    private final @NonNull @Getter BinaryMetric[] tags;

    /**
     * Constructs a new instance of a BinaryRequest using a custom non null request tag
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     * @param keys the keys used to cypher this message
     * @param tag the custom non null tag to assign to this request
     * @param flag the flag of this request
     * @param tags the tags for this request
     */
    protected BinaryRequest(@NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager keys, @NonNull String tag, @NonNull Node node, @NonNull BinaryFlag flag, @NonNull BinaryMetric... tags) {
        super(tag, configuration);
        this.serializer = new StandardWhatsappSerializer(keys);
        this.node = node;
        this.flag = flag;
        this.tags = tags;
    }

    /**
     * Constructs a new instance of a BinaryRequest using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     * @param keys the keys used to cypher this message
     * @param flag the flag of this request
     * @param tags the tags for this request
     */
    protected BinaryRequest(@NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager keys, @NonNull Node node, @NonNull BinaryFlag flag, @NonNull BinaryMetric... tags) {
        super(configuration);
        this.serializer = new StandardWhatsappSerializer(keys);
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
    public @NonNull Node buildBody() {
        return node;
    }

    /**
     * Sends a binary request to the WebSocket linked to {@code session}.
     * This message is encoded using {@link BinaryRequest#serializer} and then encrypted using {@code whatsappKeys}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<M> send(@NonNull Session session) {
        try{
            var binaryMessage = serializer.serialize(this);
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
