package com.github.auties00.cobalt.wire.linked.message.interactive;

/**
 * Represents one of the concrete variants that a template button can assume before
 * hydration.
 *
 * <p>A template button is the author-defined form of an interactive button. Unlike the
 * hydrated counterpart, the display text of each variant is a
 * {@link com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage} which may
 * contain placeholders that are resolved server-side into a
 * {@link HydratedButtonVariant} before delivery.
 *
 * <p>The three supported variants are:
 * <ul>
 *   <li>{@link TemplateButton.QuickReplyButton} sends a quick reply with a predefined payload
 *       identifier</li>
 *   <li>{@link TemplateButton.URLButton} opens a URL built from a structured template</li>
 *   <li>{@link TemplateButton.CallButton} initiates a phone call using a structured phone
 *       number template</li>
 * </ul>
 */
public sealed interface TemplateButtonVariant permits TemplateButton.QuickReplyButton, TemplateButton.URLButton, TemplateButton.CallButton {
}
