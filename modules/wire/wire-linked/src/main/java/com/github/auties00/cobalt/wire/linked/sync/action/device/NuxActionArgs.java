package com.github.auties00.cobalt.wire.linked.sync.action.device;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that uniquely identify a single {@link NuxAction} mutation in
 * the app state sync log.
 *
 * <p>The sync index is composed by concatenating {@link NuxAction#ACTION_NAME}
 * with the trailing arguments produced by {@link #toIndexArgs()}, so that
 * acknowledgements of the same NUX prompt collapse onto a single logical key
 * during conflict resolution. The NUX key identifies which onboarding step,
 * tooltip, or feature discovery prompt the action refers to.
 *
 * @param nuxKey the stable identifier of the NUX prompt this mutation refers to
 */
public record NuxActionArgs(String nuxKey) implements SyncActionArgs {
    /**
     * Returns the trailing index arguments that follow the action name when
     * computing the mutation index for this {@link NuxAction}.
     *
     * @return a single element array containing the NUX key
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{nuxKey};
    }
}
