package com.github.auties00.cobalt.wire.linked.sync.action;

/**
 * Typed carrier for the trailing arguments that follow the action name in
 * an app state sync mutation index.
 *
 * <p>Every app state mutation is keyed by a JSON array of the form
 * {@code ["actionName", arg0, arg1, ...]}. Implementations of this
 * interface encode the argument list specific to an action category (for
 * example, a chat JID for archive or mute actions, or a message key for
 * star and delete actions). The action itself prepends the action name via
 * {@link SyncAction#toIndex(SyncActionArgs)}; callers supply the trailing
 * arguments through this carrier.
 *
 * <p>Actions that take no trailing arguments use {@link #empty()} to obtain
 * the shared empty arguments singleton; actions that take trailing arguments
 * define their own dedicated implementation that produces them in the
 * correct order.
 *
 * <p>Any JID normalisation (such as lid migration) must be performed by the
 * caller before constructing the arguments; the arguments carrier treats
 * its inputs as already canonical.
 */
public interface SyncActionArgs {

    /**
     * Returns the trailing arguments to append after the action name when
     * building the encoded index.
     *
     * <p>The returned strings are appended in order and must reflect the
     * canonical encoding expected by the action category.
     *
     * @return a non {@code null} array of trailing index arguments
     */
    String[] toIndexArgs();

    /**
     * Returns the shared arguments instance that carries no trailing
     * arguments, used by actions whose index is simply
     * {@code ["actionName"]}.
     *
     * @return the shared empty arguments singleton
     */
    static SyncActionArgs empty() {
        return SyncActionEmptyArgs.INSTANCE;
    }
}
