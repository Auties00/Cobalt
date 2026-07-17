package com.github.auties00.cobalt.wire.cloud.template;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A message-template component change, decoded from a {@code message_template_components_update}
 * webhook change.
 *
 * <p>The platform reports the current rendered components of an approved template whenever its body,
 * header, footer, or buttons are edited, so a business can mirror the live template content without
 * re-querying the template node. The change always carries the template's id, name, language, and
 * body text; the header title, footer, and buttons are reported only when the template defines them.
 */
public final class CloudTemplateComponentsUpdate {
    /**
     * The server-assigned template id.
     */
    private final String templateId;

    /**
     * The template name.
     */
    private final String name;

    /**
     * The BCP-47 language code of the template.
     */
    private final String language;

    /**
     * The body text of the template.
     */
    private final String body;

    /**
     * The text header title, or {@code null} when the template has no text header.
     */
    private final String title;

    /**
     * The footer text, or {@code null} when the template has no footer.
     */
    private final String footer;

    /**
     * The template buttons, empty when the template defines none.
     */
    private final List<Button> buttons;

    /**
     * Constructs a new template components update.
     *
     * @param templateId the server-assigned template id
     * @param name       the template name
     * @param language   the BCP-47 language code
     * @param body       the body text of the template
     * @param title      the text header title, or {@code null}
     * @param footer     the footer text, or {@code null}
     * @param buttons    the template buttons, or {@code null} for none
     * @throws NullPointerException if {@code templateId}, {@code name}, {@code language}, or
     *                              {@code body} is {@code null}
     */
    public CloudTemplateComponentsUpdate(String templateId, String name, String language, String body,
                                         String title, String footer, List<Button> buttons) {
        this.templateId = Objects.requireNonNull(templateId, "templateId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.language = Objects.requireNonNull(language, "language must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.title = title;
        this.footer = footer;
        this.buttons = buttons == null ? List.of() : List.copyOf(buttons);
    }

    /**
     * Returns the server-assigned template id.
     *
     * @return the template id
     */
    public String templateId() {
        return templateId;
    }

    /**
     * Returns the template name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the BCP-47 language code of the template.
     *
     * @return the language code
     */
    public String language() {
        return language;
    }

    /**
     * Returns the body text of the template.
     *
     * @return the body text
     */
    public String body() {
        return body;
    }

    /**
     * Returns the text header title.
     *
     * @return an {@link Optional} carrying the title, or empty when the template has no text header
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the footer text.
     *
     * @return an {@link Optional} carrying the footer, or empty when the template has no footer
     */
    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }

    /**
     * Returns the template buttons.
     *
     * @return the buttons, empty when the template defines none
     */
    public List<Button> buttons() {
        return buttons;
    }

    /**
     * A single button reported by a {@link CloudTemplateComponentsUpdate}.
     *
     * <p>This is a closed union over the button kinds a rendered template exposes: a {@link Url} opens a
     * link, a {@link PhoneNumber} dials a number, and a {@link QuickReply} sends a fixed reply payload.
     * Pattern matching on the variant recovers the kind-specific value; every variant carries the button
     * {@linkplain #text() label}. The {@link #of(String, String, String, String) of} factory selects the
     * variant from the wire button type so an unrecognised type resolves to a {@link QuickReply}.
     */
    public sealed interface Button permits Button.QuickReply, Button.Url, Button.PhoneNumber {
        /**
         * Returns the button label.
         *
         * @return the label
         */
        String text();

        /**
         * Returns the button matching the given wire button type and values.
         *
         * <p>A {@code URL} type yields a {@link Url} carrying {@code url}, a {@code PHONE_NUMBER} type
         * yields a {@link PhoneNumber} carrying {@code phoneNumber}, and any other type (including
         * {@code QUICK_REPLY}) yields a {@link QuickReply}.
         *
         * @param type        the wire button type, for example {@code "URL"}
         * @param text        the button label
         * @param url         the URL for a URL button, or {@code null} otherwise
         * @param phoneNumber the phone number for a phone-number button, or {@code null} otherwise
         * @return the matching button variant
         * @throws NullPointerException if {@code type} or {@code text} is {@code null}, or if the
         *                              selected variant's value is {@code null}
         */
        static Button of(String type, String text, String url, String phoneNumber) {
            Objects.requireNonNull(type, "type must not be null");
            return switch (type) {
                case "URL" -> new Url(text, url);
                case "PHONE_NUMBER" -> new PhoneNumber(text, phoneNumber);
                default -> new QuickReply(text);
            };
        }

        /**
         * A button that sends a fixed quick-reply payload back to the business.
         *
         * <p>The button shows its {@link #text() label} and, when tapped, sends that label back as an
         * inbound message.
         */
        final class QuickReply implements Button {
            /**
             * The button label, also the reply payload sent when the button is tapped.
             */
            private final String text;

            /**
             * Constructs a new quick-reply button.
             *
             * @param text the button label and reply payload
             * @throws NullPointerException if {@code text} is {@code null}
             */
            public QuickReply(String text) {
                this.text = Objects.requireNonNull(text, "text must not be null");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String text() {
                return text;
            }
        }

        /**
         * A button that opens a URL.
         *
         * <p>The button shows its {@link #text() label} and opens {@link #url() url} when tapped.
         */
        final class Url implements Button {
            /**
             * The button label.
             */
            private final String text;

            /**
             * The URL the button opens.
             */
            private final String url;

            /**
             * Constructs a new URL button.
             *
             * @param text the button label
             * @param url  the URL the button opens
             * @throws NullPointerException if {@code text} or {@code url} is {@code null}
             */
            public Url(String text, String url) {
                this.text = Objects.requireNonNull(text, "text must not be null");
                this.url = Objects.requireNonNull(url, "url must not be null");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String text() {
                return text;
            }

            /**
             * Returns the URL the button opens.
             *
             * @return the URL
             */
            public String url() {
                return url;
            }
        }

        /**
         * A button that dials a phone number.
         *
         * <p>The button shows its {@link #text() label} and dials {@link #phoneNumber() phoneNumber}
         * when tapped.
         */
        final class PhoneNumber implements Button {
            /**
             * The button label.
             */
            private final String text;

            /**
             * The phone number the button dials.
             */
            private final String phoneNumber;

            /**
             * Constructs a new phone-number button.
             *
             * @param text        the button label
             * @param phoneNumber the phone number the button dials
             * @throws NullPointerException if {@code text} or {@code phoneNumber} is {@code null}
             */
            public PhoneNumber(String text, String phoneNumber) {
                this.text = Objects.requireNonNull(text, "text must not be null");
                this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String text() {
                return text;
            }

            /**
             * Returns the phone number the button dials.
             *
             * @return the phone number
             */
            public String phoneNumber() {
                return phoneNumber;
            }
        }
    }
}
