package com.github.auties00.cobalt.wire.cloud.flow;

import java.util.List;
import java.util.Optional;

/**
 * The outcome of migrating Flows from one WhatsApp Business Account to another.
 *
 * <p>The migration edge copies named Flows (or all Flows) from a source WABA into the calling WABA
 * and reports the per-Flow result in two buckets: the Flows that migrated successfully, each carrying
 * the source name and the new id, and the Flows that failed, each carrying the source name and the
 * error code and message.
 */
public final class CloudFlowMigrationResult {
    /**
     * The Flows that migrated successfully.
     */
    private final List<Migrated> migratedFlows;

    /**
     * The Flows that failed to migrate.
     */
    private final List<Failed> failedFlows;

    /**
     * Constructs a new migration result.
     *
     * @param migratedFlows the Flows that migrated successfully, or {@code null} for none
     * @param failedFlows   the Flows that failed to migrate, or {@code null} for none
     */
    public CloudFlowMigrationResult(List<Migrated> migratedFlows, List<Failed> failedFlows) {
        this.migratedFlows = migratedFlows == null ? List.of() : List.copyOf(migratedFlows);
        this.failedFlows = failedFlows == null ? List.of() : List.copyOf(failedFlows);
    }

    /**
     * Returns the Flows that migrated successfully.
     *
     * @return an unmodifiable list of migrated Flows, empty when none migrated
     */
    public List<Migrated> migratedFlows() {
        return migratedFlows;
    }

    /**
     * Returns the Flows that failed to migrate.
     *
     * @return an unmodifiable list of failed Flows, empty when none failed
     */
    public List<Failed> failedFlows() {
        return failedFlows;
    }

    /**
     * A Flow that migrated successfully.
     */
    public static final class Migrated {
        /**
         * The Flow name on the source WABA, or {@code null} when none was returned.
         */
        private final String sourceName;

        /**
         * The Flow id on the source WABA, or {@code null} when none was returned.
         */
        private final String sourceId;

        /**
         * The id assigned on the destination WABA, or {@code null} when none was returned.
         */
        private final String migratedId;

        /**
         * Constructs a new migrated-flow entry.
         *
         * @param sourceName the Flow name on the source WABA, or {@code null} when absent
         * @param sourceId   the Flow id on the source WABA, or {@code null} when absent
         * @param migratedId the id assigned on the destination WABA, or {@code null} when absent
         */
        public Migrated(String sourceName, String sourceId, String migratedId) {
            this.sourceName = sourceName;
            this.sourceId = sourceId;
            this.migratedId = migratedId;
        }

        /**
         * Returns the Flow name on the source WABA.
         *
         * @return an {@link Optional} carrying the source name, or empty when none was returned
         */
        public Optional<String> sourceName() {
            return Optional.ofNullable(sourceName);
        }

        /**
         * Returns the Flow id on the source WABA.
         *
         * @return an {@link Optional} carrying the source id, or empty when none was returned
         */
        public Optional<String> sourceId() {
            return Optional.ofNullable(sourceId);
        }

        /**
         * Returns the id assigned on the destination WABA.
         *
         * @return an {@link Optional} carrying the migrated id, or empty when none was returned
         */
        public Optional<String> migratedId() {
            return Optional.ofNullable(migratedId);
        }
    }

    /**
     * A Flow that failed to migrate.
     */
    public static final class Failed {
        /**
         * The Flow name on the source WABA, or {@code null} when none was returned.
         */
        private final String sourceName;

        /**
         * The failure error code, or {@code null} when none was returned.
         */
        private final String errorCode;

        /**
         * The failure error message, or {@code null} when none was returned.
         */
        private final String errorMessage;

        /**
         * Constructs a new failed-flow entry.
         *
         * @param sourceName   the Flow name on the source WABA, or {@code null} when absent
         * @param errorCode    the failure error code, or {@code null} when absent
         * @param errorMessage the failure error message, or {@code null} when absent
         */
        public Failed(String sourceName, String errorCode, String errorMessage) {
            this.sourceName = sourceName;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        /**
         * Returns the Flow name on the source WABA.
         *
         * @return an {@link Optional} carrying the source name, or empty when none was returned
         */
        public Optional<String> sourceName() {
            return Optional.ofNullable(sourceName);
        }

        /**
         * Returns the failure error code.
         *
         * @return an {@link Optional} carrying the error code, or empty when none was returned
         */
        public Optional<String> errorCode() {
            return Optional.ofNullable(errorCode);
        }

        /**
         * Returns the failure error message.
         *
         * @return an {@link Optional} carrying the error message, or empty when none was returned
         */
        public Optional<String> errorMessage() {
            return Optional.ofNullable(errorMessage);
        }
    }
}
