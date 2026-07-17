package com.github.auties00.cobalt.wire.core.message;

/**
 * Transport-agnostic root marker for any content that can ride inside a message envelope.
 *
 * <p>Both transports carry message content, but the concrete content models differ: the Linked
 * transport uses the protobuf {@code Message} content oneof and its many variants, while the Cloud
 * transport uses its own JSON-shaped bodies. This marker is the common supertype that lets a
 * transport-agnostic message envelope expose its content without naming either transport's concrete
 * hierarchy, so a Cloud envelope can hold Cloud content and a Linked envelope can hold the Linked
 * protobuf content behind the same accessor.
 *
 * <p>The interface is intentionally empty and non-sealed: its implementors live in the downstream
 * transport modules, which the module system does not permit a sealed hierarchy to span.
 */
public interface MessageContent {
}
