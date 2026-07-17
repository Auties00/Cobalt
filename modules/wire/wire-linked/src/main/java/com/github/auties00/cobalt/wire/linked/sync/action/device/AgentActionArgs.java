package com.github.auties00.cobalt.wire.linked.sync.action.device;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that uniquely identify a single {@link AgentAction} mutation in
 * the app state sync log.
 *
 * <p>The sync index is composed by concatenating {@link AgentAction#ACTION_NAME}
 * with the trailing arguments produced by {@link #toIndexArgs()}, so that
 * mutations for the same agent collapse onto a single logical key during conflict
 * resolution. The agent identifier is the stable key that lets a server or peer
 * device distinguish one agent from another across time.
 *
 * @param agentId the stable identifier of the business agent this mutation refers
 *                to
 */
public record AgentActionArgs(String agentId) implements SyncActionArgs {
    /**
     * Returns the trailing index arguments that follow the action name when
     * computing the mutation index for this {@link AgentAction}.
     *
     * @return a single element array containing the agent identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{agentId};
    }
}
