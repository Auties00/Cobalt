package com.github.auties00.cobalt.wire.linked.message.media;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Preferred render mode for links shown inside a message.
 *
 * <p>When tapping a link preview, WhatsApp can either open the destination inside
 * an in-app web view or hand it off to the system default browser. This enum carries
 * the rendering preference requested by the sender.
 */
@ProtobufEnum(name = "WebLinkRenderConfig")
public enum MessageLinkRender {
    /**
     * Render the link inside an in-app web view.
     */
    WEBVIEW(0),
    /**
     * Hand the link off to the operating system default browser.
     */
    SYSTEM(1);

    /**
     * Constructs a new enum constant.
     *
     * @param index the protobuf wire index used to serialize this constant
     */
    MessageLinkRender(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Protobuf wire index of this constant.
     */
    final int index;

    /**
     * Returns the protobuf wire index of this constant.
     *
     * @return the index
     */
    public int index() {
        return this.index;
    }
}
