package it.auties.whatsapp4j.request.model;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.Response;
import it.auties.whatsapp4j.response.model.ResponseModel;
import it.auties.whatsapp4j.utils.Validate;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a request made from the client to the server.
 * All of its implementations must be abstract in order for the accessor {@link Request#modelClass()} to work.
 *
 * This class only allows two types of requests:
 * <ul>
 * <li>{@link JsonRequest} - a json encoded response made from a List of Objects</li>
 * <li>{@link BinaryRequest} - an aes encrypted {@link WhatsappNode}</li>
 * </ul>
 *
 * @param <M>
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
public sealed abstract class Request<M extends ResponseModel> permits BinaryRequest, JsonRequest {
    /**
     * A singleton instance of WhatsappDataManager
     */
    protected static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    /**
     * The non null tag of this request.
     * This tag must be unique even amongst different sessions linked to the same encryption keys after the login process has been completed.
     */
    protected final @NotNull @Getter String tag;


    /**
     * The configuration used for {@link WhatsappAPI}
     */
    protected final @NotNull WhatsappConfiguration configuration;


    /**
     * A future completed when Whatsapp sends a response if {@link Request#isCompletable()} returns true.
     * Otherwise, it's completed as soon as the request is successfully sent to WhatsappWeb's WebSocket.
     */
    protected final @NotNull @Getter CompletableFuture<M> future;


    /**
     * Constructs a new instance of a Request using a custom non null request tag
     *
     * @param tag the custom non null tag to assign to this request
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected Request(@NotNull String tag, @NotNull WhatsappConfiguration configuration){
        this(tag, configuration, new CompletableFuture<>());
    }

    /**
     * Constructs a new instance of a Request using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     */
    protected Request(@NotNull WhatsappConfiguration configuration){
        this(WhatsappUtils.buildRequestTag(configuration), configuration);
    }

    /**
     * Returns the body of this request
     * For json requests, this should be a List of objects
     * For binary requests, it should be a WhatsappNode
     *
     * @return an object to send to WhatsappWeb's WebSocket
     */
    public abstract @NotNull Object buildBody();

    /**
     * Returns whether this request is completable or not
     *
     * @return true if the request is completable
     */
    public boolean isCompletable() {
        return false;
    }

    /**
     * Completes this request using {@code response}
     *
     * @param response the response used to complete {@link Request#future}
     * @throws IllegalArgumentException if this request isn't completable
     * @throws ClassCastException if the type parameter of this object is not a concrete type, the reason is explained here {@link Request#modelClass()}
     */
    public void complete(@NotNull Response response){
        Validate.isTrue(isCompletable(), "WhatsappAPI: Cannot complete a request with tag %s: this request is marked as non completable", tag());
        future.completeAsync(() -> response.toModel(modelClass()));
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
    private @NotNull Class<M> modelClass() throws ClassCastException{
        return (Class<M>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
