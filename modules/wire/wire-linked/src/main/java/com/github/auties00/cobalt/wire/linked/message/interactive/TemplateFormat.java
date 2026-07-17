package com.github.auties00.cobalt.wire.linked.message.interactive;

/**
 * Represents the concrete layout that a {@link TemplateMessage} uses to present its content.
 *
 * <p>A template message can be delivered in one of three shapes:
 * <ul>
 *   <li>{@link TemplateMessage.FourRowTemplate} the unhydrated four-row template, where text
 *       fields are still {@link com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage}
 *       values containing placeholders</li>
 *   <li>{@link TemplateMessage.HydratedFourRowTemplate} the hydrated four-row template, where
 *       placeholders have been substituted with literal strings ready for display</li>
 *   <li>{@link InteractiveMessage} a richer interactive message that may include carousels,
 *       native flows, shop storefronts and collections</li>
 * </ul>
 *
 * <p>Pattern matching can be used to dispatch rendering logic based on the concrete format
 * returned by {@link TemplateMessage#format()}.
 */
public sealed interface TemplateFormat permits TemplateMessage.FourRowTemplate, TemplateMessage.HydratedFourRowTemplate, InteractiveMessage {
}
