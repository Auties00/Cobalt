package it.auties.whatsapp.model.info;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that holds the information related to a Whatsapp call.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class CallInfo
        implements Info {
    /**
     * The key of this call
     */
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] key;

    /**
     * The source of this call
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String source;

    /**
     * The data of this call
     */
    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] data;

    /**
     * The delay of this call in endTimeStamp
     */
    @ProtobufProperty(index = 4, type = UINT32)
    private int delay;
}
