package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the Labyrinth inbox snapshot query built by
 * {@link DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlRequest}.
 *
 * <p>This is a debug/diagnostic operation returning a raw mailbox snapshot rather than a stable
 * domain projection, so the response deliberately exposes the {@code get_wa_mailbox} root as an opaque
 * {@link JSONObject} instead of modelling its interior. Callers inspecting the snapshot read the tree
 * directly.
 *
 * @see DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebDebugLabyrinthInboxSnapshotQuery")
public final class DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the raw {@code get_wa_mailbox} snapshot object, or {@code null} when the relay omitted it.
     */
    private final JSONObject mailbox;

    /**
     * Constructs a response wrapping the raw mailbox snapshot.
     *
     * <p>Reserved for the static parser.
     *
     * @param mailbox the raw {@code get_wa_mailbox} snapshot object, or {@code null} when the relay
     *                omitted it
     */
    private DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlResponse(JSONObject mailbox) {
        this.mailbox = mailbox;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the {@code get_wa_mailbox} root and retains it verbatim; the returned {@link Optional}
     * is empty only when {@code data} is {@code null}. A missing {@code get_wa_mailbox} object still
     * yields a response, surfaced as an empty {@link #mailbox()}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var mailbox = data.getJSONObject("get_wa_mailbox");
        return Optional.of(new DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlResponse(mailbox));
    }

    /**
     * Returns the raw {@code get_wa_mailbox} snapshot object.
     *
     * @return an {@link Optional} wrapping the raw mailbox snapshot, or {@link Optional#empty()} when
     *         the relay omitted the {@code get_wa_mailbox} object
     */
    public Optional<JSONObject> mailbox() {
        return Optional.ofNullable(mailbox);
    }
}
