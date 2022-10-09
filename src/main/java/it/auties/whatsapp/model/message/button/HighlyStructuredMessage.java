package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.business.BusinessLocalizableParameter;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a message that contains a highly structured message inside.
 * Not really clear how this could be used, contributions are welcomed.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder(builderMethodName = "newHighlyStructuredMessageBuilder")
@Accessors(fluent = true)
public final class HighlyStructuredMessage implements ButtonMessage {
    /**
     * Namespace
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String namespace;

    /**
     * Element Name
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String elementName;

    /**
     * Params
     */
    @ProtobufProperty(index = 3, type = STRING, repeated = true)
    private List<String> params;

    /**
     * FallbackLg
     */
    @ProtobufProperty(index = 4, type = STRING)
    private String fallbackLg;

    /**
     * FallbackLc
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String fallbackLc;

    /**
     * Localizable Params
     */
    @ProtobufProperty(index = 6, type = MESSAGE, implementation = BusinessLocalizableParameter.class, repeated = true)
    private List<BusinessLocalizableParameter> localizableParameters;

    /**
     * DeterministicLg
     */
    @ProtobufProperty(index = 7, type = STRING)
    private String deterministicLg;

    /**
     * DeterministicLc
     */
    @ProtobufProperty(index = 8, type = STRING)
    private String deterministicLc;

    /**
     * Hydrated message
     */
    @ProtobufProperty(index = 9, type = MESSAGE, implementation = TemplateMessage.class)
    private TemplateMessage templateMessage;

    @Override
    public MessageType type() {
        return MessageType.HIGHLY_STRUCTURED;
    }

    public static class HighlyStructuredMessageBuilder {
        public HighlyStructuredMessageBuilder params(List<String> params) {
            if (this.params == null)
                this.params = new ArrayList<>();
            this.params.addAll(params);
            return this;
        }

        public HighlyStructuredMessageBuilder localizableParameters(
                List<BusinessLocalizableParameter> localizableParams) {
            if (this.localizableParameters == null)
                this.localizableParameters = new ArrayList<>();
            this.localizableParameters.addAll(localizableParams);
            return this;
        }
    }
}
