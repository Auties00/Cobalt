package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata about the AI model used to generate a bot response on WhatsApp.
 *
 * <p>This message identifies which large-language model variant was used to
 * produce the response, whether the premium (higher-quality) model tier is
 * available to the user, and an optional display-name override for the
 * model branding shown in the UI (for example {@code "Meta Llama 4"}).
 *
 * <p>This metadata is carried inside
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata#modelMetadata()}.
 */
@ProtobufMessage(name = "BotModelMetadata")
public final class BotModelMetadata {
    /**
     * The type of AI model used to generate the response.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    ModelType modelType;

    /**
     * The availability status of the premium model tier for this user.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    PremiumModelStatus premiumModelStatus;

    /**
     * An optional display-name override for the model shown in the UI,
     * for example {@code "Meta Llama 4"}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String modelNameOverride;


    /**
     * Constructs a new {@code BotModelMetadata} with the specified values.
     *
     * @param modelType          the model type, or {@code null}
     * @param premiumModelStatus the premium model status, or {@code null}
     * @param modelNameOverride  the display-name override, or {@code null}
     */
    BotModelMetadata(ModelType modelType, PremiumModelStatus premiumModelStatus, String modelNameOverride) {
        this.modelType = modelType;
        this.premiumModelStatus = premiumModelStatus;
        this.modelNameOverride = modelNameOverride;
    }

    /**
     * Returns the type of AI model used to generate the response.
     *
     * @return an {@code Optional} describing the model type, or an empty
     *         {@code Optional} if not set
     */
    public Optional<ModelType> modelType() {
        return Optional.ofNullable(modelType);
    }

    /**
     * Returns the availability status of the premium model tier.
     *
     * @return an {@code Optional} describing the premium model status, or an
     *         empty {@code Optional} if not set
     */
    public Optional<PremiumModelStatus> premiumModelStatus() {
        return Optional.ofNullable(premiumModelStatus);
    }

    /**
     * Returns the display-name override for the model branding.
     *
     * @return an {@code Optional} describing the name override, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> modelNameOverride() {
        return Optional.ofNullable(modelNameOverride);
    }

    /**
     * Sets the type of AI model used to generate the response.
     *
     * @param modelType the new model type, or {@code null}
     */
    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    /**
     * Sets the availability status of the premium model tier.
     *
     * @param premiumModelStatus the new premium model status, or {@code null}
     */
    public void setPremiumModelStatus(PremiumModelStatus premiumModelStatus) {
        this.premiumModelStatus = premiumModelStatus;
    }

    /**
     * Sets the display-name override for the model branding.
     *
     * @param modelNameOverride the new name override, or {@code null}
     */
    public void setModelNameOverride(String modelNameOverride) {
        this.modelNameOverride = modelNameOverride;
    }

    /**
     * Enumerates the variants of large-language model used by the WhatsApp
     * AI bot to generate responses.
     */
    @ProtobufEnum(name = "BotModelMetadata.ModelType")
    public static enum ModelType {
        /**
         * The model type is not known or not specified.
         */
        UNKNOWN_TYPE(0),

        /**
         * The standard production Llama model.
         */
        LLAMA_PROD(1),

        /**
         * The premium production Llama model, offering higher-quality
         * responses at the cost of increased quota consumption.
         */
        LLAMA_PROD_PREMIUM(2);

        /**
         * Constructs a new model type constant with the specified protobuf
         * index.
         *
         * @param index the protobuf enum index
         */
        ModelType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        /**
         * Returns the protobuf enum index of this model type.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates the availability states of the premium AI model tier for
     * the current user.
     */
    @ProtobufEnum(name = "BotModelMetadata.PremiumModelStatus")
    public static enum PremiumModelStatus {
        /**
         * The premium model status is not known or not specified.
         */
        UNKNOWN_STATUS(0),

        /**
         * The premium model is available and the user has remaining quota.
         */
        AVAILABLE(1),

        /**
         * The user has exceeded their premium model usage quota.
         */
        QUOTA_EXCEED_LIMIT(2);

        /**
         * Constructs a new premium model status constant with the specified
         * protobuf index.
         *
         * @param index the protobuf enum index
         */
        PremiumModelStatus(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        /**
         * Returns the protobuf enum index of this premium model status.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
