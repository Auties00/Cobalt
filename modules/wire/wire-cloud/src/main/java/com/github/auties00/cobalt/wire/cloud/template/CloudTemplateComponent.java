package com.github.auties00.cobalt.wire.cloud.template;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One component of a WhatsApp Cloud API message-template definition.
 *
 * <p>A template is defined by an ordered list of these components. Each component is a distinct kind
 * selected by the concrete variant, so this is a sealed hierarchy rather than one wide type: a
 * {@link Header} renders a headline or media at the top, a {@link Body} renders the main text, a
 * {@link Footer} renders a short trailing line, a {@link Buttons} renders the tappable buttons, and a
 * {@link Carousel} renders a horizontally scrollable set of cards. Matching on the sealed type recovers
 * the variant-specific fields.
 *
 * <p>These components are caller-built request data passed inside a {@link CloudMessageTemplate} to
 * {@code CloudWhatsAppClient.createMessageTemplate} and read back from the management view of an existing
 * template. The scalar variants ({@link Header}, {@link Body}, {@link Footer}) are constructed through
 * their generated builders; the list-carrying variants ({@link Buttons}, {@link Carousel}) are constructed
 * through their public constructors because they carry a list of the sealed types, which has no single
 * protobuf wire form.
 */
public sealed interface CloudTemplateComponent permits CloudTemplateComponent.Header,
        CloudTemplateComponent.Body, CloudTemplateComponent.Footer, CloudTemplateComponent.Buttons,
        CloudTemplateComponent.Carousel {
    /**
     * The header component of a template, rendered at the top.
     *
     * <p>A header carries a {@link #format() format} selecting whether it shows text or media. A
     * {@link CloudTemplateHeaderFormat#TEXT} header carries its {@link #text() text}; the media and
     * location formats carry no inline text. The {@link #example() example} supplies a sample value that
     * drives the template-review preview.
     */
    @ProtobufMessage
    final class Header implements CloudTemplateComponent {
        /**
         * The header format selecting text or media rendering.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        final CloudTemplateHeaderFormat format;

        /**
         * The header text for a {@link CloudTemplateHeaderFormat#TEXT} header, or {@code null} for a
         * media or location header.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String text;

        /**
         * The sample value shown in the template-review preview, or {@code null} when none is supplied.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String example;

        /**
         * Constructs a new header component.
         *
         * @param format  the header format selecting text or media rendering
         * @param text    the header text for a text header, or {@code null}
         * @param example the sample value for the review preview, or {@code null}
         * @throws NullPointerException if {@code format} is {@code null}
         */
        Header(CloudTemplateHeaderFormat format, String text, String example) {
            this.format = Objects.requireNonNull(format, "format must not be null");
            this.text = text;
            this.example = example;
        }

        /**
         * Returns the header format.
         *
         * @return the format selecting text or media rendering
         */
        public CloudTemplateHeaderFormat format() {
            return format;
        }

        /**
         * Returns the header text.
         *
         * @return an {@link Optional} carrying the text, or empty for a media or location header
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        /**
         * Returns the sample value shown in the template-review preview.
         *
         * @return an {@link Optional} carrying the sample value, or empty when none was supplied
         */
        public Optional<String> example() {
            return Optional.ofNullable(example);
        }
    }

    /**
     * The body component of a template, rendering the main text.
     *
     * <p>The body carries the required {@link #text() text}, which may contain numbered placeholders; the
     * {@link #example() example} supplies sample values that drive the template-review preview.
     */
    @ProtobufMessage
    final class Body implements CloudTemplateComponent {
        /**
         * The body text, which may contain numbered placeholders.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String text;

        /**
         * The sample values shown in the template-review preview, or {@code null} when none is supplied.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String example;

        /**
         * Constructs a new body component.
         *
         * @param text    the body text, which may contain numbered placeholders
         * @param example the sample values for the review preview, or {@code null}
         * @throws NullPointerException if {@code text} is {@code null}
         */
        Body(String text, String example) {
            this.text = Objects.requireNonNull(text, "text must not be null");
            this.example = example;
        }

        /**
         * Returns the body text.
         *
         * @return the body text, which may contain numbered placeholders
         */
        public String text() {
            return text;
        }

        /**
         * Returns the sample values shown in the template-review preview.
         *
         * @return an {@link Optional} carrying the sample values, or empty when none was supplied
         */
        public Optional<String> example() {
            return Optional.ofNullable(example);
        }
    }

    /**
     * The footer component of a template, rendering a short trailing line.
     *
     * <p>The footer carries the required {@link #text() text}, a fixed line shown below the body that
     * carries no placeholders.
     */
    @ProtobufMessage
    final class Footer implements CloudTemplateComponent {
        /**
         * The footer text, a fixed line carrying no placeholders.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String text;

        /**
         * Constructs a new footer component.
         *
         * @param text the footer text
         * @throws NullPointerException if {@code text} is {@code null}
         */
        Footer(String text) {
            this.text = Objects.requireNonNull(text, "text must not be null");
        }

        /**
         * Returns the footer text.
         *
         * @return the footer text
         */
        public String text() {
            return text;
        }
    }

    /**
     * The buttons component of a template, rendering the tappable buttons.
     *
     * <p>The component carries an ordered list of {@link CloudTemplateButton} buttons; each button is one
     * of the sealed button variants. Because the list element type is the sealed {@link CloudTemplateButton}
     * interface, which has no single protobuf wire form, this variant is built through its public
     * constructor rather than a generated builder.
     */
    final class Buttons implements CloudTemplateComponent {
        /**
         * The ordered template buttons, empty when none were declared.
         */
        private final List<CloudTemplateButton> buttons;

        /**
         * Constructs a new buttons component.
         *
         * @param buttons the ordered template buttons, or {@code null} for none
         */
        public Buttons(List<CloudTemplateButton> buttons) {
            this.buttons = buttons == null ? List.of() : List.copyOf(buttons);
        }

        /**
         * Returns the ordered template buttons.
         *
         * @return an unmodifiable list of buttons, empty when none were declared
         */
        public List<CloudTemplateButton> buttons() {
            return buttons;
        }
    }

    /**
     * The carousel component of a template, rendering a horizontally scrollable set of cards.
     *
     * <p>The component carries an ordered list of {@link Card cards}; each card is itself defined by an
     * ordered list of {@link CloudTemplateComponent} components (typically a header, a body, and a buttons
     * component), so the structure is recursive. Because the recursive list element type is the sealed
     * {@link CloudTemplateComponent} interface, which has no single protobuf wire form, this variant is
     * built through its public constructor rather than a generated builder.
     */
    final class Carousel implements CloudTemplateComponent {
        /**
         * The ordered carousel cards, empty when none were declared.
         */
        private final List<Card> cards;

        /**
         * Constructs a new carousel component.
         *
         * @param cards the ordered carousel cards, or {@code null} for none
         */
        public Carousel(List<Card> cards) {
            this.cards = cards == null ? List.of() : List.copyOf(cards);
        }

        /**
         * Returns the ordered carousel cards.
         *
         * @return an unmodifiable list of cards, empty when none were declared
         */
        public List<Card> cards() {
            return cards;
        }

        /**
         * One card of a {@link Carousel}, defined by its own ordered list of components.
         *
         * <p>A card carries an ordered list of {@link CloudTemplateComponent} components, typically a
         * header, a body, and a buttons component, mirroring the top-level template structure.
         */
        public static final class Card {
            /**
             * The ordered components that define this card, empty when none were declared.
             */
            private final List<CloudTemplateComponent> components;

            /**
             * Constructs a new carousel card.
             *
             * @param components the ordered components that define this card, or {@code null} for none
             */
            public Card(List<CloudTemplateComponent> components) {
                this.components = components == null ? List.of() : List.copyOf(components);
            }

            /**
             * Returns the ordered components that define this card.
             *
             * @return an unmodifiable list of components, empty when none were declared
             */
            public List<CloudTemplateComponent> components() {
                return components;
            }
        }
    }
}
