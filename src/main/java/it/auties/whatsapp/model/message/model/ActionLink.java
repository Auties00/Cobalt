package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * An action link for a button
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ActionLink {
    /**
     * The url of the action
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String url;

    /**
     * The title of the action
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String buttonTitle;
}
