package com.github.auties00.cobalt.wam.threadlogging;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ThreadInteractionDataAiEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ThreadInteractionDataBizEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ThreadInteractionDataCoreConsumerEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ThreadInteractionDataIntegrityEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ThreadInteractionDataNotificationEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ThreadInteractionDataVoipEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ChatMutedType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Production {@link ThreadLoggingService} that aggregates per-thread interaction counters and uploads
 * them once per day as the {@code ThreadInteractionData} WAM event family.
 *
 * <p>The producer side ({@link #recordActivity(JidProvider, ThreadLoggingActivity)}) buckets each
 * activity into the thread's current day-bucket row in the WAM sub-store and bumps the matching
 * counters under a lock. The uploader side ({@link #uploadEvents()}) runs on a dedicated virtual thread
 * armed by {@link #start()}: it wakes on a fixed cadence, removes every row whose day bucket has fully
 * elapsed, enriches each with live chat metadata, derives the per-thread {@code threadId} HMAC and the
 * Pacific-shifted date stamps, commits the six per-thread events, and advances the single global upload
 * watermark so each bucket is uploaded at most once.
 *
 * @implNote
 * This implementation only populates the {@code ThreadInteractionData} fields a headless client can
 * faithfully source: the accumulated counters plus the chat-derived metadata available in the Cobalt
 * store. UI-driven and per-media-type fields that WhatsApp Web sets from surfaces a headless client
 * never touches are left unset, mirroring how Web omits fields it cannot supply. The Integrity and Ai
 * events are id-only stubs and the Notification event carries only the ids plus the group flag, matching
 * Web's emitted shape.
 *
 * @see ThreadLoggingService
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebChatThreadLogging")
public final class LiveThreadLoggingService implements ThreadLoggingService {
    /**
     * The number of seconds in one day, used by the day-bucket arithmetic.
     */
    private static final long SECONDS_PER_DAY = 86_400L;

    /**
     * The fixed Pacific shift, in seconds, applied before taking the UTC calendar date for the
     * {@code threadDs} day stamp and {@code threadId} month stamp, matching WhatsApp Web's
     * {@code WABase64}-stamped {@code PST} offset.
     */
    private static final long PST_OFFSET_SECONDS = 8L * 3600;

    /**
     * The wall-clock gap, in milliseconds, between successive uploader wakes.
     *
     * <p>The cadence only needs to be finer than a day; the upload watermark enforces the once-per-day
     * emission, so an hourly recheck simply lands the first upload after a day boundary within the hour.
     */
    private static final long RECHECK_INTERVAL_MILLIS = 3_600_000L;

    /**
     * The formatter producing the {@code threadDs} {@code yyyy-MM-dd} day stamp.
     */
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * The formatter producing the {@code threadId} {@code yyyy/MM} month stamp.
     */
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    /**
     * The bound WhatsApp client whose store supplies the pending rows, provisioning, and chat metadata.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which the {@code ThreadInteractionData} events are committed.
     */
    private final WamService wamService;

    /**
     * The lock serializing the get-or-create-and-bump of a counter row in
     * {@link #recordActivity(JidProvider, ThreadLoggingActivity)} against the snapshot-and-remove of
     * elapsed rows in {@link #uploadEvents()}.
     */
    private final ReentrantLock lock;

    /**
     * The guard ensuring {@link #start()} arms at most one worker thread.
     */
    private final AtomicBoolean started;

    /**
     * The stop flag observed by the worker loop; set by {@link #stop()}.
     */
    private volatile boolean stopped;

    /**
     * The worker thread armed by {@link #start()}, retained so {@link #stop()} can interrupt it.
     */
    private volatile Thread worker;

    /**
     * Constructs a new {@code LiveThreadLoggingService} bound to the given client and WAM service.
     *
     * @param client     the WhatsApp client whose store is read and written, must not be {@code null}
     * @param wamService the WAM service used to commit the emitted events, must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public LiveThreadLoggingService(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.lock = new ReentrantLock();
        this.started = new AtomicBoolean(false);
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        stopped = false;
        worker = Thread.ofVirtual()
                .name("thread-logging")
                .start(this::run);
    }

    @Override
    public void stop() {
        stopped = true;
        started.set(false);
        var thread = worker;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Runs the worker loop on the dedicated virtual thread.
     *
     * <p>Each iteration drives one {@link #uploadEvents()} pass and then sleeps for
     * {@link #RECHECK_INTERVAL_MILLIS}. The loop exits cleanly when {@link #stop()} sets the stop flag,
     * and on interruption it restores the thread's interrupt status and returns.
     */
    private void run() {
        while (!stopped) {
            try {
                uploadEvents();
                Thread.sleep(RECHECK_INTERVAL_MILLIS);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void recordActivity(JidProvider chat, ThreadLoggingActivity activity) {
        Objects.requireNonNull(chat, "chat cannot be null");
        Objects.requireNonNull(activity, "activity cannot be null");

        var wamStore = client.store().wamStore();
        var offset = wamStore.chatThreadLoggingOffset();
        if (offset.isEmpty()) {
            return;
        }

        var bucketInstant = Instant.ofEpochSecond(computeStartTs(offset.getAsInt(), Instant.now().getEpochSecond()));
        var watermark = wamStore.lastUploadedThreadLoggingTs();
        if (watermark.isPresent() && !bucketInstant.isAfter(watermark.get())) {
            return;
        }

        var legacyJid = chat.toJid().toString();
        lock.lock();
        try {
            var existing = wamStore.threadLoggingPending().stream()
                    .filter(row -> row.chatJid().equals(legacyJid) && row.startTs().equals(bucketInstant))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                bump(existing, activity);
            } else {
                var row = new ThreadLoggingCountersBuilder()
                        .chatJid(legacyJid)
                        .startTs(bucketInstant)
                        .build();
                bump(row, activity);
                wamStore.addThreadLoggingCounters(row);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Bumps the counters on a row according to the activity variant.
     *
     * @implNote
     * This implementation mirrors WhatsApp Web's {@code handleMessages} dispatch: a reaction send or
     * receive bumps only its own reaction counter, an edit send bumps only the edited-messages counter,
     * and any other send or receive bumps the messages counter plus whichever forwarded, view-once,
     * reply (sends only), and commerce sub-counters apply.
     *
     * @param row      the counter row to mutate, held under {@link #lock}
     * @param activity the activity classifying the bumps
     */
    private static void bump(ThreadLoggingCounters row, ThreadLoggingActivity activity) {
        switch (activity) {
            case ThreadLoggingActivity.MessageSent sent -> {
                if (sent.reaction()) {
                    row.setReactionsSent(row.reactionsSent() + 1);
                } else if (sent.edit()) {
                    row.setEditedMessagesSent(row.editedMessagesSent() + 1);
                } else {
                    row.setMessagesSent(row.messagesSent() + 1);
                    if (sent.forwarded()) {
                        row.setForwardMessagesSent(row.forwardMessagesSent() + 1);
                    }
                    if (sent.viewOnce()) {
                        row.setViewOnceMessagesSent(row.viewOnceMessagesSent() + 1);
                    }
                    if (sent.reply()) {
                        row.setRepliesSent(row.repliesSent() + 1);
                    }
                    if (sent.commerce()) {
                        row.setCommerceMessagesSent(row.commerceMessagesSent() + 1);
                    }
                }
            }
            case ThreadLoggingActivity.MessageReceived received -> {
                if (received.reaction()) {
                    row.setReactionsReceived(row.reactionsReceived() + 1);
                } else {
                    row.setMessagesReceived(row.messagesReceived() + 1);
                    if (received.forwarded()) {
                        row.setForwardMessagesReceived(row.forwardMessagesReceived() + 1);
                    }
                    if (received.viewOnce()) {
                        row.setViewOnceMessagesReceived(row.viewOnceMessagesReceived() + 1);
                    }
                    if (received.commerce()) {
                        row.setCommerceMessagesReceived(row.commerceMessagesReceived() + 1);
                    }
                }
            }
            case ThreadLoggingActivity.MessagesRead read ->
                    row.setMessagesRead(row.messagesRead() + Math.max(0, read.count()));
            // TODO: no producer emits ViewOnceOpened yet. The counter is bumped when the local user
            //       opens an inbound view-once, which is a headless no-op in Cobalt: there is no
            //       media-viewer open action and no markPlayed API to hook (the inbound PLAYED receipt
            //       in MessageReceiptStreamHandler is the peer playing our outbound message, a different
            //       semantic). Wire this once a local view-once open codepath exists.
            case ThreadLoggingActivity.ViewOnceOpened _ ->
                    row.setViewOnceMessagesOpened(row.viewOnceMessagesOpened() + 1);
            case ThreadLoggingActivity.Call call -> {
                if (call.outgoing()) {
                    row.setCallOffersSent(row.callOffersSent() + 1);
                } else {
                    row.setCallOffersReceived(row.callOffersReceived() + 1);
                }
                row.setTotalCallDuration(row.totalCallDuration() + Math.max(0L, call.durationSeconds()));
            }
        }
    }

    @Override
    public void uploadEvents() {
        var wamStore = client.store().wamStore();
        var secret = wamStore.chatThreadLoggingSecret();
        var offset = wamStore.chatThreadLoggingOffset();
        if (secret.isEmpty() || offset.isEmpty()) {
            return;
        }

        var now = Instant.now().getEpochSecond();
        var watermark = Instant.ofEpochSecond(computeStartTs(offset.getAsInt(), now - SECONDS_PER_DAY));

        var elapsed = new ArrayList<ThreadLoggingCounters>();
        lock.lock();
        try {
            for (var row : wamStore.threadLoggingPending()) {
                if (!row.startTs().isAfter(watermark)) {
                    elapsed.add(row);
                }
            }
        } finally {
            lock.unlock();
        }

        for (var row : elapsed) {
            emitEvents(row, secret.get());
        }
        wamStore.removeThreadLoggingCounters(elapsed);

        var lastUploaded = wamStore.lastUploadedThreadLoggingTs();
        if (lastUploaded.isEmpty() || watermark.isAfter(lastUploaded.get())) {
            wamStore.setLastUploadedThreadLoggingTs(watermark);
        }
    }

    /**
     * Enriches one elapsed counter row with live chat metadata and commits its six
     * {@code ThreadInteractionData} events.
     *
     * @param row    the elapsed counter row
     * @param secret the provisioned thread-logging secret used to derive the {@code threadId}
     */
    private void emitEvents(ThreadLoggingCounters row, byte[] secret) {
        var store = client.store();
        var legacyJid = row.chatJid();
        var jid = Jid.of(legacyJid);
        var seconds = row.startTs().getEpochSecond();
        var threadDs = pstDay(seconds);
        var threadId = threadId(secret, legacyJid, pstMonth(seconds));
        var chat = store.chatStore().findChatByJid(jid);
        var contact = store.contactStore().findContactByJid(jid);
        var isGroup = jid.hasGroupOrCommunityServer();

        var core = new ThreadInteractionDataCoreConsumerEventBuilder()
                .threadDs(threadDs)
                .threadId(threadId)
                .messagesSent(row.messagesSent())
                .messagesReceived(row.messagesReceived())
                .messagesRead(row.messagesRead())
                .reactionsSent(row.reactionsSent())
                .reactionsReceived(row.reactionsReceived())
                .forwardMessagesSent(row.forwardMessagesSent())
                .forwardMessagesReceived(row.forwardMessagesReceived())
                .editedMsgsSent(row.editedMessagesSent())
                .viewOnceMessagesSent(row.viewOnceMessagesSent())
                .viewOnceMessagesReceived(row.viewOnceMessagesReceived())
                .viewOnceMessagesOpened(row.viewOnceMessagesOpened())
                .repliesSent(row.repliesSent())
                .isAGroup(isGroup)
                .isAContact(contact.isPresent())
                .isMessageYourself(isSelf(jid));
        contact.ifPresent(value -> {
            core.hasUsername(value.hasUsername());
            core.isUsernameThread(value.isAddedByUsername());
        });
        chat.ifPresent(value -> {
            core.isArchived(value.archived());
            core.isPinned(value.pinnedTimestamp().isPresent());
            value.unreadCount().ifPresent(unread -> core.messagesUnread(unread));
            value.mute().ifPresent(mute -> core.chatMuted(mute.isMuted()
                    ? ChatMutedType.MUTED_NO_NOTIFICATIONS
                    : ChatMutedType.NOT_MUTED));
            value.ephemeralExpiration().ifPresent(timer -> core.chatEphemeralityDuration(timer.periodSeconds().longValue()));
        });
        if (isGroup) {
            chat.ifPresent(value -> core.typeOfGroup(value.isDefaultSubgroup()
                    ? TypeOfGroupEnum.DEFAULT_SUBGROUP
                    : TypeOfGroupEnum.GROUP));
            groupSize(jid).ifPresent(size -> core.groupSize(size));
        }
        wamService.commit(core.build());

        wamService.commit(new ThreadInteractionDataVoipEventBuilder()
                .threadDs(threadDs)
                .threadId(threadId)
                .callOffersSent(row.callOffersSent())
                .callOffersReceived(row.callOffersReceived())
                .totalCallDuration(row.totalCallDuration())
                .build());

        wamService.commit(new ThreadInteractionDataBizEventBuilder()
                .threadDs(threadDs)
                .threadId(threadId)
                .commerceMsgsSent(row.commerceMessagesSent())
                .commerceMsgsReceived(row.commerceMessagesReceived())
                .build());

        wamService.commit(new ThreadInteractionDataIntegrityEventBuilder()
                .threadDs(threadDs)
                .threadId(threadId)
                .build());

        wamService.commit(new ThreadInteractionDataAiEventBuilder()
                .threadDs(threadDs)
                .threadId(threadId)
                .build());

        wamService.commit(new ThreadInteractionDataNotificationEventBuilder()
                .threadDs(threadDs)
                .threadId(threadId)
                .isAGroup(isGroup)
                .build());
    }

    /**
     * Returns the cached participant count for a group thread, if available.
     *
     * @param jid the group thread JID
     * @return the participant count, or empty when no group metadata is cached for the thread
     */
    private OptionalInt groupSize(Jid jid) {
        return client.store().chatStore().findChatMetadata(jid)
                .map(metadata -> metadata instanceof GroupMetadata group ? group.size() : OptionalInt.empty())
                .orElse(OptionalInt.empty());
    }

    /**
     * Returns whether the thread JID is the bound account's own JID (a note-to-self thread).
     *
     * @param jid the thread JID
     * @return {@code true} if the thread addresses the bound account itself
     */
    private boolean isSelf(Jid jid) {
        var account = client.store().accountStore();
        var target = jid.withoutData();
        return account.jid().map(own -> own.withoutData().equals(target)).orElse(false)
                || account.lid().map(own -> own.withoutData().equals(target)).orElse(false);
    }

    /**
     * Derives the per-thread {@code threadId} as the standard Base64 of
     * {@code HMAC-SHA256(secret, utf8(legacyJid + monthDs))}.
     *
     * @implNote
     * This implementation uses {@link Base64#getEncoder()} (standard alphabet) rather than the URL-safe
     * encoder, matching WhatsApp Web's {@code WABase64.encodeB64} for this field; the HMAC plumbing is
     * the same as {@code WamPrivateStatsUploader.buildCredential}.
     *
     * @param secret    the provisioned thread-logging secret
     * @param legacyJid the legacy JID string of the thread
     * @param monthDs   the Pacific-shifted {@code yyyy/MM} month stamp
     * @return the Base64-encoded HMAC
     */
    private static String threadId(byte[] secret, String legacyJid, String monthDs) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            var digest = mac.doFinal((legacyJid + monthDs).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("HmacSHA256 must be available on every JVM", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("HMAC key rejected", e);
        }
    }

    /**
     * Derives the chat-thread {@code threadId} HMAC for the current month from a client's provisioned
     * thread-logging secret, for reuse by surfaces that stamp a thread id outside the daily uploader.
     *
     * <p>This resolves the secret from the bound client's WAM sub-store, derives the
     * Pacific-shifted {@code yyyy/MM} month stamp for the current instant, and returns the standard
     * Base64 of {@code HMAC-SHA256(secret, utf8(legacyJid + monthDs))}, the same value the per-thread
     * {@code ThreadInteractionData} events carry. When no secret has been provisioned the empty string
     * is returned so callers can omit the field rather than stamp a meaningless value.
     *
     * @implNote
     * This implementation reuses the same {@link #threadId(byte[], String, String)} derivation and
     * {@link #pstMonth(long)} month stamp as the daily uploader so the CTWA conversion-signal events and
     * the {@code ThreadInteractionData} events agree on the thread id for a given chat and month.
     *
     * @param client    the WhatsApp client whose WAM sub-store holds the provisioned secret, must not
     *                  be {@code null}
     * @param legacyJid the legacy JID string of the chat thread
     * @return the Base64-encoded HMAC, or the empty string when no secret has been provisioned
     */
    @WhatsAppWebExport(moduleName = "WAWebChatThreadLogging", exports = "getChatThreadIDHMAC", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebChatThreadLoggingUtils", exports = "generateThreadIDHMAC", adaptation = WhatsAppAdaptation.ADAPTED)
    public static String chatThreadIdHmac(LinkedWhatsAppClient client, String legacyJid) {
        var secret = client.store().wamStore().chatThreadLoggingSecret();
        if (secret.isEmpty()) {
            return "";
        }
        return threadId(secret.get(), legacyJid, pstMonth(Instant.now().getEpochSecond()));
    }

    /**
     * Formats the Pacific-shifted {@code yyyy-MM-dd} day stamp for an epoch-seconds timestamp.
     *
     * @implNote
     * This implementation subtracts {@link #PST_OFFSET_SECONDS} and then takes the UTC calendar date,
     * replicating WhatsApp Web's fixed minus-eight-hours shift exactly rather than applying a real
     * time-zone with daylight-saving transitions.
     *
     * @param ts the epoch-seconds timestamp
     * @return the {@code yyyy-MM-dd} day stamp
     */
    private static String pstDay(long ts) {
        return DAY_FORMAT.format(LocalDate.ofInstant(Instant.ofEpochSecond(ts - PST_OFFSET_SECONDS), ZoneOffset.UTC));
    }

    /**
     * Formats the Pacific-shifted {@code yyyy/MM} month stamp for an epoch-seconds timestamp.
     *
     * @param ts the epoch-seconds timestamp
     * @return the {@code yyyy/MM} month stamp
     */
    private static String pstMonth(long ts) {
        return MONTH_FORMAT.format(LocalDate.ofInstant(Instant.ofEpochSecond(ts - PST_OFFSET_SECONDS), ZoneOffset.UTC));
    }

    /**
     * Computes the start of the day bucket containing an epoch-seconds timestamp, offset from midnight
     * UTC by the provisioned day-bucket offset.
     *
     * @param offset the day-bucket offset in seconds
     * @param ts     the epoch-seconds timestamp to bucket
     * @return the epoch-seconds start of the containing day bucket
     */
    private static long computeStartTs(int offset, long ts) {
        var n = Math.floorMod(ts, SECONDS_PER_DAY);
        var r = ts - n;
        return n >= offset ? r + offset : r - SECONDS_PER_DAY + offset;
    }
}
