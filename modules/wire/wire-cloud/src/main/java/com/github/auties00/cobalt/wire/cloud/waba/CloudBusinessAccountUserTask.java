package com.github.auties00.cobalt.wire.cloud.waba;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * A permission task granted to a user assigned to a WhatsApp Business Account.
 *
 * <p>When a business adds a system user or a person to its WhatsApp Business Account it grants a set of
 * tasks that scope what the user may do: manage the account, develop against it, manage templates or
 * phone numbers, view costs, and so on. The same task set is also reported back for each already-assigned
 * user. The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudBusinessAccountUserTask {
    /**
     * A task that this client does not recognise. Resolved for any token outside the modelled set so that
     * an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * Full management of the WhatsApp Business Account.
     */
    MANAGE(1),

    /**
     * Development access against the WhatsApp Business Account.
     */
    DEVELOP(2),

    /**
     * Management of the account's message templates.
     */
    MANAGE_TEMPLATES(3),

    /**
     * Management of the account's phone numbers.
     */
    MANAGE_PHONE(4),

    /**
     * Read access to the account's cost and billing information.
     */
    VIEW_COST(5),

    /**
     * Management of the account's extensions.
     */
    MANAGE_EXTENSIONS(6),

    /**
     * Read access to a phone number's assets.
     */
    VIEW_PHONE_ASSETS(7),

    /**
     * Management of a phone number's assets.
     */
    MANAGE_PHONE_ASSETS(8),

    /**
     * Read access to the account's message templates.
     */
    VIEW_TEMPLATES(9),

    /**
     * Permission to send and receive messages on the account.
     */
    MESSAGING(10);

    /**
     * The protobuf-assigned numeric index for this task.
     */
    final int index;

    /**
     * Constructs a {@code CloudBusinessAccountUserTask} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudBusinessAccountUserTask(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudBusinessAccountUserTask} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any unrecognised
     * or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on an unexpected
     * value.
     *
     * @param input the wire token, for example {@code "MANAGE_TEMPLATES"}, or {@code null}
     * @return the matching task, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudBusinessAccountUserTask of(String input) {
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
     * Returns the protobuf-assigned numeric index for this task.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
