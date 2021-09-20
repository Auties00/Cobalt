package it.auties.whatsapp4j.standard.request;

import it.auties.whatsapp4j.common.api.AbstractWhatsappAPI;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.request.AbstractBinaryRequest;
import it.auties.whatsapp4j.common.response.ResponseModel;
import it.auties.whatsapp4j.standard.binary.BinaryFlag;
import it.auties.whatsapp4j.standard.binary.BinaryMetric;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * An abstract model class that represents a binary request made from the client to the server
 *
 * @param <M> the type of the model
 */
@Accessors(fluent = true, chain = true)
public abstract class BinaryRequest<M extends ResponseModel> extends AbstractBinaryRequest<M> {
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
}
