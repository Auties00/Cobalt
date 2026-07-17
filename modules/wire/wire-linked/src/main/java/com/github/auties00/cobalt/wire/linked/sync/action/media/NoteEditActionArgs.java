package com.github.auties00.cobalt.wire.linked.sync.action.media;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Sync index arguments that identify the target note of a {@link NoteEditAction}.
 *
 * <p>Note actions are keyed by a single note identifier so that create, update and
 * delete operations on the same note collapse into a single entry in the app-state
 * sync collection.
 *
 * @param noteId the unique identifier of the note being created, edited, or deleted
 */
public record NoteEditActionArgs(String noteId) implements SyncActionArgs {
    /**
     * Returns the index arguments used to address this note inside the app-state
     * sync collection.
     *
     * @return a single-element array containing the note identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{noteId};
    }
}
