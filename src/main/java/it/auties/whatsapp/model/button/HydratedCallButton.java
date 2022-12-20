package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a hydrated button that can start a phone call
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public class HydratedCallButton
        implements ProtobufMessage {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String text;

    /**
     * The phone number of this button
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String phoneNumber;
}
