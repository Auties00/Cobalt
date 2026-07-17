package com.github.auties00.cobalt.wire.linked.message.interactive;

/**
 * Represents the concrete payload that fills the body of an {@link InteractiveMessage}.
 *
 * <p>An interactive message contains exactly one content variant, selected from this sealed
 * hierarchy. Each permitted implementation corresponds to a distinct interactive experience:
 * <ul>
 *   <li>{@link InteractiveMessage.ShopMessage} opens a business storefront on Facebook,
 *       Instagram or WhatsApp</li>
 *   <li>{@link InteractiveMessage.CollectionMessage} references a product collection owned
 *       by a WhatsApp business account</li>
 *   <li>{@link InteractiveMessage.NativeFlowMessage} hosts native flow buttons that trigger
 *       client-side JSON payload actions</li>
 *   <li>{@link InteractiveMessage.CarouselMessage} displays multiple interactive cards as a
 *       horizontally scrollable carousel</li>
 * </ul>
 *
 * <p>Use pattern matching to dispatch handling based on the concrete content type returned
 * by {@link InteractiveMessage#content()}.
 */
public sealed interface InteractiveMessageContent permits InteractiveMessage.ShopMessage, InteractiveMessage.CollectionMessage, InteractiveMessage.NativeFlowMessage, InteractiveMessage.CarouselMessage {
}
