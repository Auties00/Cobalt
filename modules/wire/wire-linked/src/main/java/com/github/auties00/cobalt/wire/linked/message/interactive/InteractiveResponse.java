package com.github.auties00.cobalt.wire.linked.message.interactive;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Represents the payload of an interactive response that echoes the user's selection back
 * to the sender.
 *
 * <p>When a recipient picks an option from an interactive list or button grid, their client
 * produces a response whose concrete shape is captured by this sealed interface. Currently
 * only {@link SelectedDisplayText} is defined, carrying the human-readable label of the
 * option that the user selected.
 */
public sealed interface InteractiveResponse permits InteractiveResponse.SelectedDisplayText {

    /**
     * Wraps the display text of the option that the recipient selected.
     *
     * <p>This variant is serialized as a single {@code string} field by the protobuf layer
     * and allows senders to match the user's selection against the labels they originally
     * offered.
     */
    final class SelectedDisplayText implements InteractiveResponse {
        /**
         * The display text of the option the user tapped.
         */
        String selectedDisplayText;

        /**
         * Constructs a new selected display text wrapper with the given label.
         *
         * @param selectedDisplayText the display text of the chosen option
         */
        SelectedDisplayText(String selectedDisplayText) {
            this.selectedDisplayText = selectedDisplayText;
        }

        /**
         * Returns the display text of the option that the recipient selected.
         *
         * @return the selected display text
         */
        @ProtobufSerializer
        public String selectedDisplayText() {
            return selectedDisplayText;
        }

        /**
         * Creates a new selected display text wrapper from the supplied label.
         *
         * <p>This factory is invoked by the protobuf deserialization layer when materializing
         * an interactive response.
         *
         * @param selectedDisplayText the display text of the chosen option
         * @return a new wrapper around the selected label
         */
        @ProtobufDeserializer
        public static SelectedDisplayText of(String selectedDisplayText) {
            return new SelectedDisplayText(selectedDisplayText);
        }
    }
}
