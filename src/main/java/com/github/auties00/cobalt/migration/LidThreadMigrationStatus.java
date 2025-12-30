package com.github.auties00.cobalt.migration;

/**
 * Represents the status of the LID 1-to-1 thread migration process.
 * This migration transitions from phone number-based addressing to LID-based addressing.
 */
public enum LidThreadMigrationStatus {
    /**
     * Waiting for primary device to send PN-to-LID mappings
     */
    WAITING_MAPPINGS,

    /**
     * Mappings received, ready to start migration
     */
    READY,

    /**
     * Migration is currently in progress
     */
    IN_PROGRESS,

    /**
     * Migration has completed successfully
     */
    COMPLETE
}
