package com.github.auties00.cobalt.wire.cloud.flow;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The kind of an asset attached to a WhatsApp Cloud API Flow.
 *
 * <p>A Flow carries one or more assets; the {@link #FLOW_JSON} asset holds the Flow JSON document that
 * defines the Flow's screens and routing. The {@link #UNKNOWN} constant guards against tokens this
 * client does not yet model.
 */
@ProtobufEnum
public enum CloudFlowAssetType {
    /**
     * An asset kind that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The Flow JSON document defining the Flow's screens and routing.
     */
    FLOW_JSON(1);

    /**
     * The protobuf-assigned numeric index for this asset kind.
     */
    final int index;

    /**
     * Constructs a {@code CloudFlowAssetType} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudFlowAssetType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudFlowAssetType} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "FLOW_JSON"}, or {@code null}
     * @return the matching asset kind, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudFlowAssetType of(String input) {
        if (input == null) {
            return UNKNOWN;
        }
        for (var value : values()) {
            if (value != UNKNOWN && value.name().equalsIgnoreCase(input)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the WhatsApp wire token for this asset kind.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this asset kind.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
