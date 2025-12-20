package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Payload containing LID migration mappings received from the primary device.
 * <p>
 * This payload is sent during the LID migration process and contains:
 * <ul>
 *     <li>A list of phone number to LID mappings</li>
 *     <li>A timestamp indicating when the chat database migration should occur</li>
 * </ul>
 * <p>
 * The payload is typically delivered as a gzipped protobuf message.
 */
@ProtobufMessage(name = "LIDMigrationMappingSyncPayload")
public record LIDMigrationMappingSyncPayload(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<LIDMigrationMapping> pnToLidMappings,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long chatDbMigrationTimestamp
) {

}
