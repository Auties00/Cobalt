package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents the assignment of a chat
 */
@ProtobufMessage(name = "SyncActionValue.ChatAssignmentAction")
public final class ChatAssignmentAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String deviceAgentId;

    ChatAssignmentAction(String deviceAgentId) {
        this.deviceAgentId = deviceAgentId;
    }

    @Override
    public String indexName() {
        return "agentChatAssignment";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public Optional<String> deviceAgentId() {
        return Optional.ofNullable(deviceAgentId);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatAssignmentAction that
                && Objects.equals(deviceAgentId, that.deviceAgentId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceAgentId);
    }

    @Override
    public String toString() {
        return "ChatAssignmentAction[" +
                "deviceAgentId=" + deviceAgentId + ']';
    }
}
