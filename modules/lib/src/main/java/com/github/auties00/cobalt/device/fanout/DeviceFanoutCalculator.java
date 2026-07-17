package com.github.auties00.cobalt.device.fanout;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;

import java.lang.System.Logger.Level;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves the set of recipient device {@link Jid}s for a given outgoing WhatsApp send.
 *
 * <p>Given the already-resolved {@link DeviceList} for each addressee, the calculator drops the
 * sender's own device, applies the hosted-device gating rule used for business coexistence, falls
 * back to the primary user {@link Jid} when a device list is empty, and filters out devices whose
 * Signal identity key has rotated without the user acknowledging the change. It backs the
 * per-send recipient selection exposed by
 * {@link com.github.auties00.cobalt.device.DeviceService#getUserFanout(Jid, String)} and
 * {@link com.github.auties00.cobalt.device.DeviceService#getGroupFanout(Jid)}; production
 * senders reach it through {@link com.github.auties00.cobalt.device.DeviceService} and call it
 * directly only when bypassing that service, for example in tests.
 */
@WhatsAppWebModule(moduleName = "WAWebDBDeviceListFanout")
public final class DeviceFanoutCalculator {

    /**
     * The logger for {@link DeviceFanoutCalculator}.
     */
    private static final System.Logger LOGGER = Log.get(DeviceFanoutCalculator.class);

    /**
     * The {@link ABPropsService} consulted for the hosted-device gating flag.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a calculator bound to the given AB props service.
     *
     * <p>The same instance is wired by {@link com.github.auties00.cobalt.device.DeviceService} and
     * shared across all sends from a single client.
     *
     * @param abPropsService the AB props service used by {@link #isBizHostedDevicesEnabled()}
     * @throws NullPointerException if {@code abPropsService} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebDBDeviceListFanout",
            exports = "getFanOutList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public DeviceFanoutCalculator(ABPropsService abPropsService) {
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
    }

    /**
     * Computes the fanout {@link Jid} set for the given resolved device lists.
     *
     * <p>Every device entry across {@code deviceLists} becomes a recipient, except the sender's
     * own PN and LID device JIDs, which are always omitted. Hosted device entries (those reporting
     * {@code isHosted}) are skipped unless {@code includeHostedForOneToOneChatJid} is a user JID
     * for which {@link #isBizHostedDevicesEnabled()} is on; a 1:1 send passes the chat JID there to
     * opt that peer's hosted devices in, while a group send passes {@code null} so hosted devices
     * are never included. When a user has no resolved device list, the primary user JID is added in
     * its place unless it belongs to the sender's own account (PN or LID side). At most three such
     * fallback JIDs are logged at {@link System.Logger.Level#DEBUG} through the
     * {@code [getFanOutList] no device for ...} message. Resolving the {@link DeviceList} entries
     * themselves happens upstream in {@link com.github.auties00.cobalt.device.DeviceService}; this
     * method is purely a filter over the already-resolved lists.
     *
     * @param senderPnDeviceJid               the sender's PN device JID, or {@code null} if not signed in via PN
     * @param senderLidDeviceJid              the sender's LID device JID, or {@code null} if not signed in via LID
     * @param deviceLists                     the per-user resolved device lists to fan out across
     * @param includeHostedForOneToOneChatJid the chat JID of the targeted 1:1 conversation when hosted devices may participate, or {@code null} for group sends
     * @return an unmodifiable set of recipient device JIDs
     */
    @WhatsAppWebExport(moduleName = "WAWebDBDeviceListFanout",
            exports = "getFanOutList",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Set<Jid> calculate(
            Jid senderPnDeviceJid,
            Jid senderLidDeviceJid,
            Set<DeviceList> deviceLists,
            Jid includeHostedForOneToOneChatJid
    ) {
        var results = new HashSet<Jid>();
        var includeHosted = isBizHostedDevicesEnabled()
                && includeHostedForOneToOneChatJid != null
                && isUserJid(includeHostedForOneToOneChatJid);
        var fallbackWids = new ArrayList<String>();

        for (var deviceList : deviceLists) {
            var userJid = deviceList.userJid();

            if (deviceList.devices().isEmpty()) {
                var primaryJid = userJid.toUserJid();
                if (fallbackWids.size() < 3) {
                    fallbackWids.add(primaryJid.toString());
                }
                if (!isMeAccount(primaryJid, senderPnDeviceJid, senderLidDeviceJid)) {
                    results.add(primaryJid);
                }
                continue;
            }

            for (var device : deviceList.devices()) {
                if (device.isHosted() && !includeHosted) {
                    continue;
                }
                var deviceJid = device.toDeviceJid(userJid.user(), userJid.server());
                if (isMeDevice(deviceJid, senderPnDeviceJid, senderLidDeviceJid)) {
                    continue;
                }
                results.add(deviceJid);
            }
        }

        if (Log.DEBUG && !fallbackWids.isEmpty()) {
            var redactedWids = fallbackWids.stream().map(LogRedactable.User::new).toList();
            LOGGER.log(Level.DEBUG,
                    "[getFanOutList] no device for {0} wids => primary {1}",
                    fallbackWids.size(),
                    redactedWids);
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "computed fanout of {0} devices from {1} user lists, includeHosted={2}",
                    results.size(), deviceLists.size(), includeHosted);
        }

        return Collections.unmodifiableSet(results);
    }

    /**
     * Returns whether hosted devices may be included in a 1:1 fanout.
     *
     * <p>Gates the inclusion of {@code isHosted} device records (and the legacy {@code id == 99}
     * hosted device id) for business-coexistence sends. The flag is off by default and is flipped
     * on by the {@link ABProp#ADV_ACCEPT_HOSTED_DEVICES} AB prop.
     *
     * @return {@code true} when {@link ABProp#ADV_ACCEPT_HOSTED_DEVICES} is set
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCoexGatingUtils",
            exports = "bizHostedDevicesEnabled",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean isBizHostedDevicesEnabled() {
        return abPropsService.getBool(ABProp.ADV_ACCEPT_HOSTED_DEVICES);
    }

    /**
     * Returns whether the given {@link Jid} addresses an individual user account.
     *
     * <p>Used by {@link #calculate} to decide whether the 1:1 chat target is a user and so eligible
     * for hosted-device inclusion. A JID counts as a user when its server is one of the
     * user-shaped servers: {@code c.us}, {@code lid}, {@code bot}, {@code hosted}, or
     * {@code hosted.lid}.
     *
     * @param jid the JID under test
     * @return {@code true} when the JID's server denotes one of the user-shaped servers
     */
    @WhatsAppWebExport(moduleName = "WAWebWid",
            exports = "isUser",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isUserJid(Jid jid) {
        return jid.hasUserServer()
                || jid.hasLidServer()
                || jid.hasBotServer()
                || jid.hasHostedServer()
                || jid.hasHostedLidServer();
    }

    /**
     * Returns whether the candidate device {@link Jid} is one of the sender's own devices.
     *
     * <p>Used by {@link #calculate} to drop self-addressed entries before fanning out. The result
     * is {@code true} when the candidate equals either the sender's PN device JID or the sender's
     * LID device JID; a {@code null} sender JID compares unequal.
     *
     * @param candidate          the device JID under test
     * @param senderPnDeviceJid  the sender's PN device JID, or {@code null}
     * @param senderLidDeviceJid the sender's LID device JID, or {@code null}
     * @return {@code true} when the candidate equals either sender device JID
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMeUser",
            exports = "isMeDevice",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isMeDevice(Jid candidate, Jid senderPnDeviceJid, Jid senderLidDeviceJid) {
        return Objects.equals(candidate, senderPnDeviceJid)
                || Objects.equals(candidate, senderLidDeviceJid);
    }

    /**
     * Returns whether the candidate user-level {@link Jid} belongs to the sender's own account.
     *
     * <p>Used by the primary-fallback branch of {@link #calculate} to avoid fanning out to the
     * sender's own bare user JID when the resolved {@link DeviceList} for that user is empty. The
     * candidate is normalised through {@link Jid#toUserJid()} and compared against the normalised
     * user form of each sender device JID, so {@code hosted} maps to {@code c.us} and
     * {@code hosted.lid} maps to {@code lid} before equality. A {@code null} candidate returns
     * {@code false}.
     *
     * @param candidate          the user-level JID under test
     * @param senderPnDeviceJid  the sender's PN device JID, or {@code null}
     * @param senderLidDeviceJid the sender's LID device JID, or {@code null}
     * @return {@code true} when the candidate normalises to either sender account
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMeUser",
            exports = "isMeAccount",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isMeAccount(Jid candidate, Jid senderPnDeviceJid, Jid senderLidDeviceJid) {
        if (candidate == null) {
            return false;
        }
        var candidateUser = candidate.toUserJid();
        if (senderPnDeviceJid != null
                && candidateUser.equals(senderPnDeviceJid.toUserJid())) {
            return true;
        }
        return senderLidDeviceJid != null
                && candidateUser.equals(senderLidDeviceJid.toUserJid());
    }

    /**
     * Removes from the given fanout the devices whose identity key has rotated without
     * acknowledgement.
     *
     * <p>Called on the resend path to honour the rule that Cobalt never silently re-encrypts to a
     * peer whose Signal identity key has changed since the last verified state. When
     * {@code changedIdentities} is empty the input set is returned unchanged; otherwise a fresh
     * unmodifiable set is built containing only the {@code devices} entries not present in
     * {@code changedIdentities}.
     *
     * @param devices           the candidate device JIDs
     * @param changedIdentities the device JIDs flagged as having a pending identity change
     * @return the {@code devices} subset whose identity key has not been flagged
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi",
            exports = "filterDeviceWithChangedIdentity",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Set<Jid> filterIdentityChanges(Set<Jid> devices, Set<Jid> changedIdentities) {
        if (changedIdentities.isEmpty()) {
            return devices;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "filtering {0} changed identities out of {1} fanout devices",
                    changedIdentities.size(), devices.size());
        }

        return devices.stream()
                .filter(jid -> !changedIdentities.contains(jid))
                .collect(Collectors.toUnmodifiableSet());
    }
}
