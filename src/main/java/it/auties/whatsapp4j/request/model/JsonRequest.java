package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.response.model.JsonResponse;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import it.auties.whatsapp4j.response.model.Response;
import it.auties.whatsapp4j.utils.Validate;
import jakarta.websocket.Session;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class JsonRequest<M extends JsonResponseModel> extends Request<M> {
    protected JsonRequest(@NotNull WhatsappConfiguration configuration) {
        super(configuration);
    }

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

    @SneakyThrows
    public JsonRequest<M> send(@NotNull Session session) {
        return send(session, null);
    }

    @Override
    public void complete(@NotNull Response response){
        Validate.isTrue(isCompletable(), "WhatsappAPI: Cannot complete a request that isn't completable");
        if(!(response instanceof JsonResponse jsonResponse)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot complete request: expected JsonResponse, got %s".formatted(response.getClass().getName()));
        }

        future.complete(jsonResponse.toModel(modelClass()));
    }
}
