package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for {@link NctSaltSyncAction}.
 *
 * <p>The NCT salt is a singleton: only one value exists per account at
 * any time. The sync index therefore carries no positional components
 * beyond the action name itself and is produced as an empty array.
 */
public record NctSaltSyncActionArgs() implements SyncActionArgs {
    /**
     * Returns an empty index args array.
     *
     * <p>The action name is prepended by the sync infrastructure, so no
     * additional arguments are required for uniqueness.
     *
     * @return a zero-length array
     */
    @Override
    public String[] toIndexArgs() {
        return new String[0];
    }
}
