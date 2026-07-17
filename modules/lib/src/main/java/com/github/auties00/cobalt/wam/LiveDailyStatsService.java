package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.privacy.DefenseModePrivacyValue;
import com.github.auties00.cobalt.wire.linked.privacy.PrivacySettingType;
import com.github.auties00.cobalt.wire.linked.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.wire.linked.privacy.ReadReceiptsPrivacyValue;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.wire.wam.event.DailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TestAnonymousDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TestAnonymousIdLessEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TestAnonymousWeeklyIdEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebDynamicSamplingTestEventWithSamplingEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebDynamicSamplingTestEventWithoutSamplingEventBuilder;
import com.github.auties00.cobalt.wam.synthetic.SyntheticTelemetryUtils;
import com.github.auties00.cobalt.wire.wam.type.NotificationSettingType;
import com.github.auties00.cobalt.wire.wam.type.PrivacySettingsContactsBuckets;
import com.github.auties00.cobalt.wire.wam.type.PrivacySettingsValueType;
import com.github.auties00.cobalt.wire.wam.type.UsernameState;

import java.io.File;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Emits the once-per-day WhatsApp Metrics {@code Daily} event (id 1158) and
 * its accompanying private-stats canary events on a recurring cadence.
 *
 * <p>WhatsApp Web logs a {@code Daily} snapshot of long-lived account state
 * (locale, privacy audiences, contact counts, storage shape, decoder
 * capabilities) at most once every rolling twenty-four hours, immediately
 * followed by a fixed block of zero-field private-stats test events that
 * exercise the deidentified-telemetry rotation machinery. Cobalt mirrors that
 * surface so its telemetry stream carries the same daily heartbeat a genuine
 * Web session would: a Cobalt session that never emitted {@code Daily} would
 * be trivially distinguishable from a real client.
 *
 * <p>The service owns a single dedicated virtual thread, armed by
 * {@link #start()} and torn down by {@link #stop()}. The thread wakes hourly,
 * compares the wall clock against the last successful run, and triggers
 * {@link #runDailyStats()} only once a full day has elapsed (or on the very
 * first wake of a fresh session). Each run reads live account and settings
 * state from the bound {@link LinkedWhatsAppClient} store, populates the small
 * subset of {@code Daily} fields Cobalt can faithfully source (leaving every
 * field it lacks unset, exactly as Web does), and commits the event plus the
 * canary block through {@link WamService#commit}.
 *
 * @implNote
 * This implementation persists the last-run timestamp in the WAM sub-store
 * ({@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppWamStore#lastDailyStatsTimestamp()}),
 * so the rolling-day gate survives process restarts and the {@code Daily} event is emitted at most
 * once per rolling twenty-four hours across sessions.
 *
 * @see WamService
 * @see com.github.auties00.cobalt.wire.wam.event.DailyEvent
 */
@WhatsAppWebModule(moduleName = "WAWebDailyWamEvent")
public final class LiveDailyStatsService implements DailyStatsService {
    /**
     * The logger for {@link LiveDailyStatsService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveDailyStatsService.class);

    /**
     * The minimum wall-clock gap, in milliseconds, between two successive
     * {@code Daily} emissions.
     *
     * <p>Set to a full twenty-four hours to match WhatsApp Web's rolling-day
     * cadence; {@link #runDailyStats()} fires only once this interval has
     * elapsed since the previous successful run.
     */
    private static final long DAILY_INTERVAL_MILLIS = 24L * 60 * 60 * 1000;

    /**
     * The fixed delay, in milliseconds, between successive wakes of the worker
     * thread.
     *
     * <p>The thread re-checks the rolling-day gate once per hour so that the
     * first emission after a day boundary lands within an hour of the boundary
     * rather than waiting a further full day.
     */
    private static final long RECHECK_INTERVAL_MILLIS = 3_600_000L;

    /**
     * The fixed comma-separated encoder capability list reported in the
     * {@code supportedEncoders} field, captured verbatim from a real WhatsApp Web
     * session: empty, as the browser advertises no hardware video encoders for the
     * probed record configurations.
     */
    private static final String SUPPORTED_ENCODERS = "";

    /**
     * The fallback browser-storage quota, in bytes, reported when the host's total disk size is
     * unavailable; captured from a real WhatsApp Web session.
     */
    private static final long FALLBACK_STORAGE_QUOTA = 323172190617L;

    /**
     * The non-message browser-storage footprint, in bytes, observed in a captured WhatsApp Web
     * session: the fixed baseline that does not grow with the number of persisted messages
     * (service-worker asset caches, Signal key material, and the contact, settings, and metadata
     * IndexedDB tables).
     *
     * <p>The reported used-bytes figure adds the per-message contribution
     * ({@link #STORAGE_BYTES_PER_MESSAGE} times the real message count), the reclaimable cache, and a
     * per-session jitter offset on top of this baseline, so it tracks the account's actual store
     * size rather than freezing at an identical constant every session.
     */
    private static final long STORAGE_BASELINE_BYTES = 84_096_212L;

    /**
     * The estimated on-disk cost, in bytes, of a single persisted message (its protobuf record plus
     * the MVStore key and value overhead), used to scale the reported used-bytes figure with the
     * real size of the message store.
     */
    private static final long STORAGE_BYTES_PER_MESSAGE = 2_048L;

    /**
     * The inclusive width, in bytes, of the per-session jitter offset added to the reported
     * used-bytes figure so that successive sessions do not report a byte-identical value that would
     * itself fingerprint the client.
     */
    private static final long STORAGE_USAGE_JITTER_BYTES = 8_388_608L;

    /**
     * The reclaimable service-worker cache baseline, in bytes, contained within the reported
     * used-bytes figure and added back to report the with-cache available size; captured from a real
     * WhatsApp Web session and raised per session by at most {@link #STORAGE_CACHE_JITTER_BYTES}.
     */
    private static final long STORAGE_CACHE_BASE_BYTES = 57_413_632L;

    /**
     * The inclusive width, in bytes, of the per-session jitter offset added to the reclaimable
     * service-worker cache figure so that successive sessions do not report a byte-identical cache
     * size.
     */
    private static final long STORAGE_CACHE_JITTER_BYTES = 2_097_152L;

    /**
     * The default language subtag reported when the bound account carries no
     * locale.
     */
    private static final String DEFAULT_LANGUAGE_CODE = "en";

    /**
     * The default region subtag reported when the bound account's locale
     * carries no region.
     */
    private static final String DEFAULT_LOCATION_CODE = "US";

    /**
     * The bound WhatsApp client whose store supplies the live account, settings,
     * and contact state sampled on every run.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which the {@code Daily} event and the canary block
     * are committed for batched upload.
     */
    private final WamService wamService;

    /**
     * The guard ensuring {@link #start()} arms at most one worker thread; flipped
     * from {@code false} to {@code true} by the first {@link #start()} and reset
     * by {@link #stop()}.
     */
    private final AtomicBoolean started;

    /**
     * The stop flag observed by the worker loop; set by {@link #stop()} so the
     * loop exits at its next iteration even if it is not currently interrupted.
     */
    private volatile boolean stopped;

    /**
     * The worker thread armed by {@link #start()}, retained so {@link #stop()}
     * can interrupt the in-progress sleep.
     */
    private volatile Thread worker;

    /**
     * Constructs a new {@code LiveDailyStatsService} bound to the given client and
     * WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public LiveDailyStatsService(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.started = new AtomicBoolean(false);
    }

    /**
     * Arms the recurring daily-stats task on a dedicated virtual thread.
     *
     * <p>This is idempotent: a second call while the task is already running is a
     * no-op. The launched thread loops until {@link #stop()} flips the stop flag
     * or interrupts it, waking once per hour to evaluate the rolling-day gate and
     * running {@link #runDailyStats()} whenever a full day has elapsed since the
     * previous successful run (or immediately on the first wake of a session that
     * has never run).
     */
    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "daily stats worker already started, ignoring start");
            return;
        }
        stopped = false;
        worker = Thread.ofVirtual()
                .name("daily-stats")
                .start(this::run);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "daily stats worker started");
    }

    /**
     * Stops the recurring daily-stats task.
     *
     * <p>This sets the stop flag, clears the started guard so a subsequent
     * {@link #start()} can re-arm a fresh worker, and interrupts the worker so it
     * abandons any in-progress hourly sleep and returns promptly.
     */
    @Override
    public void stop() {
        stopped = true;
        started.set(false);
        var thread = worker;
        if (thread != null) {
            thread.interrupt();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "daily stats worker stopped");
    }

    /**
     * Runs the worker loop on the dedicated virtual thread.
     *
     * <p>Each iteration evaluates the rolling-day gate, emits the daily stats when
     * the gate opens, records the run timestamp, and then sleeps for
     * {@link #RECHECK_INTERVAL_MILLIS} before re-checking. The loop exits cleanly
     * when {@link #stop()} sets the stop flag, and on interruption it restores the
     * thread's interrupt status and returns.
     */
    private void run() {
        while (!stopped) {
            try {
                var now = Instant.now();
                var wamStore = client.store().wamStore();
                var last = wamStore.lastDailyStatsTimestamp();
                if (last.isEmpty() || Duration.between(last.get(), now).toMillis() >= DAILY_INTERVAL_MILLIS) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "daily stats rolling-day gate open, last run {0}", last.orElse(null));
                    runDailyStats();
                    wamStore.setLastDailyStatsTimestamp(now);
                }
                Thread.sleep(RECHECK_INTERVAL_MILLIS);
            } catch (InterruptedException _) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "daily stats worker loop interrupted");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Builds and commits one {@code Daily} event followed by the private-stats
     * canary block.
     *
     * <p>The {@code Daily} event carries only the subset of fields Cobalt can
     * faithfully source: real values computed from live store state (locale-derived
     * language and region, read-receipt and audience privacy settings, defense
     * mode, default disappearing duration, contact counts, account presence,
     * username state); device-sourced identity read from the persisted device
     * descriptor and its browser fingerprint ({@code osBuildNumber} and the
     * {@code supportedDecoders} list); a message-store-scaled storage shape; plus a
     * handful of constants captured from a real WhatsApp Web session (SIM
     * identifiers, encoder list, notification permission). Every other {@code Daily}
     * field is left unset, mirroring how Web omits fields it cannot supply. The
     * canary block is the five zero-field events Web logs alongside {@code Daily} to
     * drive deidentified-telemetry rotation.
     *
     * @implNote
     * This implementation reports {@code storageTotalSize}, {@code storageAvailSize}, and
     * {@code storageAvailSizeWithCache} as int64 byte counts (the WAM {@code INTEGER} wire type is
     * variable-width up to sixty-four bits). The quota is derived from the host disk via
     * {@link #browserStorageQuota()} so it stays plausible and host-specific rather than a fixed
     * constant. The used-bytes figure is not frozen either: {@link #storageUsageBytes(long, long)}
     * scales it with the real message count summed across every chat and adds a per-session jitter
     * offset, and {@code supportedDecoders} co-varies with the same device fingerprint that supplies
     * the GPU make so a reported Intel GPU never contradicts an advertised {@code av1} decoder.
     */
    void runDailyStats() {
        var store = client.store();
        var account = store.accountStore();
        var settings = store.settingsStore();
        var device = account.device();
        var contactCount = store.contactStore().contacts().size();

        var locale = account.locale();
        var languageCode = locale
                .map(LiveDailyStatsService::languageSubtag)
                .orElse(DEFAULT_LANGUAGE_CODE);
        var locationCode = locale
                .flatMap(LiveDailyStatsService::regionSubtag)
                .orElse(DEFAULT_LOCATION_CODE);

        var disappearingSeconds = settings.disappearingMode()
                .map(mode -> (int) mode.duration().toSeconds())
                .orElse(0);

        var supportedDecoders = device instanceof LinkedWhatsAppClientDevice.Web web
                ? web.supportedDecoders()
                : LinkedWhatsAppClientDevice.Web.random(device.platform()).supportedDecoders();

        var messageCount = store.chatStore().chats().stream()
                .mapToLong(Chat::messageCount)
                .sum();
        var storageQuota = browserStorageQuota();
        var storageCache = storageCacheBytes();
        var storageUsage = storageUsageBytes(messageCount, storageCache);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "computing daily stats: contacts={0} messages={1}", contactCount, messageCount);
        }

        var builder = new DailyEventBuilder()
                .languageCode(languageCode)
                .locationCode(locationCode)
                .osBuildNumber(device.osBuildNumber())
                .simMcc(0)
                .simMnc(0)
                .supportedEncoders(SUPPORTED_ENCODERS)
                .supportedDecoders(supportedDecoders)
                .osNotificationSetting(NotificationSettingType.UNKNOWN)
                .mediaFolderFileCount(0)
                .storageTotalSize(roundHundred(storageQuota))
                .storageAvailSize(roundHundred(storageQuota - storageUsage))
                .storageAvailSizeWithCache(roundHundred(storageQuota - storageUsage + storageCache))
                .addressbookWhatsappSize(contactCount)
                .webcContactsTableSize(contactCount)
                .webcFilteredContactsSize(contactCount)
                .isCanonicalEntPresent(account.jid().isPresent())
                .defenseMode(defenseMode(settings))
                .defaultDisappearingDuration(disappearingSeconds)
                .isDefaultDisappearingMessagingUser(disappearingSeconds != 0)
                .hasUsername(account.username().isPresent());

        settings.findPrivacySetting(PrivacySettingType.READ_RECEIPTS)
                .ifPresent(value -> builder.receiptsEnabled(!(value instanceof ReadReceiptsPrivacyValue.Nobody)));

        account.usernameState()
                .map(LiveDailyStatsService::mapUsernameState)
                .ifPresent(builder::usernameState);
        account.usernameHasRecoveryPin()
                .ifPresent(builder::hasUsernamePin);

        applyPrivacy(settings, PrivacySettingType.ABOUT,
                builder::privacySettingsAbout, builder::privacySettingsAboutExceptNum);
        applyPrivacy(settings, PrivacySettingType.GROUP_ADD,
                builder::privacySettingsGroups, builder::privacySettingsGroupsExceptNum);
        applyPrivacy(settings, PrivacySettingType.LAST_SEEN,
                builder::privacySettingsLastSeen, builder::privacySettingsLastSeenExceptNum);
        applyPrivacy(settings, PrivacySettingType.PROFILE_PICTURE,
                builder::privacySettingsProfilePhoto, builder::privacySettingsProfilePhotoExceptNum);

        wamService.commit(builder.build());

        wamService.commit(new TestAnonymousDailyEventBuilder().build());
        wamService.commit(new TestAnonymousWeeklyIdEventBuilder().build());
        wamService.commit(new TestAnonymousIdLessEventBuilder().build());
        wamService.commit(new WebDynamicSamplingTestEventWithSamplingEventBuilder().build());
        wamService.commit(new WebDynamicSamplingTestEventWithoutSamplingEventBuilder().build());

        if (Log.INFO) LOGGER.log(Level.INFO, "daily stats event committed");
    }

    /**
     * Reads a single privacy setting and applies its mapped audience and, when
     * applicable, its contacts-except bucket to the {@code Daily} builder.
     *
     * <p>When the setting is absent from the store both setters are skipped, so the
     * corresponding {@code Daily} fields stay unset. The bucket setter is invoked
     * only for the contacts-except audience, whose excluded-contact count is mapped
     * through {@link #bucketExcept(int)}.
     *
     * @param settings     the settings sub-store to read from
     * @param type         the privacy setting to resolve
     * @param valueSetter  the builder setter receiving the mapped audience
     * @param bucketSetter the builder setter receiving the contacts-except bucket
     * @param <V>          the {@link PrivacySettingValue} sub-interface accepted by
     *                     {@code type}
     */
    private static <V extends PrivacySettingValue> void applyPrivacy(
            LinkedWhatsAppSettingsStore settings,
            PrivacySettingType<V> type,
            Consumer<PrivacySettingsValueType> valueSetter,
            Consumer<PrivacySettingsContactsBuckets> bucketSetter) {
        settings.findPrivacySetting(type).ifPresent(value -> {
            var mapped = mapPrivacyValue(value);
            if (mapped != null) {
                valueSetter.accept(mapped);
            }
            if ("contact_blacklist".equals(value.token())) {
                bucketSetter.accept(bucketExcept(value.excluded().size()));
            }
        });
    }

    /**
     * Maps a privacy audience value to its {@code Daily} wire enum.
     *
     * <p>The mapping keys on the value's server token rather than its concrete
     * record type so it applies uniformly across every visibility setting:
     * {@code all} to {@link PrivacySettingsValueType#EVERYONE}, {@code contacts} to
     * {@link PrivacySettingsValueType#MY_CONTACTS}, {@code contact_blacklist} to
     * {@link PrivacySettingsValueType#MY_CONTACTS_EXCEPT}, and {@code none} to
     * {@link PrivacySettingsValueType#NOBODY}.
     *
     * @param value the privacy value to map
     * @return the matching {@link PrivacySettingsValueType}, or {@code null} when
     *         the value's token has no {@code Daily} counterpart
     */
    private static PrivacySettingsValueType mapPrivacyValue(PrivacySettingValue value) {
        return switch (value.token()) {
            case "all" -> PrivacySettingsValueType.EVERYONE;
            case "contacts" -> PrivacySettingsValueType.MY_CONTACTS;
            case "contact_blacklist" -> PrivacySettingsValueType.MY_CONTACTS_EXCEPT;
            case "none" -> PrivacySettingsValueType.NOBODY;
            default -> null;
        };
    }

    /**
     * Resolves the {@code defenseMode} integer reported in the {@code Daily} event.
     *
     * <p>The value is {@code -1} when the setting is absent from the store,
     * {@code 0} when defense mode is off, and {@code 1} when it is enabled at the
     * standard tier.
     *
     * @param settings the settings sub-store to read from
     * @return {@code -1}, {@code 0}, or {@code 1} per the defense-mode state
     */
    private static int defenseMode(LinkedWhatsAppSettingsStore settings) {
        return settings.findPrivacySetting(PrivacySettingType.DEFENSE_MODE)
                .map(value -> value instanceof DefenseModePrivacyValue.OnStandard ? 1 : 0)
                .orElse(-1);
    }

    /**
     * Maps the stored username registration state to its {@code Daily} wire enum.
     *
     * <p>{@link com.github.auties00.cobalt.wire.linked.contact.UsernameState#RESERVED} maps to
     * {@link UsernameState#RESERVED} and
     * {@link com.github.auties00.cobalt.wire.linked.contact.UsernameState#ACTIVE} to
     * {@link UsernameState#ACTIVATED}.
     *
     * @param state the stored username registration state
     * @return the matching {@link UsernameState}
     */
    private static UsernameState mapUsernameState(com.github.auties00.cobalt.wire.linked.contact.UsernameState state) {
        return switch (state) {
            case RESERVED -> UsernameState.RESERVED;
            case ACTIVE -> UsernameState.ACTIVATED;
        };
    }

    /**
     * Estimates the browser-storage quota a real WhatsApp Web session would advertise.
     *
     * <p>Chrome grants an origin roughly sixty percent of the host's total disk as its storage quota,
     * so this returns sixty percent of the home volume's total space, falling back to
     * {@link #FALLBACK_STORAGE_QUOTA} when the size cannot be read. Deriving the value from the actual
     * host disk keeps the reported quota plausible and host-specific rather than a fixed constant that
     * would fingerprint every Cobalt session identically.
     *
     * @return the estimated browser-storage quota in bytes
     */
    private static long browserStorageQuota() {
        var total = new File(System.getProperty("user.home", ".")).getTotalSpace();
        return total > 0 ? total / 10 * 6 : FALLBACK_STORAGE_QUOTA;
    }

    /**
     * Computes the reclaimable service-worker cache figure reported for this run.
     *
     * <p>The value is {@link #STORAGE_CACHE_BASE_BYTES} raised by a per-session jitter offset of at
     * most {@link #STORAGE_CACHE_JITTER_BYTES}, so successive sessions never report a byte-identical
     * reclaimable-cache size. The figure is folded into {@link #storageUsageBytes(long, long)} so
     * that {@code storageAvailSizeWithCache} adds back exactly the cache already counted as in use.
     *
     * @return the reclaimable service-worker cache in bytes
     */
    private static long storageCacheBytes() {
        return SyntheticTelemetryUtils.jitter(STORAGE_CACHE_BASE_BYTES, STORAGE_CACHE_JITTER_BYTES);
    }

    /**
     * Computes the browser-storage used-bytes figure reported for this run.
     *
     * <p>The figure is the fixed {@link #STORAGE_BASELINE_BYTES} baseline plus
     * {@link #STORAGE_BYTES_PER_MESSAGE} for every persisted message plus the reclaimable
     * {@code cacheBytes}, the whole then raised by a per-session jitter offset of at most
     * {@link #STORAGE_USAGE_JITTER_BYTES}. Scaling with the real message count keeps the reported
     * usage tracking the account's actual store size rather than freezing it at a constant that
     * would fingerprint every Cobalt session identically, and the jitter keeps successive sessions
     * from reporting a byte-identical value.
     *
     * @param messageCount the total number of persisted messages summed across every chat
     * @param cacheBytes   the reclaimable service-worker cache already computed for this run by
     *                     {@link #storageCacheBytes()}, folded in so it can be added back
     *                     consistently
     * @return the browser-storage used-bytes figure in bytes
     */
    private static long storageUsageBytes(long messageCount, long cacheBytes) {
        var base = STORAGE_BASELINE_BYTES + messageCount * STORAGE_BYTES_PER_MESSAGE + cacheBytes;
        return SyntheticTelemetryUtils.jitter(base, STORAGE_USAGE_JITTER_BYTES);
    }

    /**
     * Rounds a byte count to the nearest hundred, mirroring WhatsApp Web's coarsening of storage
     * figures before they are logged.
     *
     * @param value the byte count to round
     * @return the value rounded to the nearest hundred
     */
    private static long roundHundred(long value) {
        return (value + 50) / 100 * 100;
    }

    /**
     * Buckets a contacts-except excluded-contact count into its {@code Daily} wire
     * bucket.
     *
     * <p>The ladder mirrors WhatsApp Web's coarsening of the exact count into a
     * privacy-preserving bucket: {@code 0} to {@link PrivacySettingsContactsBuckets#B0},
     * then successive open-ended ranges up to {@link PrivacySettingsContactsBuckets#B100}
     * for one hundred or more.
     *
     * @param size the number of excluded contacts
     * @return the matching {@link PrivacySettingsContactsBuckets}
     */
    private static PrivacySettingsContactsBuckets bucketExcept(int size) {
        if (size == 0) {
            return PrivacySettingsContactsBuckets.B0;
        }
        if (size < 5) {
            return PrivacySettingsContactsBuckets.B1;
        }
        if (size < 10) {
            return PrivacySettingsContactsBuckets.B5;
        }
        if (size < 15) {
            return PrivacySettingsContactsBuckets.B10;
        }
        if (size < 20) {
            return PrivacySettingsContactsBuckets.B15;
        }
        if (size < 30) {
            return PrivacySettingsContactsBuckets.B20;
        }
        if (size < 40) {
            return PrivacySettingsContactsBuckets.B30;
        }
        if (size < 50) {
            return PrivacySettingsContactsBuckets.B40;
        }
        if (size < 60) {
            return PrivacySettingsContactsBuckets.B50;
        }
        if (size < 70) {
            return PrivacySettingsContactsBuckets.B60;
        }
        if (size < 80) {
            return PrivacySettingsContactsBuckets.B70;
        }
        if (size < 90) {
            return PrivacySettingsContactsBuckets.B80;
        }
        if (size < 100) {
            return PrivacySettingsContactsBuckets.B90;
        }
        return PrivacySettingsContactsBuckets.B100;
    }

    /**
     * Extracts the lowercase language subtag from an IETF locale tag.
     *
     * <p>For a tag such as {@code "it-IT"} this returns {@code "it"}; for a tag
     * with no region such as {@code "en"} it returns the whole tag lowercased.
     *
     * @param tag the IETF locale tag
     * @return the lowercase language subtag
     */
    private static String languageSubtag(String tag) {
        var dash = tag.indexOf('-');
        var language = dash >= 0 ? tag.substring(0, dash) : tag;
        return language.toLowerCase(Locale.ROOT);
    }

    /**
     * Extracts the uppercase region subtag from an IETF locale tag, if present.
     *
     * <p>For a tag such as {@code "it-IT"} this returns {@code "IT"}; for a tag
     * with no region such as {@code "en"} it returns an empty value so the caller
     * falls back to {@link #DEFAULT_LOCATION_CODE}.
     *
     * @param tag the IETF locale tag
     * @return the uppercase region subtag, or empty when the tag carries no region
     */
    private static Optional<String> regionSubtag(String tag) {
        var dash = tag.indexOf('-');
        if (dash < 0) {
            return Optional.empty();
        }
        return Optional.of(tag.substring(dash + 1).toUpperCase(Locale.ROOT));
    }
}
