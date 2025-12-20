package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "Message.RequestWelcomeMessageMetadata")
public final class RequestWelcomeMessageMetadata {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final LocalChatState localChatState;

    public RequestWelcomeMessageMetadata(LocalChatState localChatState) {
        this.localChatState = localChatState;
    }

    public Optional<LocalChatState> localChatState() {
        return Optional.ofNullable(localChatState);
    }

    @ProtobufEnum(name = "Message.RequestWelcomeMessageMetadata.LocalChatState")
    public enum LocalChatState {
        EMPTY(0),
        NON_EMPTY(1);

        final int index;

        LocalChatState(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
