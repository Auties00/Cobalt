package com.github.auties00.cobalt.stream.iq;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.graphql.WhatsAppWebGraphQlCanonicalNonceDecryptor;
import com.github.auties00.cobalt.graphql.WhatsAppWebGraphQlBootstrapClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.business.webgraphql.WhatsAppWebGraphQlSessionBuilder;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.pairing.CompanionPairingService;
import com.github.auties00.cobalt.pairing.ShortcakePairingService;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVDeviceIdentitySpec;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentity;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentityBuilder;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentitySpec;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPairingProps;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPairingPropsSpec;
import com.github.auties00.cobalt.wire.linked.device.pairing.LinkedPrimaryPlatform;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.stanza.smax.mdcompanion.SmaxMdSetRegEncryptionMetadata;
import com.github.auties00.cobalt.stream.NodeStreamService;
import com.github.auties00.cobalt.sync.SnapshotRecoveryService;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.util.ScheduledTask;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CanonicalEntRecoveryCriticalEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.Lid11MigrationLifecycleEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdLinkDeviceCompanionEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MdLinkDeviceCompanionStage;
import com.github.auties00.cobalt.wire.wam.type.MigrationStageEnum;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles server-initiated {@code <iq>} (info/query) stanzas, covering the
 * multi-device companion-pairing flow and the keep-alive pings.
 *
 * <p>The handler is registered under the {@code "iq"} tag inside
 * {@link NodeStreamService} and routes each inbound stanza by its {@code xmlns}
 * attribute. Two namespaces are supported: {@code urn:xmpp:ping}, which is
 * answered with an empty {@code <iq type="result">}, and {@code md}, the
 * companion-pairing exchange that lands the QR ref rotation and the follow-up
 * pair-success exchange. Response IQs for outbound requests are not routed
 * here; they flow through the per-call request/response correlator on
 * {@link LinkedWhatsAppClient}.
 *
 * @implNote
 * This implementation collapses the {@code WAWebHandlePairDevice} and
 * {@code WAWebHandlePairSuccess} entry points into a single handler keyed on
 * the {@code <iq>} child tag, and schedules the QR ref rotation as a one-shot
 * {@link ScheduledTask} that re-arms itself for the next ref rather than a
 * sliding-deadline timer primitive. The {@code mdSessionId} computation, the
 * {@link MdLinkDeviceCompanionStage} commits, and the
 * {@code Lid11MigrationLifecycle} commit on pair-success mirror the upstream
 * reporter logic so the device-link funnel reported to the WhatsApp dashboards
 * matches the official client.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleStanzaCommon")
@WhatsAppWebModule(moduleName = "WAWebHandlePairDevice")
@WhatsAppWebModule(moduleName = "WAWebHandlePairSuccess")
@WhatsAppWebModule(moduleName = "WAWebHandleCanonicalRegistration")
@WhatsAppWebModule(moduleName = "WAWebCanonicalEntRecoveryWam")
public final class IqStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * The logger for {@link IqStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(IqStreamHandler.class);

    /**
     * The display duration, in milliseconds, for the first QR ref of a fresh
     * pairing exchange.
     *
     * <p>The initial ref is shown for 60 seconds before the rotation falls back
     * to {@link #REFRESH_ROTATION_MS}.
     *
     * @implNote
     * This implementation pins the value to 60000 to match the upstream
     * {@code 6e4} constant.
     */
    private static final long QR_ROTATION_MS = 60_000L;

    /**
     * The display duration, in milliseconds, for every refresh QR ref after
     * the first.
     *
     * <p>Refresh refs cycle every 20 seconds until the user scans one or the
     * server stops pushing new refs.
     *
     * @implNote
     * This implementation pins the value to 20000 to match the upstream
     * {@code 20*1e3} constant.
     */
    private static final long REFRESH_ROTATION_MS = 20_000L;

    /**
     * The client used for store access and outbound stanza dispatch.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The verification handler that receives QR-payload strings during the QR
     * pairing branch.
     */
    private final LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler;

    /**
     * The service consulted for ADV identity validation and local-identity
     * persistence during pair-success.
     */
    private final DeviceService deviceService;

    /**
     * The service updated with the primary device's syncd-snapshot-recovery
     * support flag when pair-success resolves.
     */
    private final SnapshotRecoveryService snapshotRecoveryService;

    /**
     * The service consulted when committing the {@code Lid11MigrationLifecycle}
     * WAM event after a successful pairing.
     */
    private final LidMigrationService lidMigrationService;

    /**
     * The service that owns the alt-device-linking (phone-number pairing-code)
     * handshake state.
     *
     * <p>When {@link CompanionPairingService#isEnabled()} returns {@code true}
     * the QR ref rotation is skipped and the pairing-code flow is started
     * instead.
     */
    private final CompanionPairingService deviceLinkingService;

    /**
     * The service consulted when a passkey verification handler is configured: if its
     * {@link ShortcakePairingService#isEnabled()} returns {@code true} the QR ref rotation and the
     * pairing-code flow are skipped and the Shortcake passkey ceremony is started instead.
     */
    private final ShortcakePairingService shortcakePairingService;

    /**
     * The lock protecting {@link #rotationTask} so the rotation can be
     * cancelled atomically from both the executor thread and the handler
     * thread.
     */
    private final Object rotationLock;

    /**
     * The currently scheduled rotation tick, or {@code null} when no rotation
     * is active.
     */
    private ScheduledTask rotationTask;

    /**
     * The service used to commit the {@link MdLinkDeviceCompanionStage}
     * transitions and the {@code Lid11MigrationLifecycle} event.
     */
    private final WamService wamService;

    /**
     * The lazily-created canonical-registration trace id that ties every
     * {@code CanonicalEntRecoveryCriticalEvent} of a single pairing session
     * together.
     *
     * <p>The id is a random UUID minted on the first canonical-recovery critical
     * commit and reused for every subsequent commit on the same handler, so the
     * trace id threaded through the canonical-entity recovery funnel is stable
     * within one linking attempt. It stays {@code null} until the first failure
     * is reported.
     *
     * @implNote
     * This implementation mints the id on demand rather than eagerly at pairing
     * start because the id is only ever consumed by a critical-event commit,
     * which is the exception path; the common successful pairing never needs one.
     * WA persists this id in {@code WAWebUserPrefsCanonical}; Cobalt keeps it in
     * a per-handler field because it is only read by the telemetry commit.
     */
    private String canonicalRegistrationTraceId;

    /**
     * Constructs an IQ stream handler bound to the given services.
     *
     * <p>The dispatcher in {@link NodeStreamService} instantiates the handler once
     * per client; embedders never call this constructor directly. The rotation
     * executor is created here as a single-thread daemon scheduler so that an
     * in-flight QR rotation never keeps the JVM alive.
     *
     * @param whatsapp                the client; must not be {@code null}
     * @param webVerificationHandler  the verification handler that receives
     *                                QR/pairing payloads; must not be
     *                                {@code null}
     * @param deviceService           the service used for ADV validation; must
     *                                not be {@code null}
     * @param snapshotRecoveryService the service updated on pair-success; must
     *                                not be {@code null}
     * @param lidMigrationService     the service consulted on pair-success;
     *                                must not be {@code null}
     * @param deviceLinkingService    the service used to gate
     *                                {@code pair-device} handling; must not be
     *                                {@code null}
     * @param shortcakePairingService the service used to gate Shortcake passkey
     *                                {@code pair-device} handling; must not be
     *                                {@code null}
     * @param wamService              the service used for pairing-funnel
     *                                telemetry; must not be {@code null}
     * @throws NullPointerException if any service is {@code null}
     */
    public IqStreamHandler(
            LinkedWhatsAppClient whatsapp,
            LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler,
            DeviceService deviceService,
            SnapshotRecoveryService snapshotRecoveryService,
            LidMigrationService lidMigrationService,
            CompanionPairingService deviceLinkingService,
            ShortcakePairingService shortcakePairingService,
            WamService wamService
    ) {
        this.whatsapp = whatsapp;
        this.webVerificationHandler = Objects.requireNonNull(webVerificationHandler, "webVerificationHandler cannot be null");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService cannot be null");
        this.snapshotRecoveryService = Objects.requireNonNull(snapshotRecoveryService, "snapshotRecoveryService cannot be null");
        this.lidMigrationService = Objects.requireNonNull(lidMigrationService, "lidMigrationService cannot be null");
        this.deviceLinkingService = Objects.requireNonNull(deviceLinkingService, "altDeviceLinkingService cannot be null");
        this.shortcakePairingService = Objects.requireNonNull(shortcakePairingService, "shortcakePairingService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.rotationLock = new Object();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Routes the incoming {@code <iq>} by its {@code xmlns} attribute:
     * {@code urn:xmpp:ping} replies with an empty {@code <iq type="result">},
     * {@code md} dispatches to the {@code pair-device} or {@code pair-success}
     * branch based on the first child tag, and anything else is logged at
     * {@code DEBUG} and dropped.
     *
     * @param stanza {@inheritDoc}
     */
    @Override
    public void handle(Stanza stanza) {
        var xmlns = stanza.getAttributeAsString("xmlns", null);
        if ("urn:xmpp:ping".equals(xmlns)) {
            handlePing(stanza);
            return;
        }

        if (!"md".equals(xmlns)) {
            return;
        }

        var child = stanza.getChild().orElse(null);
        if (child == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring md iq without child: {0}", stanza);
            return;
        }

        switch (child.description()) {
            case "pair-device" -> handlePairDevice(stanza);
            case "pair-success" -> handlePairSuccess(stanza);
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring unsupported md iq child {0}", child.description());
            }
        }
    }

    /**
     * Responds to a server-initiated {@code <iq xmlns="urn:xmpp:ping">}
     * keep-alive with an empty result IQ correlated by the inbound {@code id}.
     *
     * <p>The server periodically pings the client to assert liveness; failing
     * to reply within the configured deadline causes the server to drop the
     * socket. The reply is fire-and-forget, as the server does not echo an ack.
     * A ping without a {@code from} attribute is logged at {@code DEBUG} and
     * dropped.
     *
     * @param stanza the inbound ping {@code <iq>} stanza
     */
    private void handlePing(Stanza stanza) {
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring ping iq without from attribute");
            return;
        }

        var response = new StanzaBuilder()
                .description("iq")
                .attribute("type", "result")
                .attribute("to", from)
                .attribute("id", stanza.getAttributeAsString("id", null))
                .build();
        whatsapp.sendNodeWithNoResponse(response);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "replied to ping iq from {0}", from);
    }

    /**
     * Handles the {@code <iq><pair-device/></iq>} stanza that opens the QR or
     * pairing-code flow.
     *
     * <p>A fresh 32-byte ADV secret key is generated and stored, the IQ is
     * acked so the server stops retransmitting, and then one of two branches
     * runs: when the {@link CompanionPairingService} is enabled the
     * pairing-code handshake is started and the QR ref rotation is skipped,
     * because pairing-code pairing derives its own client-side code rather than
     * displaying a server ref; otherwise the inbound {@code <ref/>} elements
     * are extracted and the QR ref rotation begins. A missing
     * {@code <pair-device/>} child or an empty ref set is logged at
     * {@code WARNING} and abandoned. A failure to start the pairing-code flow
     * is logged at {@code WARNING} and swallowed.
     *
     * @param iqStanza the full {@code <iq>} stanza containing the
     *               {@code <pair-device/>} child
     */
    private void handlePairDevice(Stanza iqStanza) {
        var pairDevice = iqStanza.getChild("pair-device").orElse(null);
        if (pairDevice == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "received md iq without pair-device child");
            return;
        }

        whatsapp.store().signalStore().setAdvSecretKey(DataUtils.randomByteArray(32));
        sendPairDeviceAck(iqStanza);

        if (shortcakePairingService.isEnabled()) {
            try {
                shortcakePairingService.start();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "started shortcake passkey linking");
            } catch (Throwable throwable) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot start shortcake passkey linking", throwable);
            }
            return;
        }

        if (deviceLinkingService.isEnabled()) {
            try {
                deviceLinkingService.start();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "started alt-device-linking");
            } catch (Throwable throwable) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot start alt-device-linking", throwable);
            }
            return;
        }

        var refs = extractPairRefs(pairDevice);
        if (refs.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "received pair-device iq without any usable refs");
            return;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting qr ref rotation with {0} refs", refs.size());
        scheduleVerificationValues(refs);
    }

    /**
     * Sends the {@code <iq type="result">} ack for the inbound
     * {@code <pair-device/>} IQ, correlated by the inbound {@code id}.
     *
     * <p>The ack is an empty {@code <iq type="result">} routed to
     * {@link Jid#userServer()} carrying the inbound stanza id for correlation.
     * The ack is skipped, and the reason logged at {@code DEBUG}, when the
     * inbound stanza is missing its {@code id} or {@code from} attribute.
     *
     * @param iqStanza the original {@code <pair-device/>} IQ stanza
     */
    private void sendPairDeviceAck(Stanza iqStanza) {
        var id = iqStanza.getAttributeAsString("id", null);
        var from = iqStanza.getAttributeAsJid("from").orElse(null);
        if (id == null || from == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cannot send pair-device ack: missing id or from");
            return;
        }

        var response = new StanzaBuilder()
                .description("iq")
                .attribute("id", id)
                .attribute("to", Jid.userServer())
                .attribute("type", "result")
                .build();
        whatsapp.sendNodeWithNoResponse(response);
    }

    /**
     * Extracts the ordered set of QR ref strings from a {@code <pair-device/>}
     * stanza.
     *
     * <p>Ref values may surface either as the content of the parent or its
     * {@code <ref/>} children, or in a {@code ref}, {@code value}, or
     * {@code code} attribute on either the parent or a child. All four sources
     * are folded into an ordered set, and blank entries are removed.
     *
     * @implNote
     * This implementation uses a {@link LinkedHashSet} rather than a plain
     * list because the multi-source extraction would otherwise emit duplicates
     * when the same ref appears in both an attribute and a content blob.
     *
     * @param pairDevice the {@code <pair-device/>} child stanza
     * @return the ordered set of non-blank ref strings; never {@code null}
     */
    private LinkedHashSet<String> extractPairRefs(Stanza pairDevice) {
        var refs = new LinkedHashSet<String>();
        decodeContentAsString(pairDevice).ifPresent(refs::add);

        for (var child : pairDevice.children()) {
            decodeContentAsString(child).ifPresent(refs::add);

            findStringAttribute(child, "ref", "value", "code")
                    .ifPresent(refs::add);
        }

        findStringAttribute(pairDevice, "ref", "value", "code")
                .ifPresent(refs::add);
        refs.removeIf(String::isBlank);
        return refs;
    }

    /**
     * Cancels any in-flight rotation and starts a fresh rotation cycle over the
     * given ref queue.
     *
     * <p>This is the entry point into the QR ref rotation; the first tick fires
     * synchronously, and subsequent ticks are scheduled on the rotation
     * executor.
     *
     * @param refs the ordered set of ref strings to rotate through
     */
    private void scheduleVerificationValues(LinkedHashSet<String> refs) {
        synchronized (rotationLock) {
            cancelRotationLocked();
        }

        var queue = new ArrayDeque<>(refs);
        runRotationTick(queue);
    }

    /**
     * Runs a single rotation tick over the shared ref queue.
     *
     * <p>Each tick first checks the early-exit conditions (the device is
     * already registered, or the queue is empty), then computes the next
     * time-to-live from the queue size ({@link #QR_ROTATION_MS} while the queue
     * still holds all six initial refs, otherwise {@link #REFRESH_ROTATION_MS}),
     * pops the next ref, publishes it to the verification handler, and
     * reschedules the next tick after the computed delay.
     *
     * @implNote
     * This implementation reschedules by cancelling the previous
     * {@link ScheduledTask} and arming a fresh one because the JDK has no native
     * sliding-deadline scheduler.
     *
     * @param queue the mutable ref queue shared across ticks
     */
    private void runRotationTick(ArrayDeque<String> queue) {
        String next;
        long rotationDelay;
        synchronized (rotationLock) {
            cancelRotationLocked();

            if (whatsapp.store().accountStore().registered()) {
                return;
            }

            if (queue.isEmpty()) {
                return;
            }

            rotationDelay = queue.size() == 6 ? QR_ROTATION_MS : REFRESH_ROTATION_MS;
            next = queue.pollFirst();
        }

        publishVerificationValue(next);

        synchronized (rotationLock) {
            if (queue.isEmpty()) {
                return;
            }

            rotationTask = ScheduledTask.scheduleDelayed(Duration.ofMillis(rotationDelay), () -> runRotationTick(queue));
        }
    }

    /**
     * Publishes a single QR ref to the verification handler.
     *
     * <p>The ref is combined with the noise, identity, and ADV keys into the
     * comma-separated payload encoded into the QR image. A {@code null} or
     * blank ref is ignored, and the publish is suppressed unless the
     * verification handler is a {@link LinkedWhatsAppClientVerificationHandler.Web.QrCode}.
     * This path runs only on the QR branch; the pairing-code branch runs
     * through {@link CompanionPairingService}, which feeds the handler a
     * client-generated code derived from its own random bytes.
     *
     * @param ref the ref string to publish; ignored when {@code null} or blank
     */
    private void publishVerificationValue(String ref) {
        if (ref == null || ref.isBlank()) {
            return;
        }

        if (!(webVerificationHandler instanceof LinkedWhatsAppClientVerificationHandler.Web.QrCode)) {
            return;
        }
        webVerificationHandler.handle(buildQrPayload(ref));
    }

    /**
     * Builds the comma-separated QR payload string from the given ref.
     *
     * <p>The payload is the exact string the official WhatsApp mobile app reads
     * off the QR code, of the form
     * {@code <ref>,<noise>,<identity>,<adv>,<clientType>}, where the keys are
     * base64-encoded and the client type is the lowercased device client-type
     * name.
     *
     * @implNote
     * This implementation regenerates the ADV secret key when the store has
     * not been seeded yet and persists it before assembling the payload, so a
     * subsequent pair-success exchange uses the same key.
     *
     * @param ref the QR ref from the server
     * @return the assembled QR payload string
     */
    private String buildQrPayload(String ref) {
        var store = whatsapp.store();
        var advSecret = store.signalStore().advSecretKey().orElseGet(() -> {
            var generated = DataUtils.randomByteArray(32);
            store.signalStore().setAdvSecretKey(generated);
            return generated;
        });

        var encoder = Base64.getEncoder();
        var noise = encoder.encodeToString(store.signalStore().noiseKeyPair().publicKey().toEncodedPoint());
        var identity = encoder.encodeToString(store.signalStore().identityKeyPair().publicKey().toEncodedPoint());
        var secret = encoder.encodeToString(advSecret);
        return String.join(",",
                ref,
                noise,
                identity,
                secret,
                whatsapp.store().accountStore().device().clientType().name().toLowerCase());
    }

    /**
     * Handles the {@code <iq><pair-success/></iq>} stanza that lands the
     * validated identity exchange and finalises companion registration.
     *
     * <p>The handler returns early when the store is already registered, then
     * cancels any in-flight rotation, validates the ADV signed-device identity,
     * persists the resulting JID and LID, persists the validated identity,
     * responds with an {@code <iq><pair-device-sign/></iq>} carrying the device
     * signature so the primary device can finish linking, applies any embedded
     * sub-state (syncd-snapshot-recovery support and 1-on-1 LID migration), and
     * finally flips the registered, online, and pairing-timestamp fields and
     * persists the store. When ADV validation fails, an error-code funnel commit
     * is emitted and the handler abandons the exchange. A
     * {@link RuntimeException} thrown by the response path emits the failure
     * commit and is rethrown so the configured error handler still runs.
     *
     * @implNote
     * This implementation folds the upstream module-level re-entry flag and the
     * separate registered guard into a single {@code store.accountStore().registered()} call,
     * because the dispatcher already runs each stanza on its own virtual thread
     * and the registered flag is flipped at the end of a successful pair. The
     * funnel telemetry emissions
     * ({@link MdLinkDeviceCompanionStage#PAIR_SUCCESS_RECEIVED} after
     * validation, {@link MdLinkDeviceCompanionStage#PAIR_DEVICE_SIGN_SENT}
     * after the response, and the error-code commit on validation failure and
     * rollback) mirror the upstream device-link reporter sequence so the funnel
     * matches the official client.
     *
     * @param iqStanza the full {@code <iq>} stanza containing the
     *               {@code <pair-success/>} child
     */
    private void handlePairSuccess(Stanza iqStanza) {
        if (whatsapp.store().accountStore().registered()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring pair-success iq: store already registered");
            return;
        }

        synchronized (rotationLock) {
            cancelRotationLocked();
        }

        var regStartSeconds = Instant.now().getEpochSecond();

        var pairSuccess = iqStanza.getChild("pair-success").orElse(null);
        if (pairSuccess == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "received md iq without pair-success child");
            return;
        }

        var store = whatsapp.store();

        resolvePairedJid(pairSuccess, false).ifPresent(jid -> {
            store.accountStore().setJid(jid);
            if (store.accountStore().phoneNumber().isEmpty()) {
                try {
                    store.accountStore().setPhoneNumber(Long.parseLong(jid.user()));
                } catch (NumberFormatException _) {
                }
            }
        });
        resolvePairedJid(pairSuccess, true).ifPresent(store.accountStore()::setLid);
        pairSuccess.getChild("platform")
                .flatMap(platform -> platform.getAttributeAsString("name"))
                .flatMap(LinkedPrimaryPlatform::ofWireValue)
                .ifPresent(store.accountStore()::setPrimaryPlatform);

        var validatedIdentity = deviceService.extractAndValidateLocalSignedDeviceIdentity(pairSuccess)
                .orElse(null);

        if (validatedIdentity == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "pair-success adv identity validation failed");
            emitMdLinkDeviceCompanionStage(null, -1, null, regStartSeconds);
            return;
        }

        var mdSessionId = computeMdLinkSessionId(
                validatedIdentity.accountSignatureKey().orElse(null),
                store.signalStore().identityKeyPair().publicKey().toEncodedPoint()
        );
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pair-success adv identity validated, session {0}", new LogRedactable.Token(mdSessionId));
        emitMdLinkDeviceCompanionStage(MdLinkDeviceCompanionStage.PAIR_SUCCESS_RECEIVED, null, mdSessionId, regStartSeconds);

        try {
            store.accountStore().jid().ifPresent(localJid -> deviceService.persistLocalDeviceIdentityFromPairSuccess(
                    localJid, validatedIdentity.accountSignatureKey().orElse(null)));
            store.signalStore().setSignedDeviceIdentity(validatedIdentity);
            sendPairSuccessResponse(iqStanza, validatedIdentity);

            emitMdLinkDeviceCompanionStage(MdLinkDeviceCompanionStage.PAIR_DEVICE_SIGN_SENT, null, mdSessionId, regStartSeconds);

            extractPairingProps(pairSuccess)
                    .ifPresent(props -> {
                        snapshotRecoveryService.updatePrimaryDeviceSupportsSyncdRecovery(props.isSyncdSnapshotRecoveryEnabled());
                        if (props.isChatDbLidMigrated()) {
                            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                                    .migrationStage(MigrationStageEnum.COMPANION_MIGRATED_ON_NEW_PAIRING)
                                    .webClientDidPairingStanzaIndicated1x1MigrationThisSession(true)
                                    .isLocally1x1MigratedFromDb(lidMigrationService.isLidMigrated())
                                    .build());
                        }
                    });
            store.accountStore().setPairingTimestamp(Instant.ofEpochSecond(regStartSeconds));
            store.accountStore().setRegistered(true);
            store.accountStore().setOnline(true);
            safeSave("pair-success");
            if (Log.INFO) LOGGER.log(Level.INFO, "device pairing completed");
            acquireWhatsAppWebGraphQlSession(pairSuccess);
        } catch (RuntimeException exception) {
            emitMdLinkDeviceCompanionStage(null, -1, mdSessionId, regStartSeconds);
            if (Log.ERROR) LOGGER.log(Level.ERROR, "pair-success handling failed", exception);
            throw exception;
        }
    }

    /**
     * Performs the best-effort canonical-registration step that bootstraps the
     * {@code http_relay} transport's credentials at pair-success.
     *
     * <p>This mirrors the trailing {@code handleCanonicalRegistration} call WA
     * Web makes at the end of {@code handlePairSuccess}: it reads the optional
     * {@code <encryption-metadata/>} child of {@code <pair-success/>}, decrypts
     * the canonical nonce blob with
     * {@link WhatsAppWebGraphQlCanonicalNonceDecryptor#decrypt(byte[], byte[], SmaxMdSetRegEncryptionMetadata)}
     * (HKDF-SHA256 over the ADV secret key salted by the Noise static public
     * key, then AES-256-GCM), stitches in the local device id taken from the
     * paired JID, fetches the {@code lsd} token and exchanges the credentials at
     * {@code /auth/token/}, and on success populates the WhatsApp Web GraphQL session via
     * {@link LinkedWhatsAppClient#establishWhatsAppWebGraphQlSession(String, String)} so the
     * relay GraphQL transport stops throwing "WhatsApp Web GraphQL session not established".
     *
     * <p>The whole step is guarded: any missing input, decryption failure, HTTP
     * failure, or unsuccessful exchange is logged at {@code DEBUG} and swallowed
     * so a credential-acquisition problem never aborts an otherwise successful
     * pairing, matching WA Web's own try/catch around the canonical step. Each
     * critical failure of the recovery flow (absent encryption metadata, absent
     * paired JID, an undecryptable nonce blob, or an unexpected throwable) also
     * commits a {@code CanonicalEntRecoveryCriticalEvent} through
     * {@link #emitCanonicalRecoveryCriticalEvent(String, String, String)} so the
     * canonical-entity recovery funnel reported to the WhatsApp dashboards
     * matches the official client; the successful exchange emits none.
     *
     * @implNote This implementation instantiates a fresh
     * {@link WhatsAppWebGraphQlBootstrapClient} per pairing so the bootstrap {@code GET} and
     * the {@code /auth/token/} POST share one cookie jar; the HttpOnly session
     * cookie is replayed opaquely rather than read by name.
     *
     * @param pairSuccess the {@code <pair-success/>} stanza whose optional
     *                    {@code <encryption-metadata/>} child carries the
     *                    canonical nonce blob
     */
    private void acquireWhatsAppWebGraphQlSession(Stanza pairSuccess) {
        try {
            var store = whatsapp.store();
            var metadata = pairSuccess.getChild("encryption-metadata")
                    .flatMap(SmaxMdSetRegEncryptionMetadata::of)
                    .orElse(null);
            if (metadata == null) {
                emitCanonicalRecoveryCriticalEvent("registration_missing_metadata", "registration", null);
                return;
            }

            var pairedJid = store.accountStore().jid().orElse(null);
            if (pairedJid == null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "canonical registration missing paired jid; skipping whatsapp web graphql bootstrap");
                emitCanonicalRecoveryCriticalEvent("registration_missing_jid", "registration", null);
                return;
            }

            var noiseStaticPublicKey = store.signalStore().noiseKeyPair().publicKey().toEncodedPoint();
            var credentials = WhatsAppWebGraphQlCanonicalNonceDecryptor.decrypt(
                            store.signalStore().advSecretKey().orElse(null), noiseStaticPublicKey, metadata)
                    .orElse(null);
            if (credentials == null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "canonical nonce blob absent or undecryptable; skipping whatsapp web graphql bootstrap");
                emitCanonicalRecoveryCriticalEvent("nonce_decryption_failed", "registration", null);
                return;
            }

            var bootstrap = new WhatsAppWebGraphQlBootstrapClient();
            var lsd = bootstrap.fetchLsd();
            if (bootstrap.exchange(credentials.withDeviceId(pairedJid.device()), lsd)) {
                store.webSessionStore().setWhatsAppWebGraphQlSession(new WhatsAppWebGraphQlSessionBuilder()
                        .sessionCookie(bootstrap.cookieHeader())
                        .lsdToken(lsd)
                        .canonicalAccessToken(credentials.accessToken())
                        .fbid(credentials.fbid())
                        .build());
                store.save();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "established http_relay session at pair-success");
            } else {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "canonical auth/token exchange did not succeed; whatsapp web graphql session not established");
            }
        } catch (Throwable throwable) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "whatsapp web graphql credential acquisition failed", throwable);
            emitCanonicalRecoveryCriticalEvent("registration_unexpected_error", "registration", String.valueOf(throwable));
        }
    }

    /**
     * Commits one {@code CanonicalEntRecoveryCriticalEvent} recording a critical
     * failure of the canonical-entity (account credential) recovery flow.
     *
     * <p>The commit carries the failure name, the stable
     * {@link #canonicalRegistrationTraceId() registration trace id}, the local
     * device id (the paired JID's device index rendered as a decimal string, or
     * the empty string when no JID is available), the always-empty family device
     * id, and a JSON metadata blob of the form {@code {"purpose":"..."}} or, when
     * an error string is supplied, {@code {"purpose":"...","error":"..."}}. The
     * sequence-number, {@code traceIdInt}, and request-id properties are left
     * unset because the upstream {@code logCriticalRecoveryEvent} caller does not
     * populate them for the registration flow.
     *
     * @implNote
     * This implementation assembles the metadata JSON by hand via
     * {@link #jsonString(String)} rather than pulling in a JSON serializer,
     * because the shape is a fixed one- or two-key object; it derives the device
     * id from the paired JID because Cobalt has no {@code getMaybeMeDeviceId}
     * user-prefs accessor. A {@link RuntimeException} raised by the commit is
     * caught and logged so a telemetry failure cannot disrupt pairing.
     *
     * @param eventName the failure name recorded as the critical-event name
     *                  (for example {@code "nonce_decryption_failed"})
     * @param purpose   the recovery purpose recorded in the metadata blob
     *                  (always {@code "registration"} for the pair-success flow)
     * @param error     the error detail folded into the metadata blob, or
     *                  {@code null} to omit the {@code "error"} key
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalEntRecoveryWam", exports = "logCriticalRecoveryEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitCanonicalRecoveryCriticalEvent(String eventName, String purpose, String error) {
        try {
            var deviceId = whatsapp.store().accountStore().jid()
                    .map(jid -> String.valueOf(jid.device()))
                    .orElse("");
            var metadata = error == null
                    ? "{\"purpose\":" + jsonString(purpose) + "}"
                    : "{\"purpose\":" + jsonString(purpose) + ",\"error\":" + jsonString(error) + "}";
            wamService.commit(new CanonicalEntRecoveryCriticalEventEventBuilder()
                    .canonicalEntRecoveryCriticalEventName(eventName)
                    .canonicalEntRegistrationTraceId(canonicalRegistrationTraceId())
                    .canonicalEntRecoveryCriticalEventMetadata(metadata)
                    .deviceId(deviceId)
                    .familyDeviceId("")
                    .build());
        } catch (RuntimeException wamException) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot commit canonicalentrecoverycriticalevent event", wamException);
        }
    }

    /**
     * Returns the canonical-registration trace id, minting one on first use.
     *
     * <p>The id is a random UUID that stays constant for the lifetime of this
     * handler once created, so every critical-event commit of a single pairing
     * attempt shares the same trace id.
     *
     * @implNote
     * This implementation tolerates the benign race of two concurrent callers
     * each minting a UUID because the canonical-recovery critical path runs on a
     * single pairing thread; the last write wins and the funnel still correlates.
     *
     * @return the stable registration trace id; never {@code null}
     */
    private String canonicalRegistrationTraceId() {
        var traceId = canonicalRegistrationTraceId;
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            canonicalRegistrationTraceId = traceId;
        }
        return traceId;
    }

    /**
     * Encodes the given value as a JSON string literal, quotes included.
     *
     * <p>The result wraps {@code value} in double quotes and escapes the
     * characters JSON requires: the quote, the backslash, the tab, and the
     * carriage-return and newline control characters, with any remaining control
     * character below {@code 0x20} emitted as a {@code \\u00xx} escape. This
     * mirrors {@code JSON.stringify} closely enough for the fixed metadata blobs
     * built by {@link #emitCanonicalRecoveryCriticalEvent(String, String, String)}.
     *
     * @param value the raw string to encode
     * @return the JSON string literal, including the surrounding quotes
     */
    private static String jsonString(String value) {
        var builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (var index = 0; index < value.length(); index++) {
            var character = value.charAt(index);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        builder.append('"');
        return builder.toString();
    }

    /**
     * Computes the {@code mdSessionId} that uniquely identifies a pairing
     * attempt across the device-link WAM stages.
     *
     * <p>The id is the base64 encoding of {@code SHA-256(accountSignatureKey ||
     * 0x5f || localIdentityKey)}. The same id is threaded through every
     * {@link MdLinkDeviceCompanionStage} commit so the funnel lines up across
     * stages. The method returns {@code null} when either input is
     * {@code null}.
     *
     * @implNote
     * This implementation falls through to {@code null} when {@code SHA-256} is
     * unavailable on the JRE (an unreachable path on any conforming platform)
     * so the rest of the funnel can still emit without a session id.
     *
     * @param accountSignatureKey the account signature key from the validated
     *                            {@link ADVSignedDeviceIdentity}; may be
     *                            {@code null} when the identity did not embed
     *                            one
     * @param localIdentityKey    the local companion identity public-key bytes
     * @return the base64-encoded {@code SHA-256} session id, or {@code null}
     *         when the input cannot produce a deterministic hash
     */
    private String computeMdLinkSessionId(byte[] accountSignatureKey, byte[] localIdentityKey) {
        if (accountSignatureKey == null || localIdentityKey == null) {
            return null;
        }

        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(accountSignatureKey);
            digest.update((byte) 0x5f);
            digest.update(localIdentityKey);
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sha-256 algorithm unavailable", exception);
            return null;
        }
    }

    /**
     * Commits one device-link funnel stage so the pairing checkpoint is
     * recorded.
     *
     * <p>A {@code null} stage paired with a non-{@code null} error code records
     * a raw error-code commit taken when the pairing flow aborts before the
     * stage machine advances; otherwise the supplied stage is recorded. The
     * commit always carries {@code mdTimestampS} (the {@code regStartSeconds}
     * baseline) and {@code mdDurationS} (the elapsed seconds since the
     * baseline), and carries {@code mdSessionId} when it is non-{@code null}.
     *
     * @implNote
     * This implementation only populates the subset of properties Cobalt can
     * derive deterministically ({@code mdSessionId}, {@code mdTimestampS},
     * {@code mdDurationS}, the stage, and the error code); the WA-Web-only
     * experience-id, locale, and application-state fields are left unset
     * because their source modules have no Cobalt counterpart. The commit is
     * wrapped in a {@link RuntimeException} catch so a WAM commit failure
     * cannot disrupt the pairing flow.
     *
     * @param stage           the stage to record, or {@code null} for a raw
     *                        error-code commit
     * @param errorCode       the error code to attach, or {@code null} for a
     *                        normal stage transition
     * @param mdSessionId     the deterministic session id produced by
     *                        {@link #computeMdLinkSessionId(byte[], byte[])};
     *                        may be {@code null} when the session id could not
     *                        be derived
     * @param regStartSeconds the Unix-seconds timestamp captured at
     *                        pair-success entry, used as the baseline for
     *                        {@code mdDurationS} and {@code mdTimestampS}
     */
    private void emitMdLinkDeviceCompanionStage(MdLinkDeviceCompanionStage stage, Integer errorCode, String mdSessionId, long regStartSeconds) {
        try {
            var nowSeconds = Instant.now().getEpochSecond();
            var builder = new MdLinkDeviceCompanionEventBuilder()
                    .mdTimestampS((int) regStartSeconds)
                    .mdDurationS((int) (nowSeconds - regStartSeconds))
                    .mdLinkDeviceCompanionErrorCode(errorCode != null ? errorCode : 0)
                    .mdLinkDeviceCompanionStage(stage);
            if (mdSessionId != null) {
                builder.mdSessionId(mdSessionId);
            }
            wamService.commit(builder.build());
        } catch (RuntimeException wamException) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot commit mdlinkdevicecompanion event", wamException);
        }
    }

    /**
     * Sends the {@code <iq><pair-device-sign><device-identity/></pair-device-sign></iq>}
     * response carrying the device-signed identity.
     *
     * <p>The response embeds the device-signed {@link ADVSignedDeviceIdentity},
     * the {@code key-index} attribute read from the inner
     * {@link ADVDeviceIdentitySpec}, and the inbound stanza {@code id} for
     * correlation, routed to {@link Jid#userServer()}. The outgoing identity
     * carries only the {@code accountSignature} and {@code deviceSignature};
     * the {@code accountSignatureKey} is intentionally omitted because the
     * server needs only the device signature. The method logs at {@code DEBUG}
     * or {@code WARNING} and returns without sending when the inbound stanza is
     * missing its {@code id}, the validated identity has no details, or the
     * inner device identity fails to decode or carries no key index; defensive
     * null checks are unavoidable because the inbound stanza shape is
     * server-driven.
     *
     * @param iqStanza            the original {@code <pair-success/>}
     *                          {@code <iq>} stanza
     * @param validatedIdentity the validated {@link ADVSignedDeviceIdentity}
     */
    private void sendPairSuccessResponse(Stanza iqStanza, ADVSignedDeviceIdentity validatedIdentity) {
        var id = iqStanza.getAttributeAsString("id", null);
        if (id == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cannot send pair-success response: missing id");
            return;
        }

        var details = validatedIdentity.details().orElse(null);
        if (details == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot send pair-success response: missing details in validated identity");
            return;
        }

        int keyIndex;
        try {
            var innerIdentity = ADVDeviceIdentitySpec.decode(details);
            keyIndex = innerIdentity.keyIndex().orElseThrow(() ->
                    new NullPointerException("keyIndex cannot be null"));
        } catch (Exception exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot send pair-success response: failed to decode inner device identity", exception);
            return;
        }

        var identityForResponse = new ADVSignedDeviceIdentityBuilder()
                .details(details)
                .accountSignature(validatedIdentity.accountSignature().orElse(null))
                .deviceSignature(validatedIdentity.deviceSignature().orElse(null))
                .build();

        var encodedIdentity = ADVSignedDeviceIdentitySpec.encode(identityForResponse);

        var deviceIdentityNode = new StanzaBuilder()
                .description("device-identity")
                .attribute("key-index", keyIndex)
                .content(encodedIdentity)
                .build();

        var pairDeviceSignNode = new StanzaBuilder()
                .description("pair-device-sign")
                .content(deviceIdentityNode)
                .build();

        var response = new StanzaBuilder()
                .description("iq")
                .attribute("id", id)
                .attribute("to", Jid.userServer())
                .attribute("type", "result")
                .content(pairDeviceSignNode)
                .build();

        whatsapp.sendNodeWithNoResponse(response);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent pair-device-sign response, key-index {0}", keyIndex);
    }

    /**
     * Resolves the paired device JID or LID from a {@code <pair-success/>}
     * stanza.
     *
     * <p>The value is read from the {@code jid} or {@code lid} attribute of the
     * {@code <device/>} child, selected by the {@code lid} flag.
     *
     * @param stanza the {@code <pair-success/>} stanza
     * @param lid  {@code true} to resolve the LID, {@code false} for the device
     *             JID
     * @return the resolved {@link Jid}, or an empty {@link Optional} when the
     *         attribute is absent
     */
    private Optional<Jid> resolvePairedJid(Stanza stanza, boolean lid) {
        return stanza.getChild("device")
                .flatMap(device -> device.getAttributeAsJid(lid ? "lid" : "jid"));
    }

    /**
     * Extracts and decodes the {@link ClientPairingProps} protobuf embedded in
     * a {@code <pair-success/>} stanza.
     *
     * <p>The bytes may live either in the {@code pair-success} content blob or
     * in any {@code <props/>} child; each candidate is decoded in order and the
     * first decode that succeeds is returned.
     *
     * @implNote
     * This implementation swallows individual decode failures so a malformed
     * {@code <props/>} child does not block the next candidate from being
     * tried.
     *
     * @param pairSuccess the {@code <pair-success/>} stanza
     * @return the decoded {@link ClientPairingProps}, or an empty
     *         {@link Optional} when no candidate decodes cleanly
     */
    private Optional<ClientPairingProps> extractPairingProps(Stanza pairSuccess) {
        var candidates = new ArrayList<Stanza>();
        candidates.add(pairSuccess);
        for (var child : pairSuccess.children()) {
            if ("props".equals(child.description())) {
                candidates.add(child);
            }
        }

        for (var candidate : candidates) {
            var bytes = candidate.toContentBytes().orElse(null);
            if (bytes == null || bytes.length == 0) {
                continue;
            }

            try {
                return Optional.of(ClientPairingPropsSpec.decode(bytes));
            } catch (Throwable _) {
            }
        }

        return Optional.empty();
    }

    /**
     * Cancels the currently scheduled rotation tick.
     *
     * @implSpec
     * Callers must hold {@link #rotationLock} for the duration of this method
     * so the cancel is atomic against a concurrent
     * {@link #runRotationTick(ArrayDeque)} reschedule.
     */
    private void cancelRotationLocked() {
        var task = rotationTask;
        if (task != null) {
            task.cancel();
            rotationTask = null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cancels any in-flight QR ref rotation so the next connection starts
     * from a clean slate. The {@link CompanionPairingService},
     * {@link DeviceService}, and the other dependencies hold no per-handler
     * state to reset.
     */
    @Override
    public void reset() {
        synchronized (rotationLock) {
            cancelRotationLocked();
        }
    }

    /**
     * Returns the first non-blank string attribute on the given stanza matching
     * one of the supplied keys.
     *
     * <p>The keys are tried in order and the first non-blank value wins. This
     * supports the multi-source ref extraction in
     * {@link #extractPairRefs(Stanza)}, where a ref may live on a {@code ref},
     * {@code value}, or {@code code} attribute rather than a content blob.
     *
     * @param stanza the stanza to search
     * @param keys the attribute keys to try, in priority order
     * @return the first non-blank attribute value, or an empty {@link Optional}
     *         when none of the keys produce one
     */
    private Optional<String> findStringAttribute(Stanza stanza, String... keys) {
        for (var key : keys) {
            var value = stanza.getAttributeAsString(key).orElse(null);
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the first {@link Jid}-typed attribute on the given stanza matching
     * one of the supplied keys.
     *
     * <p>The keys are tried in order and the first present value wins. This
     * supports stanza shapes that carry the same logical JID under multiple
     * attribute names.
     *
     * @param stanza the stanza to search
     * @param keys the attribute keys to try, in priority order
     * @return the first {@link Jid} attribute value, or an empty
     *         {@link Optional} when none of the keys produce one
     */
    private Optional<Jid> findJidAttribute(Stanza stanza, String... keys) {
        for (var key : keys) {
            var value = stanza.getAttributeAsJid(key).orElse(null);
            if (value != null) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Decodes the content of the given stanza as a UTF-8 string.
     *
     * <p>The typed string accessor is preferred; when it yields nothing the raw
     * content bytes are decoded as UTF-8. This lets a ref encoded as either a
     * {@code <ref>...</ref>} text stanza or a {@code <ref/>} binary blob resolve
     * through the same path in {@link #extractPairRefs(Stanza)}.
     *
     * @param stanza the stanza whose content to decode
     * @return the non-blank decoded string, or an empty {@link Optional} when
     *         the content is missing or blank
     */
    private Optional<String> decodeContentAsString(Stanza stanza) {
        var text = stanza.toContentString().orElse(null);
        if (text != null && !text.isBlank()) {
            return Optional.of(text);
        }

        var bytes = stanza.toContentBytes().orElse(null);
        if (bytes == null || bytes.length == 0) {
            return Optional.empty();
        }

        var decoded = new String(bytes, StandardCharsets.UTF_8);
        return decoded.isBlank() ? Optional.empty() : Optional.of(decoded);
    }

    /**
     * Persists the store to disk, logging any failure at {@code DEBUG}.
     *
     * <p>Invoked at the end of the pair-success handler so a successful pair
     * survives a process restart. Persistence is best-effort because the
     * bootstrap path that runs next re-persists the store.
     *
     * @param context a human-readable context string used in the {@code DEBUG}
     *                log entry on failure
     */
    private void safeSave(String context) {
        try {
            whatsapp.store().save();
        } catch (Exception exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "failed to persist store during " + context, exception);
        }
    }
}
