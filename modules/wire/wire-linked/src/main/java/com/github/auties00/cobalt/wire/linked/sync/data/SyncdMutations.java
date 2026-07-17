package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Container wrapping an ordered list of {@link SyncdMutation} entries.
 *
 * <p>Used when mutations are shipped outside a full {@link SyncdPatch}, for
 * instance in external mutation blobs fetched by a separate media download:
 * the reference in the patch points at a blob whose decoded content is this
 * message. The ordering of the list is significant because mutations are
 * applied sequentially.
 */
@ProtobufMessage(name = "SyncdMutations")
public final class SyncdMutations {
    /**
     * Ordered list of mutations to apply.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<SyncdMutation> mutations;


    /**
     * Constructs a new wrapper around the given mutation list.
     *
     * @param mutations the mutations to wrap
     */
    SyncdMutations(List<SyncdMutation> mutations) {
        this.mutations = mutations;
    }

    /**
     * Returns the mutation list in the order it should be applied.
     *
     * @return an unmodifiable list of mutations, never {@code null}
     */
    public List<SyncdMutation> mutations() {
        return mutations == null ? List.of() : Collections.unmodifiableList(mutations);
    }

    /**
     * Sets the mutation list.
     *
     * @param mutations the ordered mutations
     */
    public void setMutations(List<SyncdMutation> mutations) {
        this.mutations = mutations;
    }
}
