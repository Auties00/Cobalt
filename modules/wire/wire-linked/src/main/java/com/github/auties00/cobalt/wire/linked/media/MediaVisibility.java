package com.github.auties00.cobalt.wire.linked.media;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * A setting that controls whether incoming media attachments are automatically
 * downloaded and made visible in a conversation.
 *
 * <p>This setting is honored at two levels: an application-wide default
 * configured in global settings, and an optional per-conversation override
 * applied to individual chats. When a conversation uses {@link #DEFAULT}, the
 * client falls back to the application-wide value. Setting a conversation to
 * {@link #OFF} hides media attachments and skips automatic downloads even when
 * the global default is {@link #ON}.
 */
@ProtobufEnum(name = "MediaVisibility")
public enum MediaVisibility {
    /**
     * Inherits the application-wide setting.
     *
     * <p>Applied at the conversation level; when used, the global default
     * determines the effective behavior.
     *
     * <p>Numeric value {@code 0}.
     */
    DEFAULT(0),

    /**
     * Hides media attachments. Media is not automatically downloaded or
     * displayed in the conversation.
     *
     * <p>Numeric value {@code 1}.
     */
    OFF(1),

    /**
     * Shows media attachments. Media is automatically downloaded and displayed
     * in the conversation.
     *
     * <p>Numeric value {@code 2}.
     */
    ON(2);

    /**
     * Constructs a new {@code MediaVisibility} with the given protobuf index.
     *
     * @param index the protobuf enum index
     */
    MediaVisibility(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf enum index backing this visibility setting.
     */
    final int index;

    /**
     * Returns the protobuf enum index of this visibility setting.
     *
     * @return the numeric index
     */
    public int index() {
        return this.index;
    }
}
