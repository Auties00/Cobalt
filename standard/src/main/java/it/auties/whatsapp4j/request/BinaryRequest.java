package it.auties.whatsapp4j.request;

import it.auties.whatsapp4j.common.api.AbstractWhatsappAPI;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.request.AbstractBinaryRequest;
import it.auties.whatsapp4j.common.response.ResponseModel;
import it.auties.whatsapp4j.common.binary.BinaryFlag;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.binary.BinaryMetric;
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
public abstract class BinaryRequest<M extends ResponseModel> extends AbstractBinaryRequest<M> {
    /**
     * Instance of BinaryEncoder used to serialize {@link BinaryRequest#buildBody()}
     */
    private static final BinaryEncoder ENCODER = new BinaryEncoder();
    
    private final @NonNull @Getter BinaryFlag flag;
    private final @NonNull @Getter BinaryMetric[] metrics;
    
    /**
     * Constructs a new instance of a BinaryRequest using a custom non-null request tag
     *
     * @param tag the custom non-null tag to assign to this request
     * @param node the node of this request
     * @param configuration the configuration used for {@link AbstractWhatsappAPI}
     * @param keys the keys of this request
     * @param flag the flag of this request
     * @param metrics the tags for this request
     */
    protected BinaryRequest(@NonNull String tag, @NonNull Node node, @NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager keys, @NonNull BinaryFlag flag, @NonNull BinaryMetric... metrics){
        super(tag, node, configuration, keys);
        this.flag = flag;
        this.metrics = metrics;
    }

    /**
     * Constructs a new instance of a BinaryRequest using the default request tag built using {@code configuration}
     *
     * @param node the node of this request
     * @param configuration the configuration used for {@link AbstractWhatsappAPI}
     * @param keys the keys of this request
     * @param flag the flag of this request
     * @param metrics the tags for this request
     */
    protected BinaryRequest(@NonNull Node node, @NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager keys, @NonNull BinaryFlag flag, @NonNull BinaryMetric... metrics){
        super(node, configuration, keys);
        this.flag = flag;
        this.metrics = metrics;
    }

    /**
     * Sends a binary request to the WebSocket linked to {@code session}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public @NonNull CompletableFuture<M> send(@NonNull Session session) {
        try{
            var messageTag = BinaryArray.forString("%s,".formatted(tag()));
            var encodedMessage = ENCODER.encode(buildBody());
            var encrypted = CypherUtils.aesEncrypt(encodedMessage, keys.encKey());
            var hmacSign = CypherUtils.hmacSha256(encrypted, keys.macKey());
            var header = BinaryMetric.toArray(metrics()).append(flag().data());
            var binaryMessage = messageTag.append(header).append(hmacSign).append(encrypted).toBuffer();
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
