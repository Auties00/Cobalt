package com.github.auties00.cobalt.wire.linked.message.interactive;

/**
 * Represents one of the concrete variants that a hydrated template button can assume.
 *
 * <p>A hydrated template button is the final, user-ready form of a template button where all
 * placeholders have been resolved into literal values. This sealed interface enumerates the
 * three supported kinds of action a recipient can perform when tapping the button:
 * <ul>
 *   <li>{@link HydratedTemplateButton.HydratedQuickReplyButton} sends a quick reply with a
 *       predefined payload identifier</li>
 *   <li>{@link HydratedTemplateButton.HydratedURLButton} opens a URL, optionally inside an
 *       in-app web view</li>
 *   <li>{@link HydratedTemplateButton.HydratedCallButton} initiates a phone call to the
 *       provided number</li>
 * </ul>
 *
 * <p>Pattern matching can be used to dispatch behaviour based on the concrete variant held
 * by a {@link HydratedTemplateButton}.
 */
public sealed interface HydratedButtonVariant permits HydratedTemplateButton.HydratedQuickReplyButton, HydratedTemplateButton.HydratedURLButton, HydratedTemplateButton.HydratedCallButton {
}
