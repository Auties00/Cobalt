package com.github.auties00.cobalt.export;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatExportOptions;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo.StubType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.FutureProofMessageType;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.commerce.ProductMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LiveLocationMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.*;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.system.KeepInChatMessage;
import com.github.auties00.cobalt.wire.linked.message.system.PinInChatMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wam.WamMsgUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ChatExportEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMessageQueryEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ExportModeType;
import com.github.auties00.cobalt.wire.wam.type.ExportResultType;
import com.github.auties00.cobalt.wire.wam.type.WebcChatType;
import com.github.auties00.cobalt.wire.wam.type.WebcMessageQueryDirection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Production {@link ChatExporterService} that builds a self-contained archive of a single chat's history.
 *
 * <p>An exporter renders the messages of a {@link Chat} into two textual
 * transcripts, a plain-text {@code chat.txt} and a Markdown {@code chat.md},
 * and optionally bundles the chat's media attachments under a {@code media/}
 * folder. The artefacts are packed directly into the {@link OutputStream}
 * supplied by the caller.
 *
 * <p>The export pipeline mirrors WhatsApp Web's "Export chat" action: it
 * drops protocol, reaction, poll-update, keep, pin, view-once and ephemeral
 * messages, optionally filters by a timestamp range, sorts the survivors
 * chronologically, caps them at the configured limit (keeping the most
 * recent), downloads any included media, and finally formats the two
 * transcripts. Media downloads are best-effort: a download failure or an
 * over-sized attachment is skipped and rendered as an "omitted" placeholder
 * rather than failing the whole export.
 *
 * @see ChatExportOptions
 */
@WhatsAppWebModule(moduleName = "WAWebExportChatAction")
public final class LiveChatExporterService implements ChatExporterService {
    /**
     * The logger for {@link LiveChatExporterService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveChatExporterService.class);

    /**
     * The maximum size, in bytes, of a media attachment that the exporter
     * will download. Attachments larger than this are skipped and rendered
     * as omitted.
     */
    private static final long MAX_MEDIA_BYTES = 15L * 1024 * 1024;

    /**
     * The forwarding-score threshold at or above which a message is treated
     * as "frequently forwarded" rather than merely "forwarded".
     */
    private static final int FREQUENTLY_FORWARDED_SCORE = 4;

    /**
     * The formatter for the Markdown per-day section headers, for example
     * "January 5, 2026".
     */
    private static final DateTimeFormatter MARKDOWN_DAY_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    /**
     * The formatter for the Markdown per-message time, for example
     * "3:05 PM".
     */
    private static final DateTimeFormatter MARKDOWN_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    /**
     * The formatter for the Markdown export-date header, for example
     * "January 5, 2026, 3:05 PM".
     */
    private static final DateTimeFormatter MARKDOWN_HEADER_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", Locale.US);

    /**
     * The client used to download media attachments referenced by the
     * exported messages.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The telemetry service that receives the {@code ChatExport} metric
     * committed at the end of every export.
     */
    private final WamService wamService;

    /**
     * Constructs a new exporter bound to the given client and telemetry
     * service.
     *
     * @param client     the client used to download media attachments; must
     *                   not be {@code null}
     * @param wamService the telemetry service that receives the
     *                   {@code ChatExport} metric; must not be {@code null}
     * @throws NullPointerException if {@code client} or {@code wamService} is
     *                              {@code null}
     */
    public LiveChatExporterService(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client");
        this.wamService = Objects.requireNonNull(wamService, "wamService");
    }

    /**
     * Exports the given chat into a ZIP archive and commits the
     * {@code ChatExport} telemetry metric describing the outcome.
     *
     * <p>The written archive always contains {@code chat.txt} and
     * {@code chat.md}; it additionally contains one {@code media/<name>}
     * entry per successfully downloaded attachment when
     * {@link ChatExportOptions#includeMedia()} is {@code true}. The message
     * count reported on the committed metric is the number of messages that
     * survived filtering and the cap, not the raw size of the chat. A
     * completed export commits a {@code SUCCESS} metric carrying the message
     * count, media count, archive byte size and duration; a failure commits
     * an {@code ERROR} metric carrying the failure reason before the original
     * exception is rethrown.
     *
     * @param chat      the chat to export; must not be {@code null}
     * @param chatTitle the human-readable title to print in the archive
     *                  header, or {@code null} to fall back to the chat name
     *                  and then to the chat JID
     * @param options   the export configuration; must not be {@code null}
     * @param output    the stream that receives the ZIP archive
     * @throws NullPointerException if {@code chat}, {@code options} or
     *                              {@code output} is {@code null}
     * @throws UncheckedIOException if writing the archive fails
     * @implNote This implementation keeps the newest {@code messageLimit}
     *           messages when the cap is exceeded, so the
     *           "earlier messages unavailable" notice is accurate, and it
     *           treats a missing timestamp as the epoch for both filtering
     *           and ordering.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebExportChatAction", exports = "exportChat", adaptation = WhatsAppAdaptation.ADAPTED)
    public void exportChat(Chat chat, String chatTitle, ChatExportOptions options, OutputStream output) {
        Objects.requireNonNull(chat, "chat");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(output, "output");
        var chatType = WamMsgUtils.getWamChatType(chat.jid());
        var exportMode = options.includeMedia() ? ExportModeType.WITH_MEDIA : ExportModeType.TEXT_ONLY;
        var dateRangeUsed = options.startDate().isPresent() || options.endDate().isPresent();
        var startMs = System.currentTimeMillis();
        try {
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "chat export started: chat={0}, mode={1}, dateRangeUsed={2}", chat.jid(), exportMode, dateRangeUsed);
            }
            var title = resolveTitle(chat, chatTitle);

            List<ChatMessageInfo> collected;
            var queryStartMs = System.currentTimeMillis();
            try (var stream = chat.messages()) {
                collected = stream.toList();
            }
            commitMessageQuery(chat.jid(), collected, System.currentTimeMillis() - queryStartMs);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat export loaded {0} messages for {1}", collected.size(), chat.jid());

            var start = options.startDate().orElse(null);
            var end = options.endDate().orElse(null);
            var filtered = new ArrayList<ChatMessageInfo>();
            for (var info : collected) {
                if (isExcludedType(info) || isViewOnce(info) || isEphemeral(info)) {
                    continue;
                }
                var when = messageInstant(info);
                if (start != null && when.isBefore(start)) {
                    continue;
                }
                if (end != null && when.isAfter(end)) {
                    continue;
                }
                filtered.add(info);
            }
            filtered.sort(Comparator.comparingLong(LiveChatExporterService::epochSecond));

            var limit = Math.max(0, options.messageLimit());
            var hasMoreHistory = filtered.size() > limit;
            var messages = hasMoreHistory
                    ? filtered.subList(filtered.size() - limit, filtered.size())
                    : filtered;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "chat export filtered to {0} messages, capped to {1}, hasMoreHistory={2}",
                        filtered.size(), messages.size(), hasMoreHistory);
            }

            var downloadedMediaMsgIds = new HashSet<String>();
            var mediaCount = 0;
            var countingOutput = new CountingOutputStream(output);
            try (var zip = new ZipOutputStream(countingOutput)) {
                if (options.includeMedia()) {
                    for (var info : messages) {
                        if (downloadMedia(info, zip, downloadedMediaMsgIds)) {
                            mediaCount++;
                        }
                    }
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat export bundled {0} media attachments", mediaCount);
                }

                var txt = formatPlainText(messages, options.includeMedia(), hasMoreHistory, downloadedMediaMsgIds)
                        .getBytes(StandardCharsets.UTF_8);
                var md = formatMarkdown(title, messages, options.includeMedia(), hasMoreHistory, downloadedMediaMsgIds, "media")
                        .getBytes(StandardCharsets.UTF_8);
                putEntry(zip, "chat.txt", txt);
                putEntry(zip, "chat.md", md);
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to assemble the chat export archive", exception);
            }

            var durationMs = System.currentTimeMillis() - startMs;
            wamService.commit(new ChatExportEventBuilder()
                    .chatType(chatType)
                    .exportMode(exportMode)
                    .exportResult(ExportResultType.SUCCESS)
                    .exportMessageCount(messages.size())
                    .mediaCount(mediaCount)
                    .exportFileSizeBytes((int) countingOutput.bytesWritten())
                    .exportDurationMs((int) durationMs)
                    .exportDateRangeUsed(dateRangeUsed ? 1 : 0)
                    .build());
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "chat export completed: chat={0}, messages={1}, media={2}, bytes={3}, durationMs={4}",
                        chat.jid(), messages.size(), mediaCount, countingOutput.bytesWritten(), durationMs);
            }
        } catch (RuntimeException error) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "chat export failed for " + new LogRedactable.User(chat.jid().toString()), error);
            }
            wamService.commit(new ChatExportEventBuilder()
                    .chatType(chatType)
                    .exportMode(exportMode)
                    .exportResult(ExportResultType.ERROR)
                    .exportErrorReason(error.getMessage())
                    .exportDurationMs((int) (System.currentTimeMillis() - startMs))
                    .exportDateRangeUsed(dateRangeUsed ? 1 : 0)
                    .build());
            throw error;
        }
    }

    /**
     * Commits one {@code WebcMessageQuery} telemetry metric measuring the
     * message-store scan that loaded this chat's history.
     *
     * <p>The exporter loads a chat's messages with a single
     * {@link Chat#messages()} scan; this measures that scan the way WhatsApp
     * Web measures each of its paged message-store queries, right after the
     * load resolves. The committed metric carries the real result count, the
     * wall-clock query duration, the {@link WebcChatType} classification, and
     * the per-type breakdown of the loaded messages. The direction is always
     * {@link WebcMessageQueryDirection#LOAD_PREV} because a full-history export
     * reads earlier messages, mirroring WhatsApp Web's {@code before} load
     * path. The browser-only dimensions WhatsApp Web attaches (effective
     * network type, storage-quota estimate, chat-list position, wire-response
     * byte size) are inherently absent in a headless client and are therefore
     * omitted.
     *
     * @param chatJid         the JID of the chat that was scanned
     * @param results         the messages returned by the scan
     * @param queryDurationMs the wall-clock duration of the scan, in
     *                        milliseconds
     */
    @WhatsAppWebExport(moduleName = "WAWebChatLoadMessages", exports = "loadMsgsPromiseLoop", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMessageQuery(Jid chatJid, List<ChatMessageInfo> results, long queryDurationMs) {
        var query = new WebcMessageQueryEventBuilder()
                .webcChatType(webcChatType(chatJid))
                .webcMessageQueryType(WebcMessageQueryDirection.LOAD_PREV)
                .webcMessageCount(results.size())
                .webcQueryT(Instant.ofEpochMilli(queryDurationMs));
        classifyQueryResult(results, query);
        wamService.commit(query.build());
    }

    /**
     * Classifies a chat JID into the WAM {@link WebcChatType} dimension.
     *
     * <p>Newsletter, group or community, and broadcast servers map to
     * {@link WebcChatType#NEWSLETTER}, {@link WebcChatType#GROUP} and
     * {@link WebcChatType#BROADCAST_LIST} respectively; every other server
     * (user or LID) maps to {@link WebcChatType#INDIVIDUAL}. Communities are
     * reported as {@link WebcChatType#GROUP} because a community and a plain
     * group are indistinguishable from the JID server alone.
     *
     * @param chatJid the chat JID to classify
     * @return the WAM chat-type classification, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebChatModel", exports = "getWebcChatType", adaptation = WhatsAppAdaptation.ADAPTED)
    private static WebcChatType webcChatType(Jid chatJid) {
        if (chatJid.hasNewsletterServer()) {
            return WebcChatType.NEWSLETTER;
        }
        if (chatJid.hasGroupOrCommunityServer()) {
            return WebcChatType.GROUP;
        }
        if (chatJid.hasBroadcastServer()) {
            return WebcChatType.BROADCAST_LIST;
        }
        return WebcChatType.INDIVIDUAL;
    }

    /**
     * Tallies the per-type message counts of a query result onto the given
     * {@code WebcMessageQuery} builder.
     *
     * <p>The buckets mirror WhatsApp Web's {@code logMessageCounts}: the text
     * bucket absorbs plain text, location and contact payloads; the photo
     * bucket absorbs images and product cards; audio and voice notes split on
     * the push-to-talk flag; and every unclassified payload falls through to
     * the other bucket.
     *
     * @param results the messages returned by the scan
     * @param query   the builder accumulating the per-type counts
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgCountReporter", exports = "logMessageCounts", adaptation = WhatsAppAdaptation.ADAPTED)
    private static void classifyQueryResult(List<ChatMessageInfo> results, WebcMessageQueryEventBuilder query) {
        var text = 0L;
        var photo = 0L;
        var video = 0L;
        var audio = 0L;
        var ptt = 0L;
        var sticker = 0L;
        var document = 0L;
        var other = 0L;
        for (var info : results) {
            switch (info.message().content()) {
                case ExtendedTextMessage ignored -> text++;
                case LocationMessage ignored -> text++;
                case LiveLocationMessage ignored -> text++;
                case ContactMessage ignored -> text++;
                case ContactsArrayMessage ignored -> text++;
                case ImageMessage ignored -> photo++;
                case ProductMessage ignored -> photo++;
                case VideoMessage ignored -> video++;
                case AudioMessage audioMessage -> {
                    if (audioMessage.ptt()) {
                        ptt++;
                    } else {
                        audio++;
                    }
                }
                case StickerMessage ignored -> sticker++;
                case DocumentMessage ignored -> document++;
                case null, default -> other++;
            }
        }
        query.webcTextMessageCount(text)
                .webcPhotoMessageCount(photo)
                .webcVideoMessageCount(video)
                .webcAudioMessageCount(audio)
                .webcPttMessageCount(ptt)
                .webcStickerMessageCount(sticker)
                .webcDocumentMessageCount(document)
                .webcOtherMessageCount(other);
    }

    /**
     * Resolves the archive title, preferring the explicit argument, then the
     * chat name, then the chat JID.
     *
     * @param chat      the chat being exported
     * @param chatTitle the explicit title, or {@code null}
     * @return a non-null title string
     */
    private static String resolveTitle(Chat chat, String chatTitle) {
        if (chatTitle != null && !chatTitle.isBlank()) {
            return chatTitle;
        }
        return chat.name()
                .filter(name -> !name.isBlank())
                .orElseGet(() -> chat.jid().toString());
    }

    /**
     * Downloads the media attachment of a single message into the archive,
     * if the message carries one and the attachment is within the size
     * limit.
     *
     * <p>The size limit is enforced both before the download, using the
     * declared file length when present, and after the download, using the
     * actual byte count. Any failure while fetching media is swallowed so
     * that one bad attachment never fails the whole export; archive write
     * failures are propagated.
     *
     * @param info                  the message whose attachment is fetched
     * @param zip                   the archive stream receiving the media entry
     * @param downloadedMediaMsgIds the accumulator of message-key ids whose
     *                              attachment was successfully bundled
     * @return {@code true} if the media was bundled
     * @throws IOException if writing the ZIP entry fails
     */
    private boolean downloadMedia(ChatMessageInfo info, ZipOutputStream zip, Set<String> downloadedMediaMsgIds) throws IOException {
        var content = info.message().content();
        if (!(content instanceof MediaMessage media)) {
            return false;
        }
        if (media.fileLength().isPresent() && media.fileLength().getAsLong() > MAX_MEDIA_BYTES) {
            return false;
        }
        byte[] bytes;
        try {
            try (var in = client.downloadMedia(media)) {
                bytes = in.readNBytes((int) MAX_MEDIA_BYTES + 1);
            }
        } catch (Exception exception) {
            // A single failed media transfer is non-fatal: the message is
            // still exported, rendered as an omitted attachment.
            if (Log.WARNING) LOGGER.log(Level.WARNING, "chat export media download failed, omitting attachment", exception);
            return false;
        }
        if (bytes.length > MAX_MEDIA_BYTES) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "chat export media exceeds size limit, omitting attachment: bytes={0}", bytes.length);
            return false;
        }
        putEntry(zip, "media/" + mediaFileName(info, content), bytes);
        info.key().id().ifPresent(downloadedMediaMsgIds::add);
        return true;
    }

    /**
     * Writes a single entry into the given ZIP stream.
     *
     * @param zip  the open ZIP stream
     * @param name the entry name
     * @param data the entry bytes
     * @throws IOException if the underlying stream rejects the write
     */
    private static void putEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    /**
     * Renders the surviving messages as a plain-text transcript.
     *
     * <p>The transcript opens with the standard end-to-end-encryption notice
     * (when there is at least one message) and, when history was capped, a
     * note that earlier messages may be missing. Each subsequent line is a
     * bracketed timestamp followed either by a system event or by a
     * sender-prefixed body.
     *
     * @param messages      the messages to render, already filtered, sorted
     *                      and capped
     * @param includeMedia  whether media was requested for this export
     * @param hasMoreHistory whether messages were dropped by the cap
     * @param downloadedIds the message-key ids whose media was bundled
     * @return the transcript text, terminated by a trailing newline
     */
    @WhatsAppWebExport(moduleName = "WAWebExportChatPlainTextFormatter", exports = "formatChatAsPlainText", adaptation = WhatsAppAdaptation.ADAPTED)
    private String formatPlainText(List<ChatMessageInfo> messages, boolean includeMedia, boolean hasMoreHistory, Set<String> downloadedIds) {
        var lines = new ArrayList<String>();
        if (!messages.isEmpty()) {
            lines.add("Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them.");
        }
        if (hasMoreHistory) {
            lines.add("Some earlier messages may not be available.");
        }
        for (var info : messages) {
            var timestamp = plainTimestamp(info);
            if (isSystem(info)) {
                lines.add("[" + timestamp + "] - " + plainBody(info, includeMedia, downloadedIds));
                continue;
            }
            var body = applyForwardPrefix(info, plainBody(info, includeMedia, downloadedIds));
            lines.add("[" + timestamp + "] " + senderName(info) + ": " + body);
        }
        return String.join("\n", lines) + "\n";
    }

    /**
     * Renders the surviving messages as a Markdown transcript.
     *
     * <p>The transcript opens with a level-one title, the export date, the
     * encryption notice and, when applicable, the capped-history note. Days
     * are separated by level-two headers, and each line is a bracketed time
     * followed either by a system event or by a bold sender and a decorated
     * body.
     *
     * @param title         the title to print in the header
     * @param messages      the messages to render, already filtered, sorted
     *                      and capped
     * @param includeMedia  whether media was requested for this export
     * @param hasMoreHistory whether messages were dropped by the cap
     * @param downloadedIds the message-key ids whose media was bundled
     * @param mediaDir      the archive folder name under which media is
     *                      stored, used to build relative links
     * @return the transcript text, terminated by a trailing newline
     */
    @WhatsAppWebExport(moduleName = "WAWebExportChatMarkdownFormatter", exports = "formatChatAsMarkdown", adaptation = WhatsAppAdaptation.ADAPTED)
    private String formatMarkdown(String title, List<ChatMessageInfo> messages, boolean includeMedia, boolean hasMoreHistory, Set<String> downloadedIds, String mediaDir) {
        var zone = ZoneId.systemDefault();
        var lines = new ArrayList<String>();
        lines.add("# WhatsApp Chat Export: " + title);
        lines.add("");
        lines.add("Export date: " + MARKDOWN_HEADER_DATE.format(Instant.now().atZone(zone)));
        if (!messages.isEmpty()) {
            lines.add("");
            lines.add("_Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them._");
        }
        if (hasMoreHistory) {
            lines.add("_Some earlier messages may not be available._");
        }
        LocalDate currentDay = null;
        for (var info : messages) {
            var dateTime = messageInstant(info).atZone(zone);
            var day = dateTime.toLocalDate();
            if (!day.equals(currentDay)) {
                currentDay = day;
                lines.add("");
                lines.add("## " + MARKDOWN_DAY_DATE.format(dateTime));
                lines.add("");
            }
            var time = MARKDOWN_TIME.format(dateTime);
            if (isSystem(info)) {
                lines.add("[" + time + "] " + markdownBody(info, includeMedia, downloadedIds, mediaDir));
                continue;
            }
            var sender = senderName(info);
            var body = applyForwardPrefix(info, markdownBody(info, includeMedia, downloadedIds, mediaDir));
            var quote = markdownQuote(info);
            if (!quote.isEmpty()) {
                lines.add("[" + time + "] **" + sender + ":**");
                lines.add(quote.stripTrailing());
                lines.add(body);
            } else {
                lines.add("[" + time + "] **" + sender + ":** " + body);
            }
        }
        return String.join("\n", lines) + "\n";
    }

    /**
     * Renders the plain-text body of a single message.
     *
     * @param info          the message to render
     * @param includeMedia  whether media was requested for this export
     * @param downloadedIds the message-key ids whose media was bundled
     * @return the body text, never {@code null}
     */
    private String plainBody(ChatMessageInfo info, boolean includeMedia, Set<String> downloadedIds) {
        if (isRevoked(info)) {
            return info.key().fromMe() ? "You deleted this message" : "This message was deleted";
        }
        if (isSystem(info)) {
            return systemText(info);
        }
        var content = info.message().content();
        if (content instanceof MediaMessage) {
            return plainMediaBody(info, content, includeMedia, downloadedIds);
        }
        if (content instanceof LocationMessage || content instanceof LiveLocationMessage) {
            return locationBody(content);
        }
        if (content instanceof ContactMessage contact) {
            return contact.displayName()
                    .filter(name -> !name.isBlank())
                    .map(name -> "Contact: " + name)
                    .orElse("<Contact card omitted>");
        }
        if (content instanceof ContactsArrayMessage contacts) {
            var count = contacts.contacts().size();
            return count > 0 ? count + " contacts shared" : "<Contact card omitted>";
        }
        if (content instanceof PollCreationMessage poll) {
            return plainPollBody(poll);
        }
        if (content instanceof ExtendedTextMessage text) {
            return text.text().filter(value -> !value.isEmpty()).orElse("<message>");
        }
        return "<message>";
    }

    /**
     * Renders the Markdown body of a single message.
     *
     * @param info          the message to render
     * @param includeMedia  whether media was requested for this export
     * @param downloadedIds the message-key ids whose media was bundled
     * @param mediaDir      the archive folder name used to build links
     * @return the body text, never {@code null}
     */
    private String markdownBody(ChatMessageInfo info, boolean includeMedia, Set<String> downloadedIds, String mediaDir) {
        if (isRevoked(info)) {
            return info.key().fromMe() ? "_You deleted this message_" : "_This message was deleted_";
        }
        if (isSystem(info)) {
            return "__" + systemText(info) + "__";
        }
        var content = info.message().content();
        if (content instanceof MediaMessage) {
            return markdownMediaBody(info, content, includeMedia, downloadedIds, mediaDir);
        }
        if (content instanceof LocationMessage || content instanceof LiveLocationMessage) {
            return locationBody(content);
        }
        if (content instanceof ContactMessage contact) {
            return contact.displayName()
                    .filter(name -> !name.isBlank())
                    .map(name -> "[Contact: " + name + "]")
                    .orElse("[Contact card]");
        }
        if (content instanceof ContactsArrayMessage contacts) {
            var count = contacts.contacts().size();
            return count > 0 ? "[" + count + " contacts shared]" : "[Contact card]";
        }
        if (content instanceof PollCreationMessage poll) {
            return markdownPollBody(poll);
        }
        if (content instanceof ExtendedTextMessage text) {
            return text.text().filter(value -> !value.isEmpty()).orElse("[message]");
        }
        return "[message]";
    }

    /**
     * Renders the plain-text body of a media message.
     *
     * @param info          the media message to render
     * @param content       the unwrapped media content
     * @param includeMedia  whether media was requested for this export
     * @param downloadedIds the message-key ids whose media was bundled
     * @return the body text, never {@code null}
     */
    private static String plainMediaBody(ChatMessageInfo info, Message content, boolean includeMedia, Set<String> downloadedIds) {
        var downloaded = isDownloaded(info, includeMedia, downloadedIds);
        if (content instanceof VideoMessage video && video.gifPlayback()) {
            return downloaded ? "GIF attached" : "<GIF omitted>";
        }
        var type = typeName(content);
        var fileName = content instanceof DocumentMessage document
                ? document.fileName().filter(name -> !name.isBlank()).orElse(type)
                : type;
        var caption = mediaCaption(content).filter(value -> !value.isBlank()).orElse(null);
        if (downloaded) {
            return caption != null ? fileName + " (file attached) " + caption : fileName + " (file attached)";
        }
        var omitted = type + " omitted";
        return caption != null ? "<" + omitted + "> " + caption : "<" + omitted + ">";
    }

    /**
     * Renders the Markdown body of a media message as a link with an
     * optional caption.
     *
     * @param info          the media message to render
     * @param content       the unwrapped media content
     * @param includeMedia  whether media was requested for this export
     * @param downloadedIds the message-key ids whose media was bundled
     * @param mediaDir      the archive folder name used to build the link
     * @return the body text, never {@code null}
     */
    private static String markdownMediaBody(ChatMessageInfo info, Message content, boolean includeMedia, Set<String> downloadedIds, String mediaDir) {
        var downloaded = isDownloaded(info, includeMedia, downloadedIds);
        var type = markdownTypeName(content);
        var link = downloaded
                ? "[" + type + "](" + mediaDir + "/" + mediaFileName(info, content) + ")"
                : "[" + type + "]";
        var caption = mediaCaption(content).filter(value -> !value.isBlank()).orElse(null);
        return caption != null ? link + " " + caption : link;
    }

    /**
     * Renders a location or live-location message, shared by both
     * transcripts.
     *
     * @param content the unwrapped location content
     * @return the body text, never {@code null}
     */
    private static String locationBody(Message content) {
        var live = content instanceof LiveLocationMessage
                || (content instanceof LocationMessage location && location.isLive());
        var name = live ? "Live location" : "Location";
        OptionalDouble latitude;
        OptionalDouble longitude;
        String text;
        if (content instanceof LocationMessage location) {
            latitude = location.degreesLatitude();
            longitude = location.degreesLongitude();
            text = location.name().filter(value -> !value.isBlank()).orElse(null);
        } else if(content instanceof LiveLocationMessage location){
            latitude = location.degreesLatitude();
            longitude = location.degreesLongitude();
            text = location.caption().filter(value -> !value.isBlank()).orElse(null);
        } else {
            latitude = OptionalDouble.empty();
            longitude = OptionalDouble.empty();
            text = null;
        }
        if (latitude.isPresent() && longitude.isPresent()) {
            var lat = latitude.getAsDouble();
            var lng = longitude.getAsDouble();
            var prefix = text != null ? text + ": " : "";
            return prefix + name + ": " + lat + ", " + lng + " - https://maps.google.com/?q=" + lat + "," + lng;
        }
        return live ? "<Live location omitted>" : "<Location omitted>";
    }

    /**
     * Renders a poll-creation message as plain text: a "Poll" header and one
     * indented dash per option.
     *
     * @param poll the poll-creation message
     * @return the body text, never {@code null}
     */
    private static String plainPollBody(PollCreationMessage poll) {
        var name = poll.name().filter(value -> !value.isBlank()).orElse(null);
        var builder = new StringBuilder(name != null ? "Poll: " + name : "Poll");
        for (var option : poll.options()) {
            builder.append("\n    - ").append(option.optionName().orElse(""));
        }
        return builder.toString();
    }

    /**
     * Renders a poll-creation message as Markdown: a bold "Poll" header and
     * one dash-prefixed line per option.
     *
     * @param poll the poll-creation message
     * @return the body text, never {@code null}
     */
    private static String markdownPollBody(PollCreationMessage poll) {
        var name = poll.name().filter(value -> !value.isBlank()).orElse(null);
        var builder = new StringBuilder(name != null ? "**Poll: " + name + "**" : "**Poll**");
        for (var option : poll.options()) {
            builder.append("\n- ").append(option.optionName().orElse(""));
        }
        return builder.toString();
    }

    /**
     * Renders the Markdown blockquote that previews the message this message
     * replies to, when one is present.
     *
     * <p>The quote is taken from the per-content {@link ContextInfo}: a
     * quoted-message id with no preview content yields a placeholder, and a
     * present preview yields a single-line blockquote prefixed with the
     * quoted sender's user part. The returned value is either empty (no
     * reply) or a blockquote terminated by a blank line.
     *
     * @param info the message being rendered
     * @return the Markdown blockquote, or the empty string if the message is
     *         not a reply
     */
    private String markdownQuote(ChatMessageInfo info) {
        var context = contextInfo(info.message().content());
        if (context.isEmpty()) {
            return "";
        }
        var quotedId = context.get().quotedMessageId();
        var quotedContent = context.get().quotedMessageContent();
        if (quotedId.isEmpty() && quotedContent.isEmpty()) {
            return "";
        }
        if (quotedContent.isEmpty()) {
            return "> _[Original message not available]_\n\n";
        }
        var sender = context.get().quotedMessageSenderJid().map(Jid::user).orElse(null);
        var prefix = sender != null ? sender + ": " : "";
        return "> _" + prefix + quotedBody(quotedContent.get()) + "_\n\n";
    }

    /**
     * Renders a short single-line preview of a quoted message's content.
     *
     * @param container the quoted message container
     * @return the preview text, never {@code null}
     */
    private static String quotedBody(LinkedMessageContainer container) {
        if (container.isEmpty()) {
            return "This message was deleted";
        }
        var content = container.content();
        if (content instanceof MediaMessage) {
            var type = typeName(content);
            var caption = mediaCaption(content).filter(value -> !value.isBlank()).orElse(null);
            return caption != null ? type + ": " + caption : type;
        }
        if (content instanceof ExtendedTextMessage text) {
            return text.text()
                    .filter(value -> !value.isEmpty())
                    .orElseGet(() -> "[" + content.getClass().getSimpleName() + "]");
        }
        return "[" + content.getClass().getSimpleName() + "]";
    }

    /**
     * Resolves the human-readable label for a system (stub) message,
     * interpolating the actor and any participant identifiers.
     *
     * <p>This ports WhatsApp Web's {@code formatSystemMsgForExport}, which is
     * not a full stub catalogue but a focused mapping of the group,
     * encryption, disappearing-messages and call subtypes. Call subtypes
     * collapse to a single "[Call]" marker, and any unmapped subtype yields
     * the neutral "[System notification]".
     *
     * <p>Participant identifiers in the stub parameters are JIDs; the
     * headless exporter renders only their user part, since contact-name
     * lookup against a contact store is out of scope.
     *
     * @param info the system message
     * @return the label text, never {@code null}
     */
    private static String systemText(ChatMessageInfo info) {
        var type = info.messageStubType().orElse(StubType.UNKNOWN);
        var author = systemAuthor(info);
        var participants = joinParticipants(info.messageStubParameters());
        var name = info.messageStubParameters().stream().findFirst().orElse("");
        return switch (type) {
            case GROUP_CREATE -> author + " created the group";
            case GROUP_PARTICIPANT_ADD -> author + " added " + participants;
            case GROUP_PARTICIPANT_REMOVE -> author + " removed " + participants;
            case GROUP_PARTICIPANT_LEAVE -> participants + " left";
            case GROUP_PARTICIPANT_PROMOTE -> author + " made " + participants + " an admin";
            case GROUP_PARTICIPANT_DEMOTE -> author + " removed " + participants + " as admin";
            case GROUP_PARTICIPANT_INVITE -> participants + " joined via invite link";
            case GROUP_CHANGE_SUBJECT -> author + " changed the group name to '" + name + "'";
            case GROUP_CHANGE_DESCRIPTION -> name.isBlank()
                    ? author + " cleared the group description"
                    : author + " changed the group description";
            case GROUP_CHANGE_ICON -> author + " changed the group icon";
            case GROUP_CHANGE_ANNOUNCE -> author + " changed settings: only admins can send messages";
            case GROUP_CHANGE_RESTRICT -> author + " changed settings: only admins can edit group info";
            case CHANGE_EPHEMERAL_SETTING -> ephemeralTurnedOn(info)
                    ? author + " turned on disappearing messages"
                    : author + " turned off disappearing messages";
            case E2E_IDENTITY_CHANGED -> "Security code changed. Tap to learn more.";
            case E2E_ENCRYPTED -> "Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them. Tap to learn more.";
            case CALL_MISSED_VOICE, CALL_MISSED_VIDEO, CALL_MISSED_GROUP_VOICE, CALL_MISSED_GROUP_VIDEO -> "[Call]";
            default -> "[System notification]";
        };
    }

    /**
     * Resolves the actor of a system message: "You" for self-authored
     * events, otherwise the push name, falling back to the sender JID's user
     * part and finally to "Someone".
     *
     * @param info the system message
     * @return the actor label, never {@code null}
     */
    private static String systemAuthor(ChatMessageInfo info) {
        if (info.key().fromMe()) {
            return "You";
        }
        return info.pushName()
                .filter(name -> !name.isBlank())
                .orElseGet(() -> info.senderJid().map(Jid::user).orElse("Someone"));
    }

    /**
     * Joins participant JID strings WhatsApp-style, rendering each as its
     * user part: the empty string for none, the lone name for one,
     * "X and Y" for two, and "A, B, and C" for three or more.
     *
     * @param parameters the stub parameters, treated as JID strings
     * @return the joined participant list, never {@code null}
     */
    private static String joinParticipants(List<String> parameters) {
        var names = parameters.stream()
                .map(LiveChatExporterService::userPart)
                .filter(value -> !value.isBlank())
                .toList();
        return switch (names.size()) {
            case 0 -> "";
            case 1 -> names.get(0);
            case 2 -> names.get(0) + " and " + names.get(1);
            default -> String.join(", ", names.subList(0, names.size() - 1)) + ", and " + names.getLast();
        };
    }

    /**
     * Returns the user part of a JID string: the substring before the first
     * {@code '@'}, or the whole string when no {@code '@'} is present.
     *
     * @param jid the JID string
     * @return the user part
     */
    private static String userPart(String jid) {
        var at = jid.indexOf('@');
        return at >= 0 ? jid.substring(0, at) : jid;
    }

    /**
     * Returns whether a disappearing-messages change turned the timer on,
     * reading the new duration from the first stub parameter and falling
     * back to the message's own ephemeral duration.
     *
     * @param info the disappearing-messages system message
     * @return {@code true} if the timer was turned on
     */
    private static boolean ephemeralTurnedOn(ChatMessageInfo info) {
        var first = info.messageStubParameters().stream().findFirst().orElse(null);
        if (first == null) {
            return info.ephemeralDuration().orElse(0) != 0;
        }
        try {
            return Long.parseLong(first.trim()) != 0;
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    /**
     * Prepends the forwarding marker to a body when the message is
     * forwarded.
     *
     * @param info the message being rendered
     * @param body the already-rendered body
     * @return the body, possibly prefixed with the forwarding marker
     */
    private static String applyForwardPrefix(ChatMessageInfo info, String body) {
        var content = info.message().content();
        if (!isForwarded(content)) {
            return body;
        }
        return (isFrequentlyForwarded(content) ? "[Forwarded many times] " : "[Forwarded] ") + body;
    }

    /**
     * Returns the display name of the message sender.
     *
     * @param info the message
     * @return "You" for outgoing messages, otherwise the sender's push name
     *         or "Unknown"
     */
    private static String senderName(ChatMessageInfo info) {
        if (info.key().fromMe()) {
            return "You";
        }
        return info.pushName().filter(name -> !name.isBlank()).orElse("Unknown");
    }

    /**
     * Formats the plain-text timestamp of a message as
     * "M/D/YY, h:MM:SS AM/PM" in the system default time zone.
     *
     * @param info the message
     * @return the formatted timestamp
     */
    private static String plainTimestamp(ChatMessageInfo info) {
        var dateTime = messageInstant(info).atZone(ZoneId.systemDefault());
        var hour24 = dateTime.getHour();
        var hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
        var meridiem = hour24 >= 12 ? "PM" : "AM";
        return dateTime.getMonthValue()
                + "/" + dateTime.getDayOfMonth()
                + "/" + String.format(Locale.US, "%02d", dateTime.getYear() % 100)
                + ", " + hour12
                + ":" + String.format(Locale.US, "%02d", dateTime.getMinute())
                + ":" + String.format(Locale.US, "%02d", dateTime.getSecond())
                + " " + meridiem;
    }

    /**
     * Returns the deterministic archive file name for a media attachment,
     * without the {@code media/} prefix.
     *
     * <p>The name is derived solely from the message, so the download step
     * and the Markdown link builder agree without sharing state. Documents
     * reuse their original sanitized file name (prefixed with the message
     * id to guarantee uniqueness); every other media type uses the message
     * id and an extension inferred from its type or MIME type.
     *
     * @param info    the media message
     * @param content the unwrapped media content
     * @return the archive entry name
     */
    private static String mediaFileName(ChatMessageInfo info, Message content) {
        var id = info.key().id().filter(value -> !value.isBlank()).orElse("media");
        if (content instanceof DocumentMessage document) {
            var fileName = document.fileName().filter(value -> !value.isBlank()).orElse(null);
            if (fileName != null) {
                return id + "-" + sanitize(fileName);
            }
        }
        return id + mediaExtension(content);
    }

    /**
     * Replaces characters that are illegal in archive entry names with
     * underscores.
     *
     * @param name the raw name
     * @return the sanitized name
     */
    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");
    }

    /**
     * Infers a file extension (including the leading dot) for a media
     * attachment, from its MIME type when available and otherwise from its
     * concrete type.
     *
     * @param content the unwrapped media content
     * @return the extension, including the leading dot
     */
    private static String mediaExtension(Message content) {
        if (content instanceof VideoMessage video && video.gifPlayback()) {
            return ".gif";
        }
        if (content instanceof StickerMessage) {
            return ".webp";
        }
        if (content instanceof MediaMessage media) {
            var fromMime = media.mimetype().map(LiveChatExporterService::extensionFromMimeType).orElse(null);
            if (fromMime != null) {
                return fromMime;
            }
        }
        return switch (content) {
            case ImageMessage ignored -> ".jpg";
            case VideoMessage ignored -> ".mp4";
            case AudioMessage audio -> audio.ptt() ? ".opus" : ".mp3";
            default -> ".bin";
        };
    }

    /**
     * Derives a file extension from a MIME type, normalizing a few common
     * subtypes.
     *
     * @param mimeType the MIME type, possibly with parameters
     * @return the extension including the leading dot, or {@code null} if it
     *         cannot be derived
     */
    private static String extensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        var semicolon = mimeType.indexOf(';');
        var base = (semicolon >= 0 ? mimeType.substring(0, semicolon) : mimeType).trim();
        var slash = base.indexOf('/');
        if (slash < 0 || slash == base.length() - 1) {
            return null;
        }
        var subtype = base.substring(slash + 1).trim();
        return switch (subtype) {
            case "jpeg" -> ".jpg";
            case "mpeg" -> ".mp3";
            case "quicktime" -> ".mov";
            case "3gpp" -> ".3gp";
            case "plain" -> ".txt";
            default -> {
                var cleaned = subtype.replaceAll("[^A-Za-z0-9]", "");
                yield cleaned.isEmpty() ? null : "." + cleaned;
            }
        };
    }

    /**
     * Returns the lowercase plain-text type label for a media message.
     *
     * @param content the unwrapped media content
     * @return the type label
     */
    private static String typeName(Message content) {
        return switch (content) {
            case ImageMessage ignored -> "image";
            case VideoMessage ignored -> "video";
            case AudioMessage audio -> audio.ptt() ? "voice message" : "audio";
            case DocumentMessage ignored -> "document";
            case StickerMessage ignored -> "sticker";
            default -> "media";
        };
    }

    /**
     * Returns the capitalized Markdown type label for a media message.
     *
     * @param content the unwrapped media content
     * @return the type label
     */
    private static String markdownTypeName(Message content) {
        return switch (content) {
            case VideoMessage video when video.gifPlayback() -> "GIF";
            case ImageMessage ignored -> "Image";
            case VideoMessage ignored -> "Video";
            case AudioMessage audio -> audio.ptt() ? "Voice message" : "Audio";
            case DocumentMessage ignored -> "Document";
            case StickerMessage ignored -> "Sticker";
            default -> "Media";
        };
    }

    /**
     * Returns the caption attached to a media message, when its type carries
     * one.
     *
     * @param content the unwrapped media content
     * @return the caption, or empty if the type carries none
     */
    private static Optional<String> mediaCaption(Message content) {
        return switch (content) {
            case ImageMessage image -> image.caption();
            case VideoMessage video -> video.caption();
            case DocumentMessage document -> document.caption();
            default -> Optional.empty();
        };
    }

    /**
     * Returns whether the message was deleted for everyone.
     *
     * @param info the message
     * @return {@code true} if the message was revoked
     */
    private static boolean isRevoked(ChatMessageInfo info) {
        return info.revokeMessageTimestamp().isPresent()
                || info.messageStubType().map(type -> type == StubType.REVOKE).orElse(false);
    }

    /**
     * Returns whether the message is a system (stub) message other than a
     * revocation.
     *
     * @param info the message
     * @return {@code true} if the message is a non-revoke system message
     */
    private static boolean isSystem(ChatMessageInfo info) {
        return info.messageStubType().map(type -> type != StubType.REVOKE).orElse(false);
    }

    /**
     * Returns whether the message content is one of the types excluded from
     * the transcript: protocol, reaction, encrypted reaction, poll update,
     * keep-in-chat or pin-in-chat.
     *
     * @param info the message
     * @return {@code true} if the message must be excluded
     */
    private static boolean isExcludedType(ChatMessageInfo info) {
        var content = info.message().content();
        return content instanceof ProtocolMessage
                || content instanceof ReactionMessage
                || content instanceof EncReactionMessage
                || content instanceof PollUpdateMessage
                || content instanceof KeepInChatMessage
                || content instanceof PinInChatMessage;
    }

    /**
     * Returns whether the message is a view-once message.
     *
     * @param info the message
     * @return {@code true} if the message is view-once
     */
    private static boolean isViewOnce(ChatMessageInfo info) {
        return info.message().futureProofContentType() == FutureProofMessageType.VIEW_ONCE;
    }

    /**
     * Returns whether the message carries a non-zero ephemeral
     * (disappearing) timer.
     *
     * @param info the message
     * @return {@code true} if the message is ephemeral
     */
    private static boolean isEphemeral(ChatMessageInfo info) {
        return info.ephemeralDuration().isPresent() && info.ephemeralDuration().getAsInt() != 0;
    }

    /**
     * Returns whether the message's media was requested and successfully
     * bundled into the archive.
     *
     * @param info          the message
     * @param includeMedia  whether media was requested for this export
     * @param downloadedIds the message-key ids whose media was bundled
     * @return {@code true} if media was included and the message's media is
     *         present in the archive
     */
    private static boolean isDownloaded(ChatMessageInfo info, boolean includeMedia, Set<String> downloadedIds) {
        return includeMedia && info.key().id().map(downloadedIds::contains).orElse(false);
    }

    /**
     * Returns the epoch-second timestamp of a message, treating a missing
     * timestamp as zero.
     *
     * @param info the message
     * @return the timestamp in epoch seconds
     */
    private static long epochSecond(ChatMessageInfo info) {
        return info.timestamp().map(Instant::getEpochSecond).orElse(0L);
    }

    /**
     * Returns the instant of a message, treating a missing timestamp as the
     * epoch.
     *
     * @param info the message
     * @return the message instant
     */
    private static Instant messageInstant(ChatMessageInfo info) {
        return info.timestamp().orElse(Instant.EPOCH);
    }

    /**
     * Returns the context info attached to a message content, if it is
     * contextual.
     *
     * @param content the message content
     * @return the context info, or empty if absent or non-contextual
     */
    private static Optional<ContextInfo> contextInfo(Message content) {
        return content instanceof ContextualMessage contextual ? contextual.contextInfo() : Optional.empty();
    }

    /**
     * Returns whether the message content is flagged as forwarded.
     *
     * @param content the message content
     * @return {@code true} if forwarded
     */
    private static boolean isForwarded(Message content) {
        return contextInfo(content).map(ContextInfo::isForwarded).orElse(false);
    }

    /**
     * Returns whether the message content has a forwarding score at or above
     * the frequently-forwarded threshold.
     *
     * @param content the message content
     * @return {@code true} if frequently forwarded
     */
    private static boolean isFrequentlyForwarded(Message content) {
        return contextInfo(content)
                .map(info -> info.forwardingScore().orElse(0) >= FREQUENTLY_FORWARDED_SCORE)
                .orElse(false);
    }

    /**
     * Counts ZIP bytes while forwarding writes to the caller-provided stream.
     */
    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream delegate;
        private long bytesWritten;

        private CountingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
            bytesWritten++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            delegate.write(bytes, offset, length);
            bytesWritten += length;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            flush();
        }

        private long bytesWritten() {
            return bytesWritten;
        }
    }
}
