package com.github.auties00.cobalt.wire.linked.bot.profile;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Represents the character category of a WhatsApp AI bot persona.
 *
 * <p>WhatsApp surfaces a wide range of AI bot personas in chat (Meta AI,
 * celebrity-modeled bots, fictional-character bots, historical-figure bots,
 * etc.) and tags each persona with a category so the user understands what
 * kind of character they are interacting with. This sealed interface enumerates
 * the four officially defined categories and adds an {@link Unknown} variant
 * for forward compatibility with values added by the server in the future.
 *
 * <p>Use {@link #of(String)} to translate the wire-format token sent by the
 * server into the corresponding variant. Each variant exposes its wire token
 * via {@link #value()} for round-tripping back onto the wire.
 *
 * @see BotProfile#category()
 */
public sealed interface BotProfileCategory {
    /**
     * Singleton variant for a fully synthetic or artificial bot persona with
     * no real-world counterpart, such as Meta AI itself.
     */
    BotProfileCategory SYNTHETIC = new Synthetic();

    /**
     * Singleton variant for a bot persona modeled after a currently living
     * person.
     */
    BotProfileCategory LIVING = new Living();

    /**
     * Singleton variant for a bot persona modeled after a fictional
     * character.
     */
    BotProfileCategory FICTIONAL = new Fictional();

    /**
     * Singleton variant for a bot persona modeled after a historical figure.
     */
    BotProfileCategory HISTORICAL = new Historical();

    /**
     * Returns the {@code BotProfileCategory} corresponding to the given wire
     * value.
     *
     * <p>Recognized values are {@code "synthetic"}, {@code "living"},
     * {@code "fictional"} and {@code "historical"} (case-sensitive). Any other
     * non-{@code null} value yields an {@link Unknown} instance carrying the
     * raw token, so callers can still round-trip categories that the client
     * does not yet model explicitly.
     *
     * @param value the wire-format string, or {@code null}
     * @return the corresponding category, or {@code null} if {@code value}
     *         is {@code null}
     */
    @ProtobufDeserializer
    static BotProfileCategory of(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "synthetic" -> SYNTHETIC;
            case "living" -> LIVING;
            case "fictional" -> FICTIONAL;
            case "historical" -> HISTORICAL;
            default -> new Unknown(value);
        };
    }

    /**
     * Returns the wire-format string representation of this category.
     *
     * @return the category value as sent over the wire (for example
     *         {@code "synthetic"})
     */
    @ProtobufSerializer
    String value();

    /**
     * Variant representing a fully synthetic or artificial bot persona with
     * no real-world counterpart.
     */
    record Synthetic() implements BotProfileCategory {
        /**
         * Returns the wire token for the synthetic category.
         *
         * @return {@code "synthetic"}
         */
        @Override
        public String value() {
            return "synthetic";
        }
    }

    /**
     * Variant representing a bot persona modeled after a currently living
     * person.
     */
    record Living() implements BotProfileCategory {
        /**
         * Returns the wire token for the living-person category.
         *
         * @return {@code "living"}
         */
        @Override
        public String value() {
            return "living";
        }
    }

    /**
     * Variant representing a bot persona modeled after a fictional character.
     */
    record Fictional() implements BotProfileCategory {
        /**
         * Returns the wire token for the fictional-character category.
         *
         * @return {@code "fictional"}
         */
        @Override
        public String value() {
            return "fictional";
        }
    }

    /**
     * Variant representing a bot persona modeled after a historical figure.
     */
    record Historical() implements BotProfileCategory {
        /**
         * Returns the wire token for the historical-figure category.
         *
         * @return {@code "historical"}
         */
        @Override
        public String value() {
            return "historical";
        }
    }

    /**
     * Variant representing an unrecognized category value, provided for
     * forward compatibility with values added by the server in the future.
     *
     * @param value the raw wire-format string returned by the server
     */
    record Unknown(String value) implements BotProfileCategory {
    }
}
