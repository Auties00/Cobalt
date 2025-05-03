package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents an agent
 */
@ProtobufMessage(name = "SyncActionValue.AgentAction")
public final class AgentAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final int deviceId;

    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean deleted;

    AgentAction(
            String name,
            int deviceId,
            boolean deleted
    ) {
        this.name = name;
        this.deviceId = deviceId;
        this.deleted = deleted;
    }

    @Override
    public String indexName() {
        return "deviceAgent";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public int deviceId() {
        return deviceId;
    }

    public boolean deleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AgentAction that
                && deviceId == that.deviceId
                && deleted == that.deleted
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, deviceId, deleted);
    }

    @Override
    public String toString() {
        return "AgentAction[" +
                "name=" + name + ", " +
                "deviceId=" + deviceId + ", " +
                "deleted=" + deleted + ']';
    }
}
