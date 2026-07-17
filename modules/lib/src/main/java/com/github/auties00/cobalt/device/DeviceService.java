package com.github.auties00.cobalt.device;

import com.github.auties00.cobalt.device.icdc.HostedIcdcResult;
import com.github.auties00.cobalt.device.icdc.IcdcResult;
import com.github.auties00.cobalt.wire.linked.device.DeviceListMetadata;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentity;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrates device-list operations for WhatsApp multi-device.
 *
 * <p>This service is consulted whenever Cobalt needs the recipient's set of linked devices, needs
 * to refresh that set after an ADV change notification, needs to apply Identity Change Detection
 * Consistency metadata extracted from inbound messages, or needs to manage the local user's own
 * device identity. {@link LiveDeviceService} is the production implementation that talks to the
 * server via USync queries, the ADV pipeline, and the Signal protocol store.
 *
 * @implSpec
 * Implementations must be thread-safe; multiple sender pipelines may call the fanout methods
 * concurrently while the ADV check scheduler is running.
 */
public interface DeviceService {
    /**
     * Returns the device lists for the given user JIDs by running a USync query.
     *
     * <p>This is the entry point for sender pipelines that need every device JID a chat fans out
     * to. Passing {@code shouldMergeAltDevices = true} from chats that address recipients in both
     * PN and LID forms augments each returned entry with the device-id set of its
     * alternate-addressing twin.
     *
     * @implSpec
     * Implementations must run a USync query for the requested users, honour {@code expectedPhash}
     * as a short-circuit when the locally-computed participant hash matches, and apply the
     * alternate-device merge when {@code shouldMergeAltDevices} is {@code true}.
     *
     * @param userJids              the user JIDs to query
     * @param context               the USync context string the server uses for telemetry and
     *                              rate-limiting (for example, {@code "message"} or
     *                              {@code "interactive"})
     * @param expectedPhash         the expected participant hash to validate against the server's
     *                              response, or {@code null} when no expectation is held
     * @param shouldMergeAltDevices when {@code true}, each entry's device ids are merged with its
     *                              alternate-addressing twin's
     * @return the device lists
     */
    Set<DeviceList> getDeviceLists(Collection<Jid> userJids, String context, String expectedPhash, boolean shouldMergeAltDevices);

    /**
     * Returns the wall-clock instant at which the ADV expiration check last ran.
     *
     * <p>The ADV check scheduler inspects this value to compute the initial delay before its first
     * periodic tick. The result is empty before the first check has ever run.
     *
     * @implSpec
     * Implementations must return empty until {@link #updateAdvCheckTime()} has been called at
     * least once.
     *
     * @return the last check timestamp, or empty when never checked
     */
    Optional<Instant> lastAdvCheckTime();

    /**
     * Updates the persisted ADV check timestamp to the current instant.
     *
     * <p>The ADV check scheduler calls this at the end of every periodic tick (even the first
     * no-op tick) so {@link #lastAdvCheckTime()} converges to the wall clock.
     *
     * @implSpec
     * Implementations must set the persisted timestamp such that the next
     * {@link #lastAdvCheckTime()} reflects the current instant.
     */
    void updateAdvCheckTime();

    /**
     * Starts the 24-hour ADV expiration check scheduler.
     *
     * <p>This call is idempotent. Embedders that drive Cobalt's full multi-device lifecycle invoke
     * it once after the socket is online; embedders that only use Cobalt for short-lived sessions
     * may leave the scheduler off and accept stale device lists.
     *
     * @implSpec
     * Implementations must tolerate repeated calls without spawning duplicate schedulers.
     */
    void startAdvCheckScheduler();

    /**
     * Stops the ADV expiration check scheduler and cancels pending checks.
     *
     * <p>This call is idempotent and is made on client teardown. It must never be called from
     * inside the scheduler tick, because it shuts down the executor that runs the tick.
     *
     * @implSpec
     * Implementations must tolerate repeated calls and must release the scheduler's executor.
     */
    void stopAdvCheckScheduler();

    /**
     * Retries any device syncs that previously failed or were queued by the ADV check job.
     *
     * <p>The ADV check scheduler invokes this after it has queued lists that are expired or close
     * to expiration, and the receive path invokes it when a connection resumes and pending USync
     * queries can be flushed.
     *
     * @implSpec
     * Implementations must replay every queued or failed sync and must drop entries that have
     * expired or exhausted their retry budget.
     */
    void retryPendingSyncs();

    /**
     * Re-fetches device records whose Signal sessions are missing locally.
     *
     * <p>The send pipeline uses this to repair the local store before sending: any companion
     * device that the cached list claims but the Signal session store does not know about has its
     * identity re-fetched via USync.
     *
     * @implSpec
     * Implementations must reconcile the cached device set against the Signal session store and
     * re-fetch the records whose sessions are absent.
     */
    void updateMissingKeyDevices();

    /**
     * Returns the device fanout for a 1:1 chat with the given user.
     *
     * <p>This resolves the recipient's companion devices plus the local sender's other devices
     * (with the sending device stripped), returning the exact set of Signal addresses the outbound
     * message must be encrypted to.
     *
     * @implSpec
     * Implementations must exclude the sending device and apply the configured hosted-device and
     * identity-change filters before returning.
     *
     * @param chatJid       the chat JID (the recipient user)
     * @param expectedPhash the expected participant hash to validate, or {@code null} when no
     *                      expectation is held
     * @return the device JIDs to fan out to
     */
    Collection<Jid> getUserFanout(Jid chatJid, String expectedPhash);

    /**
     * Returns the recipient device fanout for a group chat.
     *
     * <p>This drives group-message sends. It refreshes the group metadata, resolves every
     * participant's device list, and strips the sender's own device. The participant hash is not
     * computed here: a send path filters this set by addressing mode and then hashes the result
     * through {@link #computeGroupPhash(Collection, Jid, boolean, boolean)}, mirroring WA Web where
     * {@code getFanOutList} returns devices and {@code phashV2} runs separately in the send job.
     *
     * @implSpec
     * Implementations must exclude the sender's own device from the returned set.
     *
     * @param chatJid the group JID
     * @return the recipient device JIDs
     */
    Set<Jid> getGroupFanout(Jid chatJid);

    /**
     * Computes the V2 participant hash for a group send over the final recipient set.
     *
     * <p>WhatsApp Web computes the {@code phash} attribute inside the send job, over the
     * addressing-mode-filtered recipient device list ({@code skList} concatenated with
     * {@code skDistribList}) folded together with the sender's own device in the conversation's
     * addressing mode. This method exposes that computation so a send path hashes the exact device
     * set it is about to address, after it has filtered the {@link #getGroupFanout(Jid)} set by
     * addressing mode.
     *
     * @implSpec
     * Implementations must fold {@code senderDeviceJid} into the hashed set and compute the V2
     * phash; the bot gates are ANDed with the corresponding AB-prop checks so a non-bot group
     * never injects a bot JID.
     *
     * @param recipientDevices the final, addressing-mode-filtered recipient device JIDs
     * @param senderDeviceJid  the sender's own device JID in the conversation's addressing mode,
     *                         folded into the hashed set
     * @param openBotGroup     whether this is an open Meta AI bot group, gating open-bot injection
     * @param teeBotGroup      whether this is a TEE Meta AI bot group, gating TEE-bot injection
     * @return the {@code phash} attribute value, prefixed with {@code "2:"}
     */
    String computeGroupPhash(Collection<Jid> recipientDevices, Jid senderDeviceJid, boolean openBotGroup, boolean teeBotGroup);

    /**
     * Returns the recipient device fanout for a business broadcast-list send.
     *
     * <p>Broadcast lists are a client-only audience model. The recipient roster is stored locally
     * on {@link com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastList} and never
     * round-tripped through server-side group metadata, so the caller passes the resolved
     * recipient user JIDs explicitly rather than expecting the SKMSG-target
     * {@code <id>@broadcast} JID to drive a server-side metadata lookup. The fanout calculator and
     * identity filter mirror {@link #getGroupFanout(Jid)} verbatim.
     *
     * @implSpec
     * Implementations must resolve devices over {@code recipientUserJids} together with the local
     * user (so the sender's own other devices receive the broadcast sender key), merge alternate
     * (PN/LID) devices, and exclude the sending device from the returned set.
     *
     * @param broadcastJid      the broadcast list JID ({@code <id>@broadcast}); used for
     *                          diagnostics only, not for metadata resolution
     * @param recipientUserJids the resolved recipient user JIDs from the local broadcast list
     *                          roster
     * @return the recipient device JIDs
     */
    Set<Jid> getBroadcastFanout(Jid broadcastJid, Collection<Jid> recipientUserJids);

    /**
     * Returns the recipient device fanout for a status broadcast over the given audience.
     *
     * <p>A status post has no server-side group metadata: its audience is derived from the user's
     * status privacy preference and supplied here as resolved user JIDs. Mirrors WA Web's
     * {@code encryptAndSendStatusMsg}, which resolves devices through
     * {@code getFanOutList({wids: [...audience, meLidDevice], shouldMergeAltDevices: true})} rather
     * than a group lookup, so this must never query group metadata.
     *
     * @implSpec
     * Implementations must resolve devices over {@code audienceUserJids} together with the local
     * user (so the poster's own other devices receive the status sender key), merge alternate
     * (PN/LID) devices, and exclude the sending device from the returned set.
     *
     * @param audienceUserJids the resolved status audience user JIDs
     * @return the recipient device JIDs
     */
    Set<Jid> getStatusFanout(Collection<Jid> audienceUserJids);

    /**
     * Computes the Identity Change Detection Consistency metadata for the given user.
     *
     * <p>The send pipeline calls this for each message that needs to embed
     * {@code deviceListMetadata} in its {@code messageContextInfo} so the recipient can detect a
     * stale view of the sender's or their own companion devices. The result is empty when no cached
     * device list is available for the user.
     *
     * @implSpec
     * Implementations must return empty when the user has no usable cached device list and must
     * otherwise compute the metadata against the cached record.
     *
     * @param userJid the user JID
     * @return the ICDC result, or empty when no cached device list is available for the user
     */
    Optional<IcdcResult> computeIcdc(Jid userJid);

    /**
     * Ensures that Signal sessions exist for every device JID supplied, fetching a prekey bundle for
     * any device that lacks one.
     *
     * <p>The send pipeline calls this before encrypting outbound messages. This is a convenience for
     * {@link #ensureSessions(Collection, boolean)} with {@code force} {@code false}, so only devices
     * without a cached session are fetched.
     *
     * @param deviceJids the device JIDs whose sessions are needed
     * @return the number of devices whose one-time pre-key pool was depleted during the fetch
     */
    default int ensureSessions(Collection<Jid> deviceJids) {
        return ensureSessions(deviceJids, false);
    }

    /**
     * Ensures that Signal sessions exist for every device JID supplied, optionally re-establishing a
     * fresh session for every device even when one is already cached.
     *
     * <p>With {@code force} {@code false} only devices without a cached session are fetched. With
     * {@code force} {@code true} the existing session for every device is dropped and re-fetched, so a
     * peer holding a stale or one-sided session is given a fresh, decryptable session; the call path
     * uses this so a stale session does not make the peer silently reject the offer.
     *
     * @param deviceJids the device JIDs whose sessions are needed
     * @param force      whether to drop and re-establish a session for every device rather than only
     *                   for devices that lack one
     * @return the number of devices whose one-time pre-key pool was depleted during the fetch
     */
    int ensureSessions(Collection<Jid> deviceJids, boolean force);

    /**
     * Handles an incoming ADV device-list change notification.
     *
     * <p>The stream handler routes the {@code <devices>} child of a
     * {@code <notification type="account_sync">} stanza here. The {@code action} is the
     * {@code type} attribute on the outer notification ({@code "add"} when a companion device was
     * linked, {@code "remove"} when one was unlinked).
     *
     * @implSpec
     * Implementations must apply the add or remove against the cached device list for
     * {@code userJid} and must ignore notifications that are older than the cached record.
     *
     * @param stanza    the {@code <devices>} child stanza
     * @param action  the action string ({@code "add"} or {@code "remove"})
     * @param userJid the user JID whose device list changed
     */
    void handleDeviceNotification(Stanza stanza, String action, Jid userJid);

    /**
     * Rebuilds the cached device-list record for one self identity from an inline {@code <devices>}
     * account-sync payload.
     *
     * <p>The account-sync stream handler is responsible for the surrounding orchestration (deferring
     * during resume-from-restart, fanning a self sender out to both the phone-number and LID
     * identities, and falling back to a full self USync when no devices are listed); this method
     * rebuilds the record for the single {@code wid} it is given. The payload carries the current
     * companion {@code <device>} children and, when the primary signs an updated key index, an
     * embedded {@code <key-index-list>}.
     *
     * @implSpec
     * Implementations must verify the embedded {@code <key-index-list>} against {@code wid}'s primary
     * (device {@code 0}) identity and rebuild the device set as the {@code <device>} children filtered
     * to the signed {@code validIndexes}, merged with cached devices whose key index is newer than the
     * signed {@code currentIndex}, plus the primary device. Implementations must populate the ADV
     * fingerprint fields ({@code rawId}, {@code currentIndex}, {@code validIndexes}) from the signed
     * list so the record stays consistent with the app-state sync key fingerprints the primary shares,
     * and must not store the device-list {@code dhash} in the {@code rawId} slot. Implementations must
     * leave a cached record untouched when the payload carries no verifiable key-index-list, when the
     * notification predates the cached snapshot, or when the signed timestamp does not match the
     * notification timestamp.
     *
     * @param wid         the self identity (phone-number or LID user JID) whose record is rebuilt
     * @param devicesStanza the {@code <devices>} child stanza carrying {@code <device>} children and an
     *                    optional {@code <key-index-list>}
     */
    void refreshOwnDeviceList(Jid wid, Stanza devicesStanza);

    /**
     * Synchronises the local user's own device list with the server.
     *
     * <p>This is triggered after pair-success and after the offline pipeline finishes replaying
     * queued notifications; it refreshes the cached own-device list to reflect server-side changes
     * that might have happened while this session was offline.
     *
     * @implSpec
     * Implementations must sync the device list for whichever of the local PN and LID identities
     * are present, and must be a no-op when neither is present.
     */
    void syncMyDeviceList();

    /**
     * Synchronises and returns the device lists for the given user JIDs.
     *
     * <p>This is a convenience for callers that need both the side effect of refreshing the cache
     * and the resolved lists; it is semantically equivalent to invoking {@link #getDeviceLists}
     * with {@code context = "message"} and {@code shouldMergeAltDevices = false}, then reading the
     * resulting cached entries.
     *
     * @implSpec
     * Implementations must issue the USync sweep before returning the resolved lists.
     *
     * @param userJids the user JIDs to query
     * @return the synchronised device lists
     */
    List<DeviceList> syncAndGetDeviceList(Collection<Jid> userJids);

    /**
     * Resolves the long-identifier (LID) for the given phone-number user, querying the server when the
     * mapping is not already cached.
     *
     * <p>Returns the cached phone-number-to-LID mapping when one is present; otherwise issues a focused
     * USync LID-protocol query (mirroring WA Web's {@code USyncLidProtocol}), records every mapping the
     * server returns, and returns the freshly learned LID. The input device suffix is ignored; the
     * returned LID is a device-stripped user JID. Used by the call-placement path, which addresses call
     * signaling by LID and must learn a peer's LID before sending an offer.
     *
     * @implSpec
     * Implementations must consult the cached mapping before any server round-trip, must persist every
     * learned mapping so subsequent lookups resolve from cache, and must return an empty result rather
     * than throwing when the server assigns no LID or the query fails.
     *
     * @param userPn the phone-number user JID whose LID must be resolved
     * @return the resolved LID user JID, or {@link Optional#empty()} when none is known or assigned
     * @throws NullPointerException if {@code userPn} is {@code null}
     */
    Optional<Jid> queryUserLid(Jid userPn);

    /**
     * Returns the cached device record for the given user JID without a server round-trip.
     *
     * <p>This is used when the caller already knows the cache is fresh (during message encoding,
     * for example). The result is empty when no record is cached; the caller decides whether to
     * issue a USync or treat the user as primary-only.
     *
     * @implSpec
     * Implementations must read only the local cache and must not trigger a server fetch.
     *
     * @param userJid the user JID
     * @return the device record, or empty when not stored
     */
    Optional<DeviceList> getDeviceRecord(Jid userJid);

    /**
     * Returns the cached device records for the given user JIDs.
     *
     * <p>This is the bulk counterpart of {@link #getDeviceRecord(Jid)}. The returned list omits
     * users with no cached entry; the caller reconciles by JID order, not by positional alignment.
     *
     * @implSpec
     * Implementations must read only the local cache and must not trigger a server fetch.
     *
     * @param userJids the user JIDs
     * @return the device records
     */
    List<DeviceList> bulkGetDeviceRecord(Collection<Jid> userJids);

    /**
     * Persists or replaces the cached device record.
     *
     * <p>The ADV update path uses this after a successful USync response or device notification;
     * the new record fully supersedes the existing one for its user JID.
     *
     * @implSpec
     * Implementations must replace any existing record keyed by the new record's user JID.
     *
     * @param record the device record
     */
    void createOrReplaceDeviceRecord(DeviceList record);

    /**
     * Persists or replaces the cached device records in bulk.
     *
     * <p>This is the bulk counterpart of {@link #createOrReplaceDeviceRecord(DeviceList)}, called
     * by the batched ADV applier so the underlying store can do one transactional write per
     * round-trip.
     *
     * @implSpec
     * Implementations must replace any existing record keyed by each record's user JID.
     *
     * @param records the device records
     */
    void bulkCreateOrReplaceDeviceRecord(Collection<DeviceList> records);

    /**
     * Returns the device id lists for the given user JIDs, fetching from the server when missing.
     *
     * <p>This is lighter-weight than {@link #getDeviceLists}: it returns only the device ids for
     * each user, suitable for fanout-only callers that do not need key indexes or timestamps. It
     * honours {@code shouldMergeAltDevices} the same way as {@link #getDeviceLists}.
     *
     * @implSpec
     * Implementations must apply the alternate-device merge when {@code shouldMergeAltDevices} is
     * {@code true}.
     *
     * @param userJids              the user JIDs
     * @param shouldMergeAltDevices when {@code true}, each entry's device ids are merged with its
     *                              alternate-addressing twin's
     * @return the device lists
     */
    List<DeviceList> getDeviceIds(Collection<Jid> userJids, boolean shouldMergeAltDevices);

    /**
     * Returns the device records to feed into a USync query for the given user JIDs.
     *
     * <p>The USync query builder consumes these records to extract each user's cached device hash,
     * snapshot timestamp, and expected timestamp so the server can return either a full device list
     * or a confirming omitted result.
     *
     * @implSpec
     * Implementations must project tombstoned and missing slots so the query builder can tell them
     * apart from live records.
     *
     * @param userJids the user JIDs
     * @return the device lists
     */
    List<DeviceList> getDeviceInfoForSync(Collection<Jid> userJids);

    /**
     * Returns whether the cached device list for {@code userJid} contains the device id.
     *
     * <p>The receive pipeline uses this to validate the {@code participant} attribute of an inbound
     * message against the sender's known devices; a mismatch triggers a device-list resync.
     *
     * @implSpec
     * Implementations must treat the primary device id as always present and must return
     * {@code false} for tombstoned or missing records.
     *
     * @param userJid  the user JID
     * @param deviceId the device id to look up
     * @return {@code true} when the device exists in the cached list
     */
    boolean hasDevice(Jid userJid, int deviceId);

    /**
     * Returns the local user's own cached device list.
     *
     * <p>The send pipeline uses this to compute the sender's own-device fanout and to feed the ICDC
     * sender-side metadata.
     *
     * @implSpec
     * Implementations must prefer the PN record and fall back to the LID record, and must signal a
     * failure when neither is cached.
     *
     * @return the local device list
     */
    DeviceList getMyDeviceList();

    /**
     * Returns every cached device list.
     *
     * <p>The ADV check scheduler uses this to walk every cached entry and decide which are expired,
     * close to expiration, or already deleted. Diagnostic surfaces consume it as well.
     *
     * @implSpec
     * Implementations must return a snapshot that the caller may iterate without holding a lock.
     *
     * @return the device lists
     */
    Collection<DeviceList> getAllDeviceLists();

    /**
     * Handles ICDC metadata extracted from an inbound message.
     *
     * <p>The inbound message processor routes here after decryption. When the sender's metadata
     * reports a newer device-list timestamp than the cached one, the cached entry's
     * expected-timestamp tracking fields are refreshed so the next ADV check job knows to fetch the
     * sender's devices again. When the sender is the recipient's own primary device and the
     * metadata carries only a timestamp (no key hash), the minimal-sync path resets the cached list
     * to primary-only and bumps the timestamp by one second.
     *
     * @implSpec
     * Implementations must be a no-op when {@code metadata} is {@code null} and must consult
     * {@code recipientUserJid} only when the sender is the local user's own primary.
     *
     * @param senderJid        the sender device JID
     * @param recipientUserJid the recipient user JID (only used when the sender is the local user's
     *                         own primary), or {@code null}
     * @param metadata         the {@code deviceListMetadata} from the inbound message's
     *                         {@code messageContextInfo}, or {@code null}
     */
    void handleICDCData(Jid senderJid, Jid recipientUserJid, DeviceListMetadata metadata);

    /**
     * Handles hosted-business ICDC metadata extracted from an inbound message and returns the
     * validation outcome.
     *
     * <p>This is called only when the {@code adv_accept_hosted_devices} AB prop is on. The returned
     * {@link HostedIcdcResult} tells the caller whether the local ADV account type disagrees with
     * the inbound HOSTED type (so a device-list refresh is required) and whether either side of the
     * chat is a hosted account (so coexistence UI markers may be needed).
     *
     * @implSpec
     * Implementations must return {@link HostedIcdcResult#DEFAULT} when hosted devices are
     * disabled, when the chat is the local user, when the chat JID is not a user JID, or when
     * {@code metadata} is {@code null}.
     *
     * @param chatJid   the chat JID
     * @param authorJid the message author JID
     * @param metadata  the {@code deviceListMetadata} from the inbound message's
     *                  {@code messageContextInfo}
     * @return the hosted ICDC result
     */
    HostedIcdcResult handleHostedIcdcMetadataInline(Jid chatJid, Jid authorJid, DeviceListMetadata metadata);

    /**
     * Handles an ADV device update extracted from an inbound message's prekey envelope.
     *
     * <p>The Signal session-creation path calls this once it has decrypted a prekey message: the
     * embedded identity key for the sender's device is forwarded along with the device's signed
     * metadata so the cached device list and Signal identity store both converge.
     *
     * @implSpec
     * Implementations must reject the primary device as input and must choose between a full list
     * reset and an incremental key-index update based on whether the {@code rawId} or primary
     * identity changed.
     *
     * @param deviceJid   the device JID
     * @param rawId       the raw device identifier
     * @param timestamp   the device timestamp (Unix seconds)
     * @param keyIndex    the device key index
     * @param identityKey the 32-byte raw device identity key
     * @param accountType the device account encryption type
     */
    void handleADVDeviceUpdateForMessage(
            Jid deviceJid,
            String rawId,
            long timestamp,
            int keyIndex,
            byte[] identityKey,
            ADVEncryptionType accountType
    );

    /**
     * Extracts and validates the local signed device identity from a {@code <device-identity>}
     * pairing stanza.
     *
     * <p>This is called once during pairing, when the server's {@code pair-success} envelope
     * carries the device-identity HMAC and signed payload. It returns the validated identity (which
     * carries the locally-generated device signature) on success, and empty when the payload fails
     * HMAC or signature checks so the caller can abort pairing.
     *
     * @implSpec
     * Implementations must validate the HMAC and account signature before returning a present
     * value and must return empty on any validation failure.
     *
     * @param deviceIdentityStanza the {@code <device-identity>} pairing stanza
     * @return the validated signed device identity, or empty when invalid
     */
    Optional<ADVSignedDeviceIdentity> extractAndValidateLocalSignedDeviceIdentity(Stanza deviceIdentityStanza);

    /**
     * Persists the local device identity emitted from a successful pair-success exchange.
     *
     * <p>This is the final pairing step: it takes the device JID and the account signature key
     * recovered from the validated {@link ADVSignedDeviceIdentity} and stores them on the local
     * store so subsequent sends can sign with them.
     *
     * @implSpec
     * Implementations must skip the write when {@code accountSignatureKey} is {@code null} or
     * empty.
     *
     * @param deviceJid           the local device JID
     * @param accountSignatureKey the pairing account signature key
     */
    void persistLocalDeviceIdentityFromPairSuccess(Jid deviceJid, byte[] accountSignatureKey);
}
