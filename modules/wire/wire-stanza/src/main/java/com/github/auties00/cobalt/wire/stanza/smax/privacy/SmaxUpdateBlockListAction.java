package com.github.auties00.cobalt.wire.stanza.smax.privacy;

/**
 * Selects whether a {@link SmaxUpdateBlockListRequest} adds or removes the target JID from the blocklist.
 *
 * <p>This selector drives the block-or-unblock action surfaced by the chat-info and contact-info screens.
 */
public enum SmaxUpdateBlockListAction {
    /**
     * Adds the target JID to the blocklist.
     *
     * <p>Selected when the user taps Block; once the relay applies the action it rejects further sends to the
     * blocked target and stops delivering messages from them.
     */
    BLOCK("block"),

    /**
     * Removes the target JID from the blocklist.
     *
     * <p>Selected when the user taps Unblock; the relay re-enables bidirectional message delivery on success.
     */
    UNBLOCK("unblock");

    /**
     * The wire string serialised into the {@code action} attribute of the {@code <item>} child.
     */
    private final String wire;

    /**
     * Constructs an action constant.
     *
     * @param wire the wire string for this action
     */
    SmaxUpdateBlockListAction(String wire) {
        this.wire = wire;
    }

    /**
     * Returns the wire string for this action.
     *
     * <p>Used by {@link SmaxUpdateBlockListRequest#toStanza()} to serialise the action onto the outbound stanza;
     * callers outside the stanza builder should prefer the enum value over the raw wire string.
     *
     * @return the wire string; never {@code null}
     */
    public String wire() {
        return wire;
    }
}
