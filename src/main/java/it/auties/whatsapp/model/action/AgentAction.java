package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model clas that represents an agent
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("AgentAction")
public final class AgentAction implements Action {
    /**
     * The agent's name
     */
    @ProtobufProperty(index = 1, name = "name", type = STRING)
    private String name;

    /**
     * The agent's device id
     */
    @ProtobufProperty(index = 2, name = "deviceID", type = INT32)
    private int deviceId;

    /**
     * Whether the agent was deleted
     */
    @ProtobufProperty(index = 3, name = "isDeleted", type = BOOL)
    private boolean deleted;

    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send action: no index name");
    }
}
