package it.auties.whatsapp4j.request.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.response.model.Response;
import it.auties.whatsapp4j.response.model.ResponseModel;
import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Accessors(fluent = true)
public abstract class Request<M extends ResponseModel> {
    protected static final ObjectWriter JACKSON = new ObjectMapper().writerWithDefaultPrettyPrinter();
    protected static final BinaryEncoder ENCODER = new BinaryEncoder();
    protected static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    protected final @NotNull @Getter String tag;
    protected final @NotNull WhatsappConfiguration configuration;
    protected final @NotNull @Getter CompletableFuture<M> future;
    protected Request(@NotNull String tag, @NotNull WhatsappConfiguration configuration){
        this(tag, configuration, new CompletableFuture<>());
    }

    protected Request(@NotNull WhatsappConfiguration configuration){
        this(WhatsappUtils.buildRequestTag(configuration), configuration);
    }

    public abstract @NotNull Object buildBody();
    public abstract void complete(@NotNull Response response);
    public boolean isCompletable() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public @NotNull Class<M> modelClass(){
        return (Class<M>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
