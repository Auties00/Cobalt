package com.github.auties00.cobalt.props;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;

/**
 * Projected payload of a successful {@code getGroupAbPropsProtocol} call.
 *
 * <p>This is the return shape of {@link ABPropsService#getGroupAbPropsProtocol(Jid, String)}. Each
 * nullable field defaults to {@code null} when the relay omitted the corresponding attribute. The
 * group AB-props sync job consumes the result to refresh the per-group experiment cache.
 *
 * @param groupJid  the group JID echoed back from the request; never {@code null}
 * @param hash      the relay-returned content hash, or {@code null} when omitted
 * @param refresh   the relay-returned refresh-cooldown hint in seconds, or {@code null} when
 *                  omitted
 * @param refreshId the relay-returned refresh id, or {@code null} when omitted
 * @param props     the projected experiment-config list; never {@code null}
 */
@WhatsAppWebModule(moduleName = "WAGetGroupAbPropsProtocol")
public record GroupAbPropsResult(Jid groupJid, String hash, Integer refresh, Integer refreshId, List<Entry> props) {
    /**
     * Validates {@code groupJid} and defensively copies {@code props} into an immutable list.
     *
     * <p>The supplied {@code props} list may safely be mutated after construction because it is
     * copied.
     *
     * @throws NullPointerException if {@code groupJid} or {@code props} is {@code null}
     */
    public GroupAbPropsResult {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        props = List.copyOf(props);
    }

    /**
     * One {@code ExperimentConfig <prop>} child from a {@code getGroupAbPropsProtocol} response.
     *
     * <p>{@code SamplingConfig} entries returned alongside on the wire are not projected into the
     * group result because WA Web consumes only the experiment entries for the group surface.
     *
     * @param configCode    the numeric experiment code
     * @param configValue   the experiment value as a raw string; never {@code null}
     * @param configExpoKey the exposure key as a string, or {@code null} when the relay omitted it
     */
    @WhatsAppWebModule(moduleName = "WAGetGroupAbPropsProtocol")
    public record Entry(int configCode, String configValue, String configExpoKey) {
        /**
         * Validates {@code configValue}.
         *
         * <p>{@code configExpoKey} is allowed to be {@code null} so the entry can represent the
         * no-exposure-key case.
         *
         * @throws NullPointerException if {@code configValue} is {@code null}
         */
        public Entry {
            Objects.requireNonNull(configValue, "configValue cannot be null");
        }
    }
}
