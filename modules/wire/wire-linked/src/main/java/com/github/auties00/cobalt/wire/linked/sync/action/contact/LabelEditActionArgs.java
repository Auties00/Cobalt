package com.github.auties00.cobalt.wire.linked.sync.action.contact;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link LabelEditAction} inside a
 * sync patch.
 *
 * <p>A label is uniquely addressed by its identifier. When building or
 * reading a patch the sync engine translates these arguments into the index
 * tuple {@code ["label_edit", labelId]}.
 *
 * @param labelId the identifier of the label that is being created, edited
 *                or deleted
 */
public record LabelEditActionArgs(String labelId) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * label.
     *
     * @return a single-element array containing the label identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{labelId};
    }
}
