package it.auties.whatsapp4j.request.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.whatsapp.WhatsappConfiguration;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.NonNull;

import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An abstract model class that represents a json request made from the client to the server
 *
 * @param <M> the type of the model
 */
public abstract non-sealed class JsonRequest<M extends JsonResponseModel> extends Request<List<Object>, M, CharSequence> {
    /**
     * An instance of Jackson's writer
     */
    private static final ObjectWriter JACKSON = new ObjectMapper().writerWithDefaultPrettyPrinter();

    /**
     * Constructs a new instance of a JsonRequest using a custom non null request tag
     *
     * @param tag the custom non null tag to assign to this request
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected JsonRequest(@NonNull String tag, @NonNull WhatsappConfiguration configuration) {
        super(tag, configuration);
    }

    /**
     * Constructs a new instance of a JsonRequest using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected JsonRequest(@NonNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    /**
     * Sends a json request to the WebSocket linked to {@code session}
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     **/
    public @NonNull CompletableFuture<M> send(@NonNull WebSocket session) {
        var request = session.sendText(encode(), true);
        if (configuration.async()) {
            request.whenCompleteAsync((res, ex) -> {
                if(ex != null){
                    throw new RuntimeException("An exception occurred while sending a JSON message to Whatsapp", ex);
                }

                MANAGER.pendingRequests().add(this);
                if(noResponse()) future.complete(null);
            });

            return future();
        }

        try {
            request.get(30, TimeUnit.SECONDS);
            MANAGER.pendingRequests().add(this);
            if(noResponse()) future.complete(null);
            return future();
        } catch (ExecutionException | InterruptedException | TimeoutException ex) {
            throw new RuntimeException("An exception occurred while sending a JSON message to Whatsapp", ex);
        }
    }

    @Override
    public @NonNull CharSequence encode() {
        try {
            return "%s,%s".formatted(tag(), JACKSON.writeValueAsString(buildBody()));
        }catch (JsonProcessingException ex){
            throw new RuntimeException("An exception occurred while encoding a JSON message", ex);
        }
    }
}
