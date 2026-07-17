package com.github.auties00.cobalt.wire.linked.sync.action.bot;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that synchronises a user-generated content (UGC) bot
 * definition across linked devices.
 *
 * <p>UGC bots are custom WhatsApp bots authored by users (as opposed to
 * first-party bots provided by Meta). Their full definition, including the
 * system prompt, persona configuration, and any associated metadata, is
 * encoded as an opaque binary blob and carried by this action so that every
 * linked device sees the same bot configuration.
 *
 * <p>The payload is stored in the high-priority regular sync collection,
 * which ensures that bot definitions propagate promptly to companion devices
 * after they are created or edited on the primary device.
 */
@ProtobufMessage(name = "SyncActionValue.UGCBot")
public final class UGCBotAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name used to identify this sync action on the wire.
     */
    public static final String ACTION_NAME = "ugc_bot";

    /**
     * The canonical action version negotiated by the WhatsApp sync protocol
     * for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that stores this action on the server.
     *
     * <p>UGC bot definitions are placed in the high-priority regular
     * collection so that changes made on one device are visible on linked
     * devices with minimal delay.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name used to identify this sync action.
     *
     * @return the action name string
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version negotiated for this sync action.
     *
     * @return the action version integer
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The opaque serialised UGC bot definition, or {@code null} if the action
     * does not carry a payload.
     *
     * <p>The bytes are an internal WhatsApp-defined encoding of the bot's
     * configuration and are treated as a pass-through blob by Cobalt.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] definition;


    /**
     * Constructs a new {@code UGCBotAction} carrying the supplied bot
     * definition blob.
     *
     * @param definition the opaque serialised bot definition, or {@code null}
     *                   if no payload is available
     */
    UGCBotAction(byte[] definition) {
        this.definition = definition;
    }

    /**
     * Returns the opaque serialised bot definition carried by this action.
     *
     * @return an {@link Optional} containing the raw bot definition bytes,
     *         or {@link Optional#empty()} if the payload is absent
     */
    public Optional<byte[]> definition() {
        return Optional.ofNullable(definition);
    }

    /**
     * Updates the opaque serialised bot definition carried by this action.
     *
     * @param definition the new bot definition bytes, or {@code null} to
     *                   clear the payload
     */
    public void setDefinition(byte[] definition) {
        this.definition = definition;
    }
}
