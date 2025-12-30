package com.github.auties00.cobalt.exception;

/**
 * Exception thrown when LID migration encounters a critical error.
 * <p>
 * This exception indicates that the LID migration process has failed
 * in a way that requires the session to be terminated (e.g., split thread
 * mismatch, obsolete mappings from primary device).
 */
public sealed abstract class LidMigrationException
        extends RuntimeException
        permits LidMigrationException.SplitThreadMismatch, LidMigrationException.PrimaryMappingsObsolete, LidMigrationException.FailedToParseMappings {

    protected LidMigrationException(String message) {
        super("LID migration failed: " + message);
    }

    protected LidMigrationException(String message, Throwable reason) {
        super("LID migration failed: " + message, reason);
    }

    /**
     * Split thread mismatch between local and primary device.
     */
    public static final class SplitThreadMismatch extends LidMigrationException {
        public SplitThreadMismatch() {
            super("Split thread mismatch between local and primary device");
        }
    }

    /**
     * Primary device mappings are obsolete.
     */
    public static final class PrimaryMappingsObsolete extends LidMigrationException {
        public PrimaryMappingsObsolete() {
            super("Primary device mappings are obsolete");
        }
    }

    /**
     * Failed to parse migration mappings.
     */
    public static final class FailedToParseMappings extends LidMigrationException {
        public FailedToParseMappings(String message) {
            super("Failed to parse migration mappings (" + message + ")");
        }

        public FailedToParseMappings(String message, Throwable reason) {
            super("Failed to parse migration mappings (" + message + ")", reason);
        }
    }
}