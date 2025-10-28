package com.github.auties00.cobalt.model.preferences;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

@ProtobufMessage
public final class QuickReply {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String shortcut;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String message;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> keywords;

    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int count;

    QuickReply(String shortcut, String message, List<String> keywords, int count) {
        this.shortcut = shortcut;
        this.message = message;
        this.keywords = keywords;
        this.count = count;
    }

    public String shortcut() {
        return shortcut;
    }

    public String message() {
        return message;
    }

    public List<String> keywords() {
        return keywords;
    }

    public int count() {
        return count;
    }
}
