package it.auties.whatsapp.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.response.JsonResponseModel;
import it.auties.whatsapp.utils.WhatsappUtils;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import lombok.NonNull;
import org.bouncycastle.util.Store;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a json request made from the client to the server
 *
 * @param <M> the type of the model
 */
public abstract non-sealed class JsonRequest<M extends JsonResponseModel> extends Request<List<Object>, M> {
    /**
     * An instance of Jackson's writer
     */
    private static final ObjectWriter JACKSON = new ObjectMapper().writerWithDefaultPrettyPrinter();

    /**
     * Constructs a new instance of a JsonRequest using a custom non-null request tag
     *
     * @param tag the custom non-null tag to assign to this request
     * @param configuration the configuration used for {@link Whatsapp}
     */
    protected JsonRequest(String tag, WhatsappConfiguration configuration){
        super(tag, configuration, new CompletableFuture<>());
    }

    /**
     * Constructs a new instance of a JsonRequest using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link Whatsapp}
     */
    protected JsonRequest(WhatsappConfiguration configuration){
        super(WhatsappUtils.buildRequestTag(configuration), configuration);
    }

    /**
     * Sends a json request to the WebSocket linked to {@code session}
     *
     * @param store the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     **/
    @Override
    public CompletableFuture<M> send(@NonNull WhatsappStore store, @NonNull Session session) {
        try {
            var body = buildBody();
            var json = JACKSON.writeValueAsString(body);
            var request = "%s,%s".formatted(tag, json);
            if (configuration.async()) {
                session.getAsyncRemote().sendObject(request, __ -> addRequest(store));
                return future();
            }

            session.getBasicRemote().sendObject(request);
            addRequest(store);
            return future();
        }catch (IOException exception){
            throw new RuntimeException("An exception occurred while sending a JSON message", exception);
        }catch (EncodeException exception){
            throw new RuntimeException("An exception occurred while encoding a JSON message", exception);
        }
    }
}
