package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.shared.ResponseModel;
import it.auties.whatsapp4j.utils.CypherUtils;
import jakarta.websocket.Session;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An abstract model class that represents a binary request made from the client to the server
 *
 * @param <M>
 */
public abstract non-sealed class BinaryRequest<M extends ResponseModel> extends Request<M> {
    /**
     * An instance of BinaryEncoder used to serialize {@link WhatsappNode} as a Whatsapp encoded array of bytes
     */
    private final BinaryEncoder ENCODER = new BinaryEncoder();

    /**
     * Constructs a new instance of a BinaryRequest using a custom non null request tag
     *
     * @param tag the custom non null tag to assign to this request
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected BinaryRequest(@NotNull String tag, @NotNull WhatsappConfiguration configuration) {
        super(tag, configuration);
    }

    /**
     * Constructs a new instance of a BinaryRequest using the default request tag built using {@param configuration}
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected BinaryRequest(@NotNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    /**
     * Sends a binary request to the WebSocket linked to {@param session}
     * This message is encoded using {@link BinaryRequest#ENCODER} and then encrypted using {@param whatsappKeys}
     *
     * @param session the WhatsappWeb's WebSocket session
     * @param whatsappKeys the keys used to encrypt this message
     * @param flag the flag used to determine how whatsapp should handle this request
     * @param tags the tags used to categorize this request: usually this tag is the enum matching the description of the last WhatsappNode in the request, including the ones provided as content
     * @return this request
     */
    @SneakyThrows
    public BinaryRequest<M> send(@NotNull Session session, @NotNull WhatsappKeysManager whatsappKeys, @NotNull BinaryFlag flag, @NotNull BinaryMetric... tags) {
        var body = buildBody();
        if (!(body instanceof WhatsappNode node)) {
            throw new IllegalArgumentException("WhatsappRequest#sendRequest: Cannot accept %s as content for binary message, expected List<WhatsappNode>".formatted(body.getClass().getName()));
        }

        var messageTag = BinaryArray.forString("%s,".formatted(tag));
        var encodedMessage = ENCODER.encodeMessage(node);
        var encrypted = CypherUtils.aesEncrypt(encodedMessage, Objects.requireNonNull(whatsappKeys.encKey()));
        var hmacSign = CypherUtils.hmacSha256(encrypted, Objects.requireNonNull(whatsappKeys.macKey()));
        var binaryMessage = messageTag.merged(BinaryArray.forArray(BinaryMetric.toArray(tags)).merged(BinaryArray.singleton(flag.data()))).merged(hmacSign).merged(encrypted).toBuffer();
        if (configuration.async()) {
            session.getAsyncRemote().sendBinary(binaryMessage, __ -> {
                if(isCompletable()) {
                    MANAGER.pendingRequests().add(this);
                    return;
                }

                future.complete(null);
            });

            return this;
        }

        session.getBasicRemote().sendBinary(binaryMessage);
        if(isCompletable()) {
            MANAGER.pendingRequests().add(this);
            return this;
        }

        future.complete(null);
        return this;
    }
}
