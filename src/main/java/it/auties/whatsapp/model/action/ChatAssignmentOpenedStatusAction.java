package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model clas that represents the assignment of a chat as opened
 */
@ProtobufMessage(name = "SyncActionValue.ChatAssignmentOpenedStatusAction")
public final class ChatAssignmentOpenedStatusAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean chatOpened;

    ChatAssignmentOpenedStatusAction(boolean chatOpened) {
        this.chatOpened = chatOpened;
    }

    @Override
    public String indexName() {
        return "agentChatAssignmentOpenedStatus";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public boolean chatOpened() {
        return chatOpened;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatAssignmentOpenedStatusAction that
                && chatOpened == that.chatOpened;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(chatOpened);
    }

    @Override
    public String toString() {
        return "ChatAssignmentOpenedStatusAction[" +
                "chatOpened=" + chatOpened + ']';
    }
}
