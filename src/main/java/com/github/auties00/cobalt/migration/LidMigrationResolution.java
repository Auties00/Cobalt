package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.exception.LidMigrationException;
import com.github.auties00.cobalt.model.jid.Jid;

import java.util.Objects;

/**
 * Result of a chat resolution during migration.
 */
public sealed interface LidMigrationResolution {
    /**
     * The chat should be migrated to the specified LID
     */
    record Migrate(Jid threadLid) implements LidMigrationResolution {
        public Migrate {
            Objects.requireNonNull(threadLid, "Thread LID cannot be null");
        }
    }

    /**
     * The chat should be deleted (e.g., empty/inactive chat with no LID)
     */
    record Delete() implements LidMigrationResolution {

    }

    /**
     * An error occurred during resolution requiring logout
     */
    record Error(LidMigrationException exception) implements LidMigrationResolution {
        public Error {
            Objects.requireNonNull(exception, "exception cannot be null");
        }
    }
}
