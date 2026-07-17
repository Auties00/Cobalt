package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the relay query that fetches a Labyrinth inbox snapshot for diagnostics.
 *
 * <p>The operation takes a single {@code params} GraphQL input object carrying two integer page
 * sizes: {@code messageFirst}, the number of messages to include per thread, and {@code threadFirst},
 * the number of threads to include in the snapshot. WhatsApp Web's fetcher forwards both straight
 * through as {@code {"params": {"messageFirst": <int>, "threadFirst": <int>}}}. The relay returns the
 * raw mailbox snapshot under {@code get_wa_mailbox}; the reply is consumed through
 * {@link DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlResponse}.
 *
 * @see DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebDebugLabyrinthInboxSnapshotQuery")
public final class DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebDebugLabyrinthInboxSnapshotQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27416732057922291";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebDebugLabyrinthInboxSnapshotQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebDebugLabyrinthInboxSnapshotQuery";

    /**
     * The number of messages to include per thread in the snapshot.
     *
     * <p>Emitted as the {@code messageFirst} field of the {@code params} input object.
     */
    private final int messageFirst;

    /**
     * The number of threads to include in the snapshot.
     *
     * <p>Emitted as the {@code threadFirst} field of the {@code params} input object.
     */
    private final int threadFirst;

    /**
     * Constructs a Labyrinth inbox snapshot query request carrying the two snapshot page sizes.
     *
     * @param messageFirst the number of messages to include per thread
     * @param threadFirst  the number of threads to include in the snapshot
     */
    public DebugLabyrinthInboxSnapshotWhatsAppWebGraphQlRequest(int messageFirst, int threadFirst) {
        this.messageFirst = messageFirst;
        this.threadFirst = threadFirst;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits
     * {@code {"params": {"messageFirst": <messageFirst>, "threadFirst": <threadFirst>}}}, always
     * writing both integer fields of the nested {@code params} object.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("params");
            writer.writeColon();
            writer.startObject();
            writer.writeName("messageFirst");
            writer.writeColon();
            writer.writeInt32(messageFirst);
            writer.writeName("threadFirst");
            writer.writeColon();
            writer.writeInt32(threadFirst);
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
