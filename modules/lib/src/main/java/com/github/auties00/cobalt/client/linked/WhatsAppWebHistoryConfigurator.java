package com.github.auties00.cobalt.client.linked;

/**
 * Abstract base for the per-mode configurators used by the history steps of
 * the linked-client builder.
 *
 * <p>The builder's {@code fullHistory} and {@code defaultHistory} steps each
 * accept a configurator for their own mode: {@link Full} exposes the full-sync
 * day window, {@link Default} exposes the storage quota and windowing caps.
 * The newsletter toggle is shared by both modes and lives on this base. Because
 * each mode has its own configurator type, settings that are invalid for a mode
 * are simply absent from it and cannot be expressed. The discard mode has no
 * settings and so takes no configurator.
 *
 * <p>Configurators are mutable and short-lived: the builder creates one, runs
 * the caller's lambda over it, copies the resulting settings into the
 * {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore}, and
 * discards it.
 *
 * @param <SELF> the concrete configurator type, returned by the shared fluent
 *               setters so they chain in any order
 *
 * @apiNote
 * The amount of history the primary actually delivers is bounded by the user's
 * per-device setting on the phone; the values configured here are advertised
 * requests, not guarantees.
 *
 * @see LinkedWhatsAppClient
 */
public abstract sealed class WhatsAppWebHistoryConfigurator<SELF extends WhatsAppWebHistoryConfigurator<SELF>>
        permits WhatsAppWebHistoryConfigurator.Full, WhatsAppWebHistoryConfigurator.Default {
    /**
     * Whether the newsletter list is bootstrapped after login.
     */
    boolean newsletters = true;

    /**
     * Constructs a configurator with the default newsletter setting, reached by
     * the concrete subtypes.
     */
    WhatsAppWebHistoryConfigurator() {

    }

    /**
     * Returns this configurator narrowed to its concrete type for fluent
     * chaining.
     *
     * @return this configurator
     */
    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }

    /**
     * Sets whether the newsletter list is bootstrapped after login.
     *
     * @param newsletters whether to bootstrap newsletters
     * @return this configurator
     */
    public SELF newsletters(boolean newsletters) {
        this.newsletters = newsletters;
        return self();
    }

    /**
     * The settings for a full history sync.
     */
    public static final class Full extends WhatsAppWebHistoryConfigurator<Full> {
        /**
         * WhatsApp Web's maximum full-sync day window, applied as the default.
         */
        static final int DEFAULT_DAYS = 365;

        /**
         * The full-sync day window.
         */
        int days = DEFAULT_DAYS;

        /**
         * Constructs a full-sync configurator with default settings, reached by
         * the linked-client builder.
         */
        Full() {

        }

        /**
         * Sets the full-sync day window.
         *
         * <p>WhatsApp Web never requests more than {@value #DEFAULT_DAYS} days;
         * larger values are honoured only at the primary's discretion.
         *
         * @param days the day window; must be positive
         * @return this configurator
         * @throws IllegalArgumentException if {@code days} is not positive
         */
        public Full days(int days) {
            if (days <= 0) {
                throw new IllegalArgumentException("days must be positive: " + days);
            }
            this.days = days;
            return this;
        }
    }

    /**
     * The settings for the default (production-default) history sync.
     */
    public static final class Default extends WhatsAppWebHistoryConfigurator<Default> {
        /**
         * The advertised storage budget in megabytes, or {@code null} to
         * compute it from the host's available storage at handshake time.
         */
        Integer storageQuotaMb;

        /**
         * The recent-sync day window, or {@code null} for the server default.
         */
        Integer recentSyncDays;

        /**
         * The thumbnail-sync day window, or {@code null} for the server default.
         */
        Integer thumbnailSyncDays;

        /**
         * The maximum messages per chat in the initial sync, or {@code null}
         * for the server default.
         */
        Integer maxMessagesPerChat;

        /**
         * Constructs a default-sync configurator with default settings, reached
         * by the linked-client builder.
         */
        Default() {

        }

        /**
         * Sets the advertised storage budget in megabytes; when unset it is
         * computed from the host's available storage at handshake time.
         *
         * @param storageQuotaMb the storage quota in megabytes; must be
         *                       non-negative
         * @return this configurator
         * @throws IllegalArgumentException if {@code storageQuotaMb} is negative
         */
        public Default storageQuotaMb(int storageQuotaMb) {
            if (storageQuotaMb < 0) {
                throw new IllegalArgumentException("storageQuotaMb must be non-negative: " + storageQuotaMb);
            }
            this.storageQuotaMb = storageQuotaMb;
            return this;
        }

        /**
         * Sets the recent-sync day window.
         *
         * @param days the day window; must be positive
         * @return this configurator
         * @throws IllegalArgumentException if {@code days} is not positive
         */
        public Default recentSyncDays(int days) {
            if (days <= 0) {
                throw new IllegalArgumentException("days must be positive: " + days);
            }
            this.recentSyncDays = days;
            return this;
        }

        /**
         * Sets the thumbnail-sync day window.
         *
         * @param days the day window; must be positive
         * @return this configurator
         * @throws IllegalArgumentException if {@code days} is not positive
         */
        public Default thumbnailSyncDays(int days) {
            if (days <= 0) {
                throw new IllegalArgumentException("days must be positive: " + days);
            }
            this.thumbnailSyncDays = days;
            return this;
        }

        /**
         * Sets the maximum number of messages per chat included in the initial
         * sync.
         *
         * @param count the per-chat message cap; must be positive
         * @return this configurator
         * @throws IllegalArgumentException if {@code count} is not positive
         */
        public Default maxMessagesPerChat(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("count must be positive: " + count);
            }
            this.maxMessagesPerChat = count;
            return this;
        }
    }
}
