package com.github.auties00.cobalt.wire.linked.message.payment;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents the metadata attached to a message that embeds a payment
 * link.
 *
 * <p>A payment link message renders as a rich card in the chat: it
 * shows a header (either a link preview or an order summary), a call
 * to action button, and a provider-specific payload that the client
 * uses to drive the checkout flow. This container groups the three
 * pieces of metadata that describe the card.
 */
@ProtobufMessage(name = "Message.PaymentLinkMetadata")
public final class PaymentLinkMetadata implements Message {
    /**
     * The action button rendered on the payment card.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    PaymentLinkButton button;

    /**
     * The header rendered at the top of the payment card.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    PaymentLinkHeader header;

    /**
     * The provider-specific payload used to drive the checkout flow.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    PaymentLinkProvider provider;


    /**
     * Constructs a new payment link metadata with the given button,
     * header and provider payload.
     *
     * @param button   the action button, may be {@code null}
     * @param header   the card header, may be {@code null}
     * @param provider the provider-specific payload, may be
     *                 {@code null}
     */
    PaymentLinkMetadata(PaymentLinkButton button, PaymentLinkHeader header, PaymentLinkProvider provider) {
        this.button = button;
        this.header = header;
        this.provider = provider;
    }

    /**
     * Returns the action button rendered on the payment card.
     *
     * @return an {@link Optional} containing the {@link PaymentLinkButton},
     *         or {@link Optional#empty()} if not set
     */
    public Optional<PaymentLinkButton> button() {
        return Optional.ofNullable(button);
    }

    /**
     * Returns the header rendered at the top of the payment card.
     *
     * @return an {@link Optional} containing the {@link PaymentLinkHeader},
     *         or {@link Optional#empty()} if not set
     */
    public Optional<PaymentLinkHeader> header() {
        return Optional.ofNullable(header);
    }

    /**
     * Returns the provider-specific payload used to drive the
     * checkout flow.
     *
     * @return an {@link Optional} containing the
     *         {@link PaymentLinkProvider}, or {@link Optional#empty()}
     *         if not set
     */
    public Optional<PaymentLinkProvider> provider() {
        return Optional.ofNullable(provider);
    }

    /**
     * Sets the action button rendered on the payment card.
     *
     * @param button the action button, may be {@code null}
     */
    public void setButton(PaymentLinkButton button) {
        this.button = button;
    }

    /**
     * Sets the header rendered at the top of the payment card.
     *
     * @param header the card header, may be {@code null}
     */
    public void setHeader(PaymentLinkHeader header) {
        this.header = header;
    }

    /**
     * Sets the provider-specific payload used to drive the checkout
     * flow.
     *
     * @param provider the provider-specific payload, may be
     *                 {@code null}
     */
    public void setProvider(PaymentLinkProvider provider) {
        this.provider = provider;
    }

    /**
     * Represents the action button displayed on a payment link card.
     *
     * <p>Carries the user-visible text of the call-to-action shown
     * beneath the header. Tapping the button triggers the checkout
     * flow defined by the associated {@link PaymentLinkProvider}.
     */
    @ProtobufMessage(name = "Message.PaymentLinkMetadata.PaymentLinkButton")
    public static final class PaymentLinkButton {
        /**
         * The user-visible text of the call-to-action button.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String displayText;


        /**
         * Constructs a new payment link button with the given display
         * text.
         *
         * @param displayText the button label, may be {@code null}
         */
        PaymentLinkButton(String displayText) {
            this.displayText = displayText;
        }

        /**
         * Returns the user-visible text of the button.
         *
         * @return an {@link Optional} containing the button label,
         *         or {@link Optional#empty()} if not set
         */
        public Optional<String> displayText() {
            return Optional.ofNullable(displayText);
        }

        /**
         * Sets the user-visible text of the button.
         *
         * @param displayText the button label, may be {@code null}
         */
        public void setDisplayText(String displayText) {
            this.displayText = displayText;
    }
    }

    /**
     * Represents the header displayed at the top of a payment link
     * card.
     *
     * <p>The header describes how the card should be rendered: either
     * as a generic link preview (favicon, title and domain pulled
     * from the destination URL) or as an order summary (item list
     * and totals). The exact rendering is controlled by the
     * {@link PaymentLinkHeaderType} value.
     */
    @ProtobufMessage(name = "Message.PaymentLinkMetadata.PaymentLinkHeader")
    public static final class PaymentLinkHeader {
        /**
         * The rendering style of this header.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        PaymentLinkHeader.PaymentLinkHeaderType headerType;


        /**
         * Constructs a new payment link header with the given
         * rendering style.
         *
         * @param headerType the header type, may be {@code null}
         */
        PaymentLinkHeader(PaymentLinkHeaderType headerType) {
            this.headerType = headerType;
        }

        /**
         * Returns the rendering style of this header.
         *
         * @return an {@link Optional} containing the
         *         {@link PaymentLinkHeaderType}, or
         *         {@link Optional#empty()} if not set
         */
        public Optional<PaymentLinkHeaderType> headerType() {
            return Optional.ofNullable(headerType);
        }

        /**
         * Sets the rendering style of this header.
         *
         * @param headerType the header type, may be {@code null}
         */
        public void setHeaderType(PaymentLinkHeaderType headerType) {
            this.headerType = headerType;
    }

        /**
         * Enumerates the ways in which a {@link PaymentLinkHeader}
         * can be rendered.
         */
        @ProtobufEnum(name = "Message.PaymentLinkMetadata.PaymentLinkHeader.PaymentLinkHeaderType")
        public static enum PaymentLinkHeaderType {
            /**
             * The header is rendered as a generic link preview,
             * showing the destination page's favicon, title and
             * domain.
             */
            LINK_PREVIEW(0),
            /**
             * The header is rendered as an order summary, showing
             * the list of items and their totals.
             */
            ORDER(1);

            /**
             * Constructs a new header type with the given protobuf
             * index.
             *
             * @param index the protobuf wire index for this type
             */
            PaymentLinkHeaderType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index of this header type.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this header type.
             *
             * @return the numeric index used when serialising this
             *         constant over the wire
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Represents the provider-specific payload attached to a payment
     * link card.
     *
     * <p>Carries an opaque JSON blob understood by the payment
     * provider's backend. WhatsApp clients pass this payload through
     * unchanged when initiating the checkout flow, so that the
     * provider can restore the full context of the payment request.
     */
    @ProtobufMessage(name = "Message.PaymentLinkMetadata.PaymentLinkProvider")
    public static final class PaymentLinkProvider {
        /**
         * The provider-specific parameters, serialised as a JSON
         * string.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String paramsJson;


        /**
         * Constructs a new payment link provider payload with the
         * given JSON string.
         *
         * @param paramsJson the provider parameters as JSON, may be
         *                   {@code null}
         */
        PaymentLinkProvider(String paramsJson) {
            this.paramsJson = paramsJson;
        }

        /**
         * Returns the provider-specific parameters as a JSON string.
         *
         * @return an {@link Optional} containing the JSON payload,
         *         or {@link Optional#empty()} if not set
         */
        public Optional<String> paramsJson() {
            return Optional.ofNullable(paramsJson);
        }

        /**
         * Sets the provider-specific parameters as a JSON string.
         *
         * @param paramsJson the provider parameters as JSON, may be
         *                   {@code null}
         */
        public void setParamsJson(String paramsJson) {
            this.paramsJson = paramsJson;
    }
    }
}
