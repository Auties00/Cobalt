package it.auties.whatsapp.model.business;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a time a localizable name
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(builderMethodName = "newLocalizedName")
@Jacksonized
@Accessors(fluent = true)
public class BusinessLocalizedName implements ProtobufMessage {
    /**
     * Lg
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String lg;

    /**
     * Lc
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String lc;

    /**
     * The localized name
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String name;
}
