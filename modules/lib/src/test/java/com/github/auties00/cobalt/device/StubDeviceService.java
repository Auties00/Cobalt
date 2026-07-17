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
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Test-only {@link DeviceService} double used by tests in adjacent packages that need a
 * {@link DeviceService} dependency without the full {@link LiveDeviceService} collaborator graph.
 *
 * <p>Defaults are deliberately loud: every method raises an {@link UnsupportedOperationException}
 * carrying its own name (so the test report points at the missing stub) except
 * {@link #ensureSessions(Collection)}, {@link #computeIcdc(Jid)},
 * {@link #computeGroupPhash(Collection, Jid, boolean, boolean)}, and
 * {@link #syncAndGetDeviceList(Collection)}, which return a harmless default because several
 * sender and call-engine tests use them as no-op participants. Each {@code withXxx} setter installs
 * the handler for exactly one method and returns the stub so calls can be chained.
 */
public final class StubDeviceService implements DeviceService {

    private Function<Jid, Collection<Jid>> userFanout;
    private Function<Jid, Set<Jid>> groupFanout;
    private BiFunction<Collection<Jid>, Jid, String> groupPhash;
    private Function<Collection<Jid>, Set<Jid>> broadcastFanout;
    private Function<Collection<Jid>, Set<Jid>> statusFanout;
    private Function<Collection<Jid>, Integer> ensureSessions;
    private Function<Jid, Optional<IcdcResult>> computeIcdc;
    private Function<Collection<Jid>, List<DeviceList>> syncAndGetDeviceList;

    /**
     * Returns a new stub with every method throwing by default.
     *
     * @return the new stub
     */
    public static StubDeviceService create() {
        return new StubDeviceService();
    }

    /**
     * Installs the handler used by {@link #getUserFanout(Jid, String)}; the handler receives only
     * the chat JID, and the {@code expectedPhash} argument is discarded.
     *
     * @param handler the handler, keyed on chat JID
     * @return this stub for chaining
     */
    public StubDeviceService withUserFanout(Function<Jid, Collection<Jid>> handler) {
        this.userFanout = handler;
        return this;
    }

    /**
     * Installs the handler used by {@link #getGroupFanout(Jid)}.
     *
     * @param handler the handler, keyed on group JID
     * @return this stub for chaining
     */
    public StubDeviceService withGroupFanout(Function<Jid, Set<Jid>> handler) {
        this.groupFanout = handler;
        return this;
    }

    /**
     * Installs the handler used by
     * {@link #computeGroupPhash(Collection, Jid, boolean, boolean)}; the handler receives the
     * recipient devices and sender device JID, and the bot gates are discarded. When no handler is
     * installed the stub returns the canonical {@code "2:hash"}.
     *
     * @param handler the handler, keyed on recipient devices and sender device JID
     * @return this stub for chaining
     */
    public StubDeviceService withGroupPhash(BiFunction<Collection<Jid>, Jid, String> handler) {
        this.groupPhash = handler;
        return this;
    }

    /**
     * Installs the handler used by {@link #getBroadcastFanout(Jid, Collection)}; the handler
     * receives only the recipient user JIDs, and the broadcast JID is discarded.
     *
     * @param handler the handler, keyed on recipient user JIDs
     * @return this stub for chaining
     */
    public StubDeviceService withBroadcastFanout(Function<Collection<Jid>, Set<Jid>> handler) {
        this.broadcastFanout = handler;
        return this;
    }

    /**
     * Installs the handler used by {@link #getStatusFanout(Collection)}; the handler receives the
     * resolved status audience user JIDs.
     *
     * @param handler the handler, keyed on the audience user JIDs
     * @return this stub for chaining
     */
    public StubDeviceService withStatusFanout(Function<Collection<Jid>, Set<Jid>> handler) {
        this.statusFanout = handler;
        return this;
    }

    /**
     * Installs the handler used by {@link #ensureSessions(Collection)}, which returns the number of
     * sessions established; when no handler is installed the stub returns zero (treated as "all
     * sessions already exist").
     *
     * @param handler the handler
     * @return this stub for chaining
     */
    public StubDeviceService withEnsureSessions(Function<Collection<Jid>, Integer> handler) {
        this.ensureSessions = handler;
        return this;
    }

    /**
     * Installs the handler used by {@link #computeIcdc(Jid)}; when no handler is installed the stub
     * returns {@link Optional#empty()}.
     *
     * @param handler the handler, keyed on user JID
     * @return this stub for chaining
     */
    public StubDeviceService withComputeIcdc(Function<Jid, Optional<IcdcResult>> handler) {
        this.computeIcdc = handler;
        return this;
    }

    /**
     * Installs the handler used by {@link #syncAndGetDeviceList(Collection)}; when no handler is
     * installed the stub returns an empty list (treated as "no companion devices for any of the
     * queried users", which is the common shape call-engine tests exercise).
     *
     * @param handler the handler, keyed on the requested user JIDs
     * @return this stub for chaining
     */
    public StubDeviceService withSyncAndGetDeviceList(Function<Collection<Jid>, List<DeviceList>> handler) {
        this.syncAndGetDeviceList = handler;
        return this;
    }

    @Override
    public Set<DeviceList> getDeviceLists(Collection<Jid> userJids, String context, String expectedPhash, boolean shouldMergeAltDevices) {
        throw unsupported("getDeviceLists");
    }

    @Override
    public Optional<Instant> lastAdvCheckTime() {
        throw unsupported("lastAdvCheckTime");
    }

    @Override
    public void updateAdvCheckTime() {
        throw unsupported("updateAdvCheckTime");
    }

    @Override
    public void startAdvCheckScheduler() {
        throw unsupported("startAdvCheckScheduler");
    }

    @Override
    public void stopAdvCheckScheduler() {
        throw unsupported("stopAdvCheckScheduler");
    }

    @Override
    public void retryPendingSyncs() {
        throw unsupported("retryPendingSyncs");
    }

    @Override
    public void updateMissingKeyDevices() {
        throw unsupported("updateMissingKeyDevices");
    }

    @Override
    public Collection<Jid> getUserFanout(Jid chatJid, String expectedPhash) {
        if (userFanout == null) {
            throw unsupported("getUserFanout: install via withUserFanout(...)");
        }
        return userFanout.apply(chatJid);
    }

    @Override
    public Set<Jid> getGroupFanout(Jid groupJid) {
        if (groupFanout == null) {
            throw unsupported("getGroupFanout: install via withGroupFanout(...)");
        }
        return groupFanout.apply(groupJid);
    }

    @Override
    public String computeGroupPhash(Collection<Jid> recipientDevices, Jid senderDeviceJid, boolean openBotGroup, boolean teeBotGroup) {
        return groupPhash == null ? "2:hash" : groupPhash.apply(recipientDevices, senderDeviceJid);
    }

    @Override
    public Set<Jid> getBroadcastFanout(Jid broadcastJid, Collection<Jid> recipientUserJids) {
        if (broadcastFanout == null) {
            throw unsupported("getBroadcastFanout: install via withBroadcastFanout(...)");
        }
        return broadcastFanout.apply(recipientUserJids);
    }

    @Override
    public Set<Jid> getStatusFanout(Collection<Jid> audienceUserJids) {
        if (statusFanout == null) {
            throw unsupported("getStatusFanout: install via withStatusFanout(...)");
        }
        return statusFanout.apply(audienceUserJids);
    }

    @Override
    public Optional<IcdcResult> computeIcdc(Jid userJid) {
        if (computeIcdc == null) {
            return Optional.empty();
        }
        return computeIcdc.apply(userJid);
    }

    @Override
    public int ensureSessions(Collection<Jid> deviceJids, boolean force) {
        if (ensureSessions == null) {
            return 0;
        }
        return ensureSessions.apply(deviceJids);
    }

    @Override
    public void handleDeviceNotification(Stanza stanza, String action, Jid userJid) {
        throw unsupported("handleDeviceNotification");
    }

    @Override
    public void refreshOwnDeviceList(Jid wid, Stanza devicesStanza) {
        throw unsupported("refreshOwnDeviceList");
    }

    @Override
    public void syncMyDeviceList() {
        throw unsupported("syncMyDeviceList");
    }

    @Override
    public List<DeviceList> syncAndGetDeviceList(Collection<Jid> userJids) {
        if (syncAndGetDeviceList == null) {
            return List.of();
        }
        return syncAndGetDeviceList.apply(userJids);
    }

    @Override
    public Optional<Jid> queryUserLid(Jid userPn) {
        return Optional.empty();
    }

    @Override
    public Optional<DeviceList> getDeviceRecord(Jid userJid) {
        throw unsupported("getDeviceRecord");
    }

    @Override
    public List<DeviceList> bulkGetDeviceRecord(Collection<Jid> userJids) {
        throw unsupported("bulkGetDeviceRecord");
    }

    @Override
    public void createOrReplaceDeviceRecord(DeviceList record) {
        throw unsupported("createOrReplaceDeviceRecord");
    }

    @Override
    public void bulkCreateOrReplaceDeviceRecord(Collection<DeviceList> records) {
        throw unsupported("bulkCreateOrReplaceDeviceRecord");
    }

    @Override
    public List<DeviceList> getDeviceIds(Collection<Jid> userJids, boolean shouldMergeAltDevices) {
        throw unsupported("getDeviceIds");
    }

    @Override
    public List<DeviceList> getDeviceInfoForSync(Collection<Jid> userJids) {
        throw unsupported("getDeviceInfoForSync");
    }

    @Override
    public boolean hasDevice(Jid userJid, int deviceId) {
        throw unsupported("hasDevice");
    }

    @Override
    public DeviceList getMyDeviceList() {
        throw unsupported("getMyDeviceList");
    }

    @Override
    public Collection<DeviceList> getAllDeviceLists() {
        throw unsupported("getAllDeviceLists");
    }

    @Override
    public void handleICDCData(Jid senderJid, Jid recipientUserJid, DeviceListMetadata metadata) {
        throw unsupported("handleICDCData");
    }

    @Override
    public HostedIcdcResult handleHostedIcdcMetadataInline(Jid chatJid, Jid authorJid, DeviceListMetadata metadata) {
        throw unsupported("handleHostedIcdcMetadataInline");
    }

    @Override
    public void handleADVDeviceUpdateForMessage(Jid deviceJid, String rawId, long timestamp, int keyIndex, byte[] identityKey, ADVEncryptionType accountType) {
        throw unsupported("handleADVDeviceUpdateForMessage");
    }

    @Override
    public Optional<ADVSignedDeviceIdentity> extractAndValidateLocalSignedDeviceIdentity(Stanza deviceIdentityStanza) {
        throw unsupported("extractAndValidateLocalSignedDeviceIdentity");
    }

    @Override
    public void persistLocalDeviceIdentityFromPairSuccess(Jid deviceJid, byte[] accountSignatureKey) {
        throw unsupported("persistLocalDeviceIdentityFromPairSuccess");
    }

    /**
     * Builds the failure thrown by every unstubbed method, embedding the method name in the message
     * so the test report points at the missing stub.
     *
     * @param method the method name
     * @return the configured exception
     */
    private static UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException("StubDeviceService." + method + " is not stubbed");
    }
}
