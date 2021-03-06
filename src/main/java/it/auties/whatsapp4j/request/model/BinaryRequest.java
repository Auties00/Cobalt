package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.response.model.shared.Response;
import it.auties.whatsapp4j.response.model.shared.ResponseModel;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.utils.CypherUtils;
import it.auties.whatsapp4j.utils.Validate;
import it.auties.whatsapp4j.model.WhatsappNode;
import jakarta.websocket.Session;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class BinaryRequest<M extends ResponseModel> extends Request<M> {
    protected BinaryRequest(@NotNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    protected BinaryRequest(@NotNull String tag, @NotNull WhatsappConfiguration configuration) {
        super(tag, configuration);
    }

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

    @Override
    public void complete(@NotNull Response response){
        Validate.isTrue(isCompletable(), "WhatsappAPI: Cannot complete a request that isn't completable");
        System.out.printf("Completing %s with %s%n", tag, response);
        if (response instanceof BinaryResponse binaryResponse) {
            future.complete(binaryResponse.toModel(modelClass()));
            return;
        }

        if (response instanceof JsonResponse jsonResponseModel) {
            future.complete(jsonResponseModel.toModel(modelClass()));
            return;
        }

        throw new IllegalArgumentException("WhatsappAPI: Cannot complete request: expected BinaryResponse or JsonResponse, got %s".formatted(response.getClass().getName()));
    }
}
