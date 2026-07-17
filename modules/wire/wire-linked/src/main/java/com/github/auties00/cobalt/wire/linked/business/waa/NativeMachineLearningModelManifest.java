package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Download manifest for a batch of on-device machine-learning models.
 *
 * <p>WhatsApp ships some features that run a machine-learning model directly on
 * the device (for example sticker classification). Before the model can run the
 * client resolves the requested model names and versions, given its own decode
 * capabilities, into the list of files it must download and the manifest
 * metadata describing the batch.
 *
 * <p>This model is that resolved batch: the per-model entries
 * ({@linkplain NativeMachineLearningModel models}) carrying their downloadable
 * assets and free-form properties, the batch-level entry-point module
 * identifier, the asset and model counts the server reports, and the batch
 * status markers.
 */
@ProtobufMessage(name = "NativeMachineLearningModelManifest")
public final class NativeMachineLearningModelManifest {
    /**
     * Resolved models, in the order the server returned them. Never
     * {@code null}, possibly empty when the server returned none.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<NativeMachineLearningModel> models;

    /**
     * Manifest entry-point module identifier. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String entryPoint;

    /**
     * Total number of downloadable assets across all resolved models, as the
     * server reports it. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final Long assetCount;

    /**
     * Total number of resolved models, as the server reports it. {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final Long modelCount;

    /**
     * Batch status marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String status;

    /**
     * Batch status detail marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String statusDetails;

    /**
     * Constructs a new {@code NativeMachineLearningModelManifest}. A
     * {@code null} {@code models} is coerced to an empty list, and the
     * reference scalar arguments may be {@code null} when the server omitted
     * them.
     *
     * @param models        the resolved models; {@code null} treated as empty
     * @param entryPoint    the entry-point module identifier, or {@code null}
     * @param assetCount    the total asset count, or {@code null}
     * @param modelCount    the total model count, or {@code null}
     * @param status        the batch status marker, or {@code null}
     * @param statusDetails the batch status detail marker, or {@code null}
     */
    NativeMachineLearningModelManifest(List<NativeMachineLearningModel> models,
                                       String entryPoint,
                                       Long assetCount,
                                       Long modelCount,
                                       String status,
                                       String statusDetails) {
        this.models = models == null ? List.of() : models;
        this.entryPoint = entryPoint;
        this.assetCount = assetCount;
        this.modelCount = modelCount;
        this.status = status;
        this.statusDetails = statusDetails;
    }

    /**
     * Returns the resolved models.
     *
     * @return an unmodifiable view of the resolved models; never {@code null},
     *         possibly empty
     */
    public List<NativeMachineLearningModel> models() {
        return Collections.unmodifiableList(models);
    }

    /**
     * Returns the manifest entry-point module identifier.
     *
     * @return the entry-point identifier, or empty when the server omitted it
     */
    public Optional<String> entryPoint() {
        return Optional.ofNullable(entryPoint);
    }

    /**
     * Returns the total number of downloadable assets across all resolved
     * models.
     *
     * @return the asset count, or empty when the server omitted it
     */
    public OptionalLong assetCount() {
        return assetCount == null ? OptionalLong.empty() : OptionalLong.of(assetCount);
    }

    /**
     * Returns the total number of resolved models.
     *
     * @return the model count, or empty when the server omitted it
     */
    public OptionalLong modelCount() {
        return modelCount == null ? OptionalLong.empty() : OptionalLong.of(modelCount);
    }

    /**
     * Returns the batch status marker.
     *
     * @return the status marker, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the batch status detail marker.
     *
     * @return the status detail marker, or empty when the server omitted it
     */
    public Optional<String> statusDetails() {
        return Optional.ofNullable(statusDetails);
    }
}
