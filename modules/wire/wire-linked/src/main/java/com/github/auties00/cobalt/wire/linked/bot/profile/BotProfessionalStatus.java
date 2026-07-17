package com.github.auties00.cobalt.wire.linked.bot.profile;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Represents whether a WhatsApp AI bot claims to act as a professional,
 * such as a doctor, lawyer, or financial advisor.
 *
 * <p>WhatsApp classifies bots that present themselves as professional
 * advisors so that users can be informed about the nature of the
 * advice they receive. This sealed interface models three possible
 * states:
 * <ul>
 * <li>{@link Yes} - the bot claims a professional role
 * <li>{@link No} - the bot does not claim a professional role
 * <li>{@link Unknown} - the status has not been determined or the
 *     server returned an unrecognized value
 * </ul>
 *
 * <p>Instances are obtained via the {@link #of(String)} factory method,
 * which maps wire-format strings to the appropriate variant. The three
 * singleton constants {@link #UNKNOWN}, {@link #YES}, and {@link #NO}
 * are provided for convenience.
 *
 * @see BotProfile#professionalStatus()
 */
public sealed interface BotProfessionalStatus {
    /**
     * Singleton indicating that the professional status has not been
     * determined or the server returned an unrecognized value.
     */
    BotProfessionalStatus UNKNOWN = new Unknown();

    /**
     * Singleton indicating that the bot claims a professional role.
     */
    BotProfessionalStatus YES = new Yes();

    /**
     * Singleton indicating that the bot does not claim a professional role.
     */
    BotProfessionalStatus NO = new No();

    /**
     * Returns the {@code BotProfessionalStatus} corresponding to the given
     * wire value.
     *
     * <p>Recognized values are {@code "yes"} and {@code "no"}
     * (case-sensitive). Any other non-{@code null} value yields an
     * {@link Unknown} instance.
     *
     * @param value the wire-format string, or {@code null}
     * @return the corresponding status, or {@code null} if {@code value}
     *         is {@code null}
     */
    @ProtobufDeserializer
    static BotProfessionalStatus of(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "yes" -> YES;
            case "no" -> NO;
            default -> UNKNOWN;
        };
    }

    /**
     * Returns the wire-format string representation of this status.
     *
     * @return the status value as sent over the wire (e.g. {@code "yes"})
     */
    @ProtobufSerializer
    String value();

    /**
     * Variant indicating that the professional status has not been
     * determined or the server returned an unrecognized value.
     */
    record Unknown() implements BotProfessionalStatus {
        /**
         * {@inheritDoc}
         *
         * @return {@code "unknown"}
         */
        @Override
        public String value() {
            return "unknown";
        }
    }

    /**
     * Variant indicating that the bot claims a professional role.
     */
    record Yes() implements BotProfessionalStatus {
        /**
         * {@inheritDoc}
         *
         * @return {@code "yes"}
         */
        @Override
        public String value() {
            return "yes";
        }
    }

    /**
     * Variant indicating that the bot does not claim a professional role.
     */
    record No() implements BotProfessionalStatus {
        /**
         * {@inheritDoc}
         *
         * @return {@code "no"}
         */
        @Override
        public String value() {
            return "no";
        }
    }
}
