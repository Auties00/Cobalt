package com.github.auties00.cobalt.wire.linked.business.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * The set of WhatsApp features the server has disabled for a session acting on
 * behalf of an authorized business agent.
 *
 * <p>A business can let a delegated agent operate the account from a separate
 * session; the server publishes a policy listing the features it has switched
 * off for that delegated session, and the client hides the corresponding UI
 * before rendering. This model is that disabled-feature list.
 *
 * <p>The feature names are server-defined opaque strings; unknown entries are
 * tolerated and ignored.
 */
@ProtobufMessage(name = "AuthorizedAgentFeaturePolicy")
public final class AuthorizedAgentFeaturePolicy {
    /**
     * Server-defined names of the features disabled for the delegated agent,
     * in the order the server returned them. Never {@code null}, possibly
     * empty when the server returned none.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<String> disabledFeatures;

    /**
     * Constructs a new {@code AuthorizedAgentFeaturePolicy}. A {@code null}
     * {@code disabledFeatures} is coerced to an empty list.
     *
     * @param disabledFeatures the disabled-feature names; {@code null} treated
     *                         as empty
     */
    AuthorizedAgentFeaturePolicy(List<String> disabledFeatures) {
        this.disabledFeatures = disabledFeatures == null ? List.of() : disabledFeatures;
    }

    /**
     * Returns the names of the features disabled for the delegated agent.
     *
     * @return an unmodifiable view of the disabled-feature names; never
     *         {@code null}, possibly empty
     */
    public List<String> disabledFeatures() {
        return Collections.unmodifiableList(disabledFeatures);
    }
}
