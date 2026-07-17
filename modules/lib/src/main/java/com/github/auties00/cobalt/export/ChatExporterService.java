package com.github.auties00.cobalt.export;

import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatExportOptions;

import java.io.OutputStream;

/**
 * Service that exports a chat history into a WhatsApp-format ZIP archive.
 *
 * <p>Implementations write the archive directly to the caller-provided
 * {@link OutputStream}. The stream is flushed and left open for the caller to
 * close. Implementations also commit the {@code ChatExport} telemetry metric
 * describing the outcome of the export.
 */
public interface ChatExporterService {
    /**
     * Exports the given chat into the supplied stream and commits the
     * {@code ChatExport} metric describing the outcome.
     *
     * @param chat      the chat to export
     * @param chatTitle the title to print in the transcript header
     * @param options   the export options
     * @param output    the stream that receives the ZIP archive
     * @throws NullPointerException if any required argument is {@code null}
     */
    void exportChat(Chat chat, String chatTitle, ChatExportOptions options, OutputStream output);
}