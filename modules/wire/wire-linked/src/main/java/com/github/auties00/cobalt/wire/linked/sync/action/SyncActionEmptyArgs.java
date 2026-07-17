package com.github.auties00.cobalt.wire.linked.sync.action;

/**
 * Shared arguments carrier used by sync actions that require no trailing
 * index arguments.
 *
 * <p>Used for actions whose encoded index is simply
 * {@code ["actionName"]} with no additional parameters (for example, global
 * settings whose key is identified purely by the action name). A single
 * shared instance is exposed via {@link SyncActionArgs#empty()}.
 */
public final class SyncActionEmptyArgs implements SyncActionArgs {
    /**
     * The shared singleton instance, reused by all actions that have no
     * trailing index arguments.
     */
    static final SyncActionEmptyArgs INSTANCE = new SyncActionEmptyArgs();

    /**
     * Private constructor preventing additional instantiation; callers use
     * {@link SyncActionArgs#empty()} to obtain the shared instance.
     */
    private SyncActionEmptyArgs() {

    }

    /**
     * Returns an empty array because this carrier contributes no trailing
     * arguments to the encoded index.
     *
     * @return a zero length string array
     */
    @Override
    public String[] toIndexArgs() {
        return new String[0];
    }
}
