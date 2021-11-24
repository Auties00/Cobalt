package it.auties.whatsapp.request;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.model.Node;
import it.auties.whatsapp.response.Response;
import it.auties.whatsapp.response.ResponseModel;
import it.auties.whatsapp.utils.WhatsappUtils;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a request made from the client to the server.
 * All of its implementations must be abstract in order for the accessor {@link Request#modelClass()} to work.
 *
 * This class only allows two types of requests:
 * <ul>
 * <li>{@link JsonRequest} - a json encoded response made from a List of Objects</li>
 * <li>{@link AbstractBinaryRequest} - an aes encrypted {@link Node}</li>
 * </ul>
 *
 * @param <B> the type of the body
 * @param <M> the type of the model
 */
@RequiredArgsConstructor
@Accessors(fluent = true, chain = true)
public sealed abstract class Request<B, M extends ResponseModel> permits AbstractBinaryRequest, JsonRequest {
    /**
     * The non-null tag of this request.
     * This tag must be unique even amongst different sessions linked to the same encryption keys after the login process has been completed.
     */
    protected final @NonNull @Getter String tag;

    /**
     * The configuration used for {@link Whatsapp}
     */
    protected final @NonNull WhatsappConfiguration configuration;

    /**
     * A future completed when Whatsapp sends a response
     */
    protected final @NonNull @Getter CompletableFuture<M> future;

    /**
     * Whether this request requires a response
     */
    protected @Getter @Setter boolean noResponse;

    /**
     * Constructs a new instance of a Request using a custom non-null request tag
     *
     * @param tag the custom non-null tag to assign to this request
     * @param configuration the configuration used for {@link Whatsapp}
     */
    protected Request(@NonNull String tag, @NonNull WhatsappConfiguration configuration){
        this(tag, configuration, new CompletableFuture<>());
    }

    /**
     * Constructs a new instance of a Request using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link Whatsapp}
     */
    protected Request(@NonNull WhatsappConfiguration configuration){
        this(WhatsappUtils.buildRequestTag(configuration), configuration);
    }

    /**
     * Returns the body of this request
     *
     * @return an object to send to WhatsappWeb's WebSocket
     */
    public abstract @NonNull B buildBody();

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public abstract @NonNull CompletableFuture<M> send(@NonNull WhatsappStore store, @NonNull Session session);

    /**
     * Completes this request using {@code response}
     *
     * @param response the response used to complete {@link Request#future}
     */
    public void complete(@NonNull Response<?> response){
        var result = response.toModel(modelClass());
        future.complete(result);
    }

    /**
     * Returns a Class representing the type parameter of this object
     * In order for this method to work, the implementations of the implementations of this class must also be abstract and initialized using only concrete types, generics will break this implementation
     * A {@link TypeReference} couldn't have been used in this case as the exact type of the type parameter of this object is needed to effectively convert the response to the model
     * In Kotlin, it's possible to do the following: {@code inline fun <reified T> modelClass(): KClass<T> = T::class}, although, inline functions cannot be accessed from Java
     *
     * @throws ClassCastException if the type parameter of this object isn't a concrete type
     * @return a class representing the type parameter of this object
     */
    @SuppressWarnings("unchecked")
    private @NonNull Class<M> modelClass() throws ClassCastException{
        return (Class<M>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * Adds this request to {@link WhatsappStore#pendingRequests()} if {@link Request#noResponse()} is false
     * Otherwise the future associated with this request is immediately resolved
     *
     * @param store the storer
     */
    protected void addRequest(WhatsappStore store) {
        if(noResponse()){
            future.complete(null);
            return;
        }

        store.pendingRequests()
                .add(this);
    }
}