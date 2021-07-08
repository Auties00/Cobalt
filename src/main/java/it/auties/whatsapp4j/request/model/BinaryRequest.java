package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.whatsapp.WhatsappConfiguration;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.response.model.common.ResponseModel;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import lombok.NonNull;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An abstract model class that represents a binary request made from the client to the server
 *
 * @param <M> the type of the model
 */
@Accessors(fluent = true, chain = true)
public abstract non-sealed class BinaryRequest<M extends ResponseModel> extends Request<Node, M>{
    private static final WhatsappKeysManager KEYS_MANAGER = WhatsappKeysManager.singletonInstance();
    private static final BinaryEncoder ENCODER = new BinaryEncoder();
    
    private final @NonNull @Getter Node node;
    private final @NonNull @Getter BinaryFlag flag;
    private final @NonNull @Getter BinaryMetric[] tags;

    /**
     * Constructs a new instance of a BinaryRequest using a custom non null request tag
     *
     * @param tag the custom non null tag to assign to this request
     * @param configuration the configuration used for {@link WhatsappAPI}
     * @param flag the flag of this request
     * @param tags the tags for this request
     */
    protected BinaryRequest(@NonNull WhatsappConfiguration configuration, @NonNull String tag, @NonNull Node node, @NonNull BinaryFlag flag, @NonNull BinaryMetric... tags) {
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
    protected BinaryRequest(@NonNull WhatsappConfiguration configuration, @NonNull Node node, @NonNull BinaryFlag flag, @NonNull BinaryMetric... tags) {
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
    public @NonNull Node buildBody() {
        return node;
    }

    /**
     * Sends a binary request to the WebSocket linked to {@code session}.
     * This message is encoded using {@link BinaryRequest#ENCODER} and then encrypted using {@code whatsappKeys}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<M> send(@NonNull Session session) {
        try{
            var binaryMessage = encode();
            if (configuration.async()) {
                session.getAsyncRemote().sendBinary(binaryMessage, __ -> MANAGER.pendingRequests().add(this));
                return future;
            }

            session.getBasicRemote().sendBinary(binaryMessage);
            MANAGER.pendingRequests().add(this);
            return future;
        }catch (IOException exception){
            throw new RuntimeException("An exception occurred while sending a binary message", exception);
        }
    }

    public @NonNull ByteBuffer encode() {
        var messageTag = BinaryArray.forString("%s,".formatted(tag()));
        var encodedMessage = ENCODER.encodeMessage(buildBody());
        var encrypted = CypherUtils.aesEncrypt(encodedMessage, Objects.requireNonNull(KEYS_MANAGER.encKey()));
        var hmacSign = CypherUtils.hmacSha256(encrypted, Objects.requireNonNull(KEYS_MANAGER.macKey()));
        return messageTag.merged(BinaryMetric.toArray(tags())
                .merged(BinaryArray.singleton(flag().data())))
                .merged(hmacSign)
                .merged(encrypted)
                .toBuffer();
    }

}
