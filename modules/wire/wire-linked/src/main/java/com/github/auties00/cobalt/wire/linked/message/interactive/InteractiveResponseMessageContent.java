package com.github.auties00.cobalt.wire.linked.message.interactive;

/**
 * Represents the concrete payload that fills the body of an {@link InteractiveResponseMessage}.
 *
 * <p>When a recipient interacts with a native flow button inside an {@link InteractiveMessage},
 * their client replies with an {@code InteractiveResponseMessage} whose content is one of the
 * variants in this sealed hierarchy. At present only
 * {@link InteractiveResponseMessage.NativeFlowResponseMessage} is defined, carrying the flow
 * name, serialized parameters and message version of the response.
 */
public sealed interface InteractiveResponseMessageContent permits InteractiveResponseMessage.NativeFlowResponseMessage {
}
