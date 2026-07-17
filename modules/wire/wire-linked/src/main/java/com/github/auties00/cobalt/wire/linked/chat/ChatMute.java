package com.github.auties00.cobalt.wire.linked.chat;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Represents the mute state of a WhatsApp chat, controlling whether
 * notifications for the chat are suppressed and for how long.
 *
 * <p>WhatsApp allows users to mute individual chats to silence notifications.
 * A chat can be muted for a fixed duration (8 hours, 1 week, or until a
 * specific timestamp), muted indefinitely, or not muted at all. This sealed
 * interface models all three states via its permitted implementations:
 * {@link Disabled} (notifications enabled), {@link Enabled.Timeframe}
 * (muted until a specific instant), and {@link Enabled.Indefinitely}
 * (muted with no expiration).
 *
 * <p>Instances are obtained through the static factory methods
 * {@link #notMuted()}, {@link #muted()}, {@link #mutedForEightHours()},
 * {@link #mutedForOneWeek()}, and {@link #mutedUntil(Long)}.
 *
 * <p>The mute state is serialized as an epoch-second value via
 * {@link #toEpochSecond()}: {@code 0} means not muted, {@code -1} means
 * muted indefinitely, and any other positive value represents the
 * expiration timestamp.
 */
public sealed interface ChatMute {
    /**
     * Returns the epoch-second representation of this mute state.
     *
     * <p>The returned value is {@code 0} for {@link Disabled}, {@code -1}
     * for {@link Enabled.Indefinitely}, or a positive epoch-second timestamp
     * for {@link Enabled.Timeframe}.
     *
     * @return the epoch-second value representing this mute state
     */
    @ProtobufSerializer
    long toEpochSecond();

    /**
     * Returns whether the chat is currently muted.
     *
     * @return {@code true} if notifications are suppressed, {@code false}
     *         otherwise
     */
    boolean isMuted();

    /**
     * Returns a {@code ChatMute} representing the not-muted state.
     *
     * <p>The returned instance has an epoch-second value of {@code 0} and
     * its {@link #isMuted()} method returns {@code false}.
     *
     * @return a {@code ChatMute} indicating that notifications are enabled,
     *         not null
     */
    static ChatMute notMuted() {
        return Disabled.INSTANCE;
    }

    /**
     * Returns a {@code ChatMute} representing an indefinite mute with no
     * expiration.
     *
     * <p>The returned instance has an epoch-second value of {@code -1} and
     * its {@link #isMuted()} method returns {@code true}.
     *
     * @return a {@code ChatMute} indicating that notifications are
     *         suppressed indefinitely, not null
     */
    static ChatMute muted() {
        return Enabled.Indefinitely.INSTANCE;
    }

    /**
     * Returns a {@code ChatMute} that expires eight hours from the current
     * time.
     *
     * <p>The expiration instant is computed as
     * {@code Instant.now().plus(8, ChronoUnit.HOURS)}.
     *
     * @return a {@code ChatMute} muted for eight hours, not null
     */
    static ChatMute mutedForEightHours() {
        return mutedUntil(Instant.now().plus(8, ChronoUnit.HOURS).getEpochSecond());
    }

    /**
     * Returns a {@code ChatMute} that expires one week from the current
     * time.
     *
     * <p>The expiration instant is computed as
     * {@code Instant.now().plus(1, ChronoUnit.WEEKS)}.
     *
     * @return a {@code ChatMute} muted for one week, not null
     */
    static ChatMute mutedForOneWeek() {
        return mutedUntil(Instant.now().plus(1, ChronoUnit.WEEKS).getEpochSecond());
    }

    /**
     * Returns a {@code ChatMute} for the given epoch-second expiration
     * timestamp.
     *
     * @param seconds the epoch-second expiration timestamp, or {@code null}
     *        to indicate not muted
     * @return a {@code ChatMute} corresponding to the given timestamp, not
     *         null
     */
    @ProtobufDeserializer
    static ChatMute mutedUntil(Long seconds) {
        if (seconds == null || seconds == Disabled.EPOCH_SECOND) {
            return Disabled.INSTANCE;
        } else if (seconds == Enabled.Indefinitely.EPOCH_SECOND) {
            return Enabled.Indefinitely.INSTANCE;
        } else {
            return new ChatMute.Enabled.Timeframe(Instant.ofEpochSecond(seconds));
        }
    }

    /**
     * A {@link ChatMute} representing the not-muted state, where notifications
     * are delivered normally.
     *
     * <p>This class uses a singleton pattern; the sole instance is obtained
     * through {@link ChatMute#notMuted()}.
     */
    final class Disabled implements ChatMute {
        /**
         * The singleton not-muted instance.
         */
        private static final Disabled INSTANCE = new Disabled();

        /**
         * The epoch-second sentinel value ({@code 0}) indicating that the
         * chat is not muted.
         */
        private static final int EPOCH_SECOND = 0;

        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation always returns {@code 0}.
         */
        @Override
        public long toEpochSecond() {
            return EPOCH_SECOND;
        }

        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation always returns {@code false}.
         */
        @Override
        public boolean isMuted() {
            return false;
        }
    }

    /**
     * A {@link ChatMute} sub-interface representing an active mute, either
     * time-bound or indefinite.
     *
     * <p>All implementations of this interface return {@code true} from
     * {@link #isMuted()}.
     */
    sealed interface Enabled extends ChatMute {
        /**
         * {@inheritDoc}
         *
         * @implSpec The default implementation always returns {@code true}.
         */
        @Override
        default boolean isMuted() {
            return true;
        }

        /**
         * An {@link Enabled} mute that expires at a specific {@link Instant}.
         *
         * <p>Instances are created through {@link ChatMute#mutedUntil(Long)},
         * {@link ChatMute#mutedForEightHours()}, or
         * {@code ChatMute#mutedForOneWeek()}.
         */
        final class Timeframe implements Enabled {
            /**
             * The instant at which this mute expires.
             */
            final Instant end;

            /**
             * Constructs a new {@code Timeframe} mute that expires at the
             * given instant.
             *
             * @param end the instant at which the mute expires, not null
             */
            Timeframe(Instant end) {
                this.end = end;
            }

            /**
             * {@inheritDoc}
             *
             * @implSpec This implementation returns the epoch second of the
             *           expiration instant.
             */
            @Override
            public long toEpochSecond() {
                return end.getEpochSecond();
            }
        }

        /**
         * An {@link Enabled} mute with no expiration.
         *
         * <p>This class uses a singleton pattern; the sole instance is obtained
         * through {@link ChatMute#muted()}.
         */
        final class Indefinitely implements Enabled {
            /**
             * The singleton indefinitely-muted instance.
             */
            private static final Indefinitely INSTANCE = new Indefinitely();

            /**
             * The epoch-second sentinel value ({@code -1}) indicating that
             * the mute has no expiration.
             */
            private static final long EPOCH_SECOND = -1;

            /**
             * {@inheritDoc}
             *
             * @implSpec This implementation always returns {@code -1}.
             */
            @Override
            public long toEpochSecond() {
                return EPOCH_SECOND;
            }
        }
    }
}