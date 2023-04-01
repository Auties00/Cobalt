package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a native flow button
 */
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class InteractiveButton implements ProtobufMessage {
    /**
     * The name of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    @NonNull
    private String name;

    /**
     * The parameters of this button as json
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String parameters;
}
