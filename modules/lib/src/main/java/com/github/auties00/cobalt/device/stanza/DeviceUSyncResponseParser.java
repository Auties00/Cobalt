package com.github.auties00.cobalt.device.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.DeviceConstants;
import com.github.auties00.cobalt.device.DeviceListResult;
import com.github.auties00.cobalt.device.adv.DeviceADVValidator;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceInfo;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses USync IQ responses produced by the device-list synchronisation flow.
 *
 * <p>Used by {@link com.github.auties00.cobalt.device.DeviceService} as the sink for IQs sent through
 * {@link DeviceUSyncQueryBuilder}, this parser returns one {@link DeviceListResult} per user entry in
 * the response, mapping each one of the three wire shapes WhatsApp emits ({@link DeviceListResult.Full},
 * {@link DeviceListResult.Omitted}, {@link DeviceListResult.Error}) to a Cobalt-side record. The
 * collected results let the caller update its ADV cache or surface the failure. Signature verification
 * of the {@code <key-index-list>} payload is delegated to {@link DeviceADVValidator} so the crypto
 * stays out of the parser.
 *
 * @implNote
 * This implementation merges WA Web's {@code usyncParser}, the {@code WAWebUsyncDevice.deviceParser},
 * and the {@code WAWebHandleAdvForUsyncApi} / {@code WAWebHandleAdvKeyIndexResultApi} dispatch into a
 * single class because Cobalt only consumes the device and username sub-protocols; the JS dispatch
 * table for the other USync protocols (contact, picture, status, business, ...) is not modelled.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
@WhatsAppWebModule(moduleName = "WAWebUsyncDevice")
@WhatsAppWebModule(moduleName = "WAWebHandleAdvForUsyncApi")
@WhatsAppWebModule(moduleName = "WAWebUsyncUsername")
@WhatsAppWebModule(moduleName = "WAWebUsyncLid")
public final class DeviceUSyncResponseParser {
    /**
     * Holds the logger used to trace per-user and protocol-level parse warnings.
     */
    private static final System.Logger LOGGER = Log.get(DeviceUSyncResponseParser.class);

    /**
     * Holds the ADV validator used to verify {@code <key-index-list>} signatures and to read the
     * hosted-devices gating flag.
     */
    private final DeviceADVValidator advValidatorService;

    /**
     * Creates a new parser bound to a {@link DeviceADVValidator}.
     *
     * <p>A parser instance is constructed once per session and shared across all USync IQ responses
     * the device service handles; the supplied validator carries the per-account identity key material
     * that the {@code <key-index-list>} signature verification needs.
     *
     * @param advValidatorService the ADV validator backing
     *                            {@link #parseFullResult(Jid, String, Stanza, Stanza, byte[])} and the
     *                            hosted-devices gating used inside
     *                            {@link #parseDeviceEntry(Stanza, Map, SequencedSet)}
     * @throws NullPointerException if {@code advValidatorService} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvForUsyncApi",
            exports = "handleADVSyncResultSync",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public DeviceUSyncResponseParser(DeviceADVValidator advValidatorService) {
        this.advValidatorService = Objects.requireNonNull(advValidatorService, "advValidatorService cannot be null");
    }

    /**
     * Walks the response IQ and emits one {@link DeviceListResult} per user entry.
     *
     * <p>This is the top-level entry point of the parser. It returns an empty list when the response
     * has no {@code <usync>} envelope, a single-element list carrying a fatal
     * {@link DeviceListResult.Error} when a global {@code <error>} is present, or a per-user breakdown
     * otherwise. Protocol-level errors carried under {@code <usync><result><devices><error/>} are
     * surfaced as non-fatal {@link DeviceListResult.Error} entries with a {@code null} JID.
     *
     * @implNote
     * This implementation walks the tree using {@link Stanza}'s convenience stream accessors instead of
     * WA Web's {@code WADeprecatedWapParser} builder, but produces the same result classification.
     *
     * @param responseStanza the {@code <iq>} response received from the socket
     * @return the per-user and per-protocol classification of the response
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "usyncParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public List<DeviceListResult> parse(Stanza responseStanza) {
        var usyncNode = responseStanza.getChild("usync");
        if (usyncNode.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "usync response has no usync envelope");
            return List.of();
        }

        var usync = usyncNode.get();

        var globalErrorNode = usync.getChild("error");
        if (globalErrorNode.isPresent()) {
            var error = globalErrorNode.get();
            var errorCode = error.getAttributeAsInt("code", 0);
            var errorText = error.getAttributeAsString("text", "Unknown error");
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "usync global error: code={0}, text={1}", errorCode, errorText);
            }
            var errorResult = new DeviceListResult.Error(null, errorCode, errorText, true);
            return List.of(errorResult);
        }

        var usernameMap = parseUsernameMap(usync);

        var protocolErrorStream = usync.streamChild("result")
                .flatMap(result -> result.streamChild("devices"))
                .flatMap(devices -> devices.streamChild("error"))
                .map(error -> {
                    var errorCode = error.getAttributeAsInt("code", 0);
                    var errorText = error.getAttributeAsString("text", "Unknown error");
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "usync devices error: code={0}, text={1}", errorCode, errorText);
                    return (DeviceListResult) new DeviceListResult.Error(null, errorCode, errorText, false);
                });

        var listNode = usync.getChild("list");
        if (listNode.isEmpty()) {
            var results = protocolErrorStream.toList();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsed usync response: no list node, {0} protocol errors", results.size());
            return results;
        }

        var userResults = listNode.get().streamChildren("user")
                .flatMap(userNode -> parseUserDevices(userNode, usernameMap));
        var results = Stream.concat(protocolErrorStream, userResults)
                .toList();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsed usync response: {0} results", results.size());
        return results;
    }

    /**
     * Collects the phone-number-to-LID mappings carried by a USync LID-protocol response.
     *
     * <p>Walks every {@code <user>} entry under the {@code <usync><list>} envelope, reading the queried
     * phone-number JID from the {@code jid} attribute and the assigned LID from the {@code <lid val>}
     * child, mirroring WA Web's {@code WAWebUsyncLid.lidParser}. Entries whose {@code <lid>} child is
     * absent, carries an {@code <error/>}, or lacks a {@code val} attribute are skipped, so the returned
     * map holds one entry only for users the server actually resolved. The map is keyed by the
     * device-stripped phone-number JID and valued by the device-stripped LID.
     *
     * @implNote
     * This implementation reuses the same {@code <usync><list><user>} walk as {@link #parse(Stanza)} and
     * {@link #parseUsernameMap(Stanza)} so a LID query can be parsed without the device-protocol
     * machinery; it is consumed by the call-placement LID resolution in
     * {@link com.github.auties00.cobalt.device.DeviceService#queryUserLid(Jid)}.
     *
     * @param responseStanza the {@code <iq>} response received from the socket
     * @return the phone-number-to-LID map, or an empty map when no LID entries are present
     * @throws NullPointerException if {@code responseStanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "lidParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Map<Jid, Jid> parseLidMappings(Stanza responseStanza) {
        Objects.requireNonNull(responseStanza, "responseStanza cannot be null");
        var mappings = responseStanza.streamChild("usync")
                .flatMap(usync -> usync.streamChild("list"))
                .flatMap(list -> list.streamChildren("user"))
                .flatMap(this::parseLidEntry)
                .collect(Collectors.toUnmodifiableMap(LidEntry::phoneJid, LidEntry::lid, (first, _) -> first));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsed {0} lid mappings from usync response", mappings.size());
        return mappings;
    }

    /**
     * Extracts a {@code (phoneJid, lid)} pair from a single {@code <user><lid val="..."/></user>} entry.
     *
     * <p>This is the inner worker for {@link #parseLidMappings(Stanza)}; it emits nothing when the
     * {@code jid} attribute is missing, the {@code <lid>} child is absent or carries an {@code <error/>},
     * or the {@code val} attribute is missing or not a valid LID JID.
     *
     * @param userStanza the {@code <user>} entry
     * @return a stream carrying the parsed entry, or empty when the LID could not be read
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "lidParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stream<LidEntry> parseLidEntry(Stanza userStanza) {
        var jid = userStanza.getAttributeAsJid("jid");
        if (jid.isEmpty()) {
            return Stream.empty();
        }

        var lidNode = userStanza.getChild("lid");
        if (lidNode.isEmpty()) {
            return Stream.empty();
        }

        var node = lidNode.get();
        if (node.getChild("error").isPresent()) {
            return Stream.empty();
        }

        var lid = node.getAttributeAsJid("val");
        if (lid.isEmpty()) {
            return Stream.empty();
        }

        return Stream.of(new LidEntry(jid.get().toUserJid(), lid.get().toUserJid()));
    }

    /**
     * Collects the per-user username mappings from the response.
     *
     * <p>This is the inner worker for {@link #parse(Stanza)}; the resulting map is later passed into
     * {@link #parseUserDevices(Stanza, Map)} so a parsed username can be folded into the
     * {@link DeviceListResult.Full} record without a second walk of the list.
     *
     * @implNote
     * This implementation skips username entries that carry an {@code <error/>} child or whose content
     * string is empty, matching WA Web's {@code WAWebUsyncUsername.usernameParser} which returns the
     * error object or {@code null} respectively; Cobalt has no downstream consumer for those values.
     *
     * @param usync the {@code <usync>} envelope
     * @return the user-JID to username map, or an empty map when no usernames are present
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUsername",
            exports = "usernameParser",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Map<Jid, String> parseUsernameMap(Stanza usync) {
        return usync.streamChild("list")
                .flatMap(list -> list.streamChildren("user"))
                .flatMap(this::parseUsernameEntry)
                .collect(Collectors.toUnmodifiableMap(UsernameEntry::userJid, UsernameEntry::username));
    }

    /**
     * Classifies a single {@code <user>} entry into Full, Omitted, or Error.
     *
     * <p>This is the inner worker for {@link #parse(Stanza)}. It returns an empty stream when the entry
     * has no {@code jid} attribute or no {@code <devices>} child, a single
     * {@link DeviceListResult.Error} when {@code <devices>} carries an {@code <error/>}, and otherwise
     * delegates to either {@link #parseOmittedResult(Jid, Stanza, Stanza)} or
     * {@link #parseFullResult(Jid, String, Stanza, Stanza, byte[])} depending on whether the
     * {@code <key-index-list>} payload is present.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code WAWebHandleAdvForUsyncApi.handleADVSyncResultSync}
     * dispatch, including the rule that an absent signed-key-index payload routes to the Omitted path
     * even when a {@code <device-list>} is present.
     *
     * @param userStanza    the {@code <user>} entry stanza
     * @param usernameMap the username map produced by {@link #parseUsernameMap(Stanza)}
     * @return the per-user classification
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
            exports = "deviceParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stream<DeviceListResult> parseUserDevices(Stanza userStanza, Map<Jid, String> usernameMap) {
        var jidAttr = userStanza.getAttributeAsJid("jid");
        if (jidAttr.isEmpty()) {
            return Stream.empty();
        }

        var userJid = jidAttr.get().toUserJid();

        var devicesNode = userStanza.getChild("devices");
        if (devicesNode.isEmpty()) {
            return Stream.empty();
        }

        var devices = devicesNode.get();

        var errorChild = devices.getChild("error");
        if (errorChild.isPresent()) {
            var error = errorChild.get();
            var errorCode = error.getAttributeAsInt("code", 0);
            var errorText = error.getAttributeAsString("text", "Unknown error");
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "usync user devices error for {0}: code={1}, text={2}", userJid, errorCode, errorText);
            }
            return Stream.of(new DeviceListResult.Error(userJid, errorCode, errorText, false));
        }

        var keyIndexListNode = devices.getChild("key-index-list", null);
        var deviceListNode = devices.getChild("device-list").orElse(null);

        var signedKeyIndexBytes = keyIndexListNode != null
                ? keyIndexListNode.toContentBytes().orElse(null)
                : null;

        if (signedKeyIndexBytes == null) {
            return parseOmittedResult(userJid, deviceListNode, keyIndexListNode);
        }

        if (deviceListNode == null) {
            return Stream.empty();
        }

        var username = usernameMap.get(userJid);
        return parseFullResult(userJid, username, deviceListNode, keyIndexListNode, signedKeyIndexBytes);
    }

    /**
     * Builds a {@link DeviceListResult.Omitted} record for a cached-list confirmation.
     *
     * <p>This path is reached when the server skipped resending the device list because the cached
     * {@code device_hash} still matches. The timestamps are read from the {@code ts} and
     * {@code expected_ts} attributes of the {@code <key-index-list>} stanza when present, and downstream
     * consumers treat the Omitted record as a no-op for the cache while using those timestamps to
     * refresh the expected-timestamp tracking fields.
     *
     * @implNote
     * This implementation guards against a malformed wire shape where a {@code <device-list>} carrying
     * companion devices arrives without a signed key index; that combination is dropped because the
     * companions would otherwise be accepted without a verified key, matching WA Web's
     * {@code WAWebHandleAdvForUsyncApi.handleADVSyncResultSync}.
     *
     * @param userJid          the user JID
     * @param deviceListStanza   the {@code <device-list>} stanza, or {@code null}
     * @param keyIndexListStanza the {@code <key-index-list>} stanza, or {@code null}
     * @return a stream carrying the Omitted record, or empty if the wire shape was invalid
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvForUsyncApi",
            exports = "handleADVSyncResultSync",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stream<DeviceListResult> parseOmittedResult(Jid userJid, Stanza deviceListStanza, Stanza keyIndexListStanza) {
        if (deviceListStanza != null && hasCompanionDevices(deviceListStanza)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "dropping omitted usync result for {0}: companion devices without signed key index", userJid);
            }
            return Stream.empty();
        }

        if (keyIndexListStanza == null) {
            return Stream.of(new DeviceListResult.Omitted(userJid, null, null, true));
        }

        var ts = keyIndexListStanza.getAttributeAsLong("ts", null);
        var timestamp = ts != null ? Instant.ofEpochSecond(ts) : null;

        var expTs = keyIndexListStanza.getAttributeAsLong("expected_ts", null);
        var expectedTs = expTs != null ? Instant.ofEpochSecond(expTs) : null;

        return Stream.of(new DeviceListResult.Omitted(userJid, timestamp, expectedTs, true));
    }

    /**
     * Tests whether a {@code <device-list>} mentions at least one companion device.
     *
     * <p>This predicate is used by {@link #parseOmittedResult(Jid, Stanza, Stanza)} to reject the wire
     * shape that advertises companion devices without a signed key index.
     *
     * @param deviceListStanza the {@code <device-list>} stanza
     * @return {@code true} when any {@code <device>} child carries an {@code id} attribute different
     *         from {@link DeviceConstants#PRIMARY_DEVICE_ID}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvForUsyncApi",
            exports = "handleADVSyncResultSync",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean hasCompanionDevices(Stanza deviceListStanza) {
        return deviceListStanza.streamChildren("device")
                .anyMatch(device -> !device.hasAttribute("id", DeviceConstants.PRIMARY_DEVICE_ID));
    }

    /**
     * Builds the {@link DeviceListResult.Full} record after verifying the signed key index list.
     *
     * <p>This path is reached when the server resent the full device list. It returns an empty stream
     * when the {@code <key-index-list>} signature does not verify, so the caller drops the user instead
     * of poisoning its ADV cache. The hosted-business gating selects between
     * {@link DeviceADVValidator#verifySKeyIndexWithAccSigKey(byte[])} (when the list advertises at
     * least one hosted device and the AB prop is on) and
     * {@link DeviceADVValidator#decodeSignedKeyIndexBytes(Jid, byte[])} (the standard path keyed by the
     * user's stored primary identity). On success it assembles the verified device set, key indexes,
     * and timestamps into a {@link DeviceListBuilder} result.
     *
     * @implNote
     * This implementation follows the verification path of WA Web's
     * {@code WAWebHandleAdvKeyIndexResultApi.handleKeyIndexResultSync}; it skips the JS function's
     * downstream clear-record computation because that step belongs to the consumer, mirrored in
     * {@link com.github.auties00.cobalt.device.DeviceService}.
     *
     * @param userJid             the user JID
     * @param username            the parsed username, or {@code null}
     * @param deviceListStanza      the {@code <device-list>} stanza
     * @param keyIndexListStanza    the {@code <key-index-list>} stanza
     * @param signedKeyIndexBytes the raw signed-key-index payload to verify
     * @return a stream carrying the Full record, or empty if signature verification failed
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvKeyIndexResultApi",
            exports = "handleKeyIndexResultSync",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stream<DeviceListResult> parseFullResult(
            Jid userJid,
            String username,
            Stanza deviceListStanza,
            Stanza keyIndexListStanza,
            byte[] signedKeyIndexBytes
    ) {
        var expectedTsSeconds = keyIndexListStanza.getAttributeAsLong("expected_ts", 0);
        var expectedTs = expectedTsSeconds != 0  ? Instant.ofEpochSecond(expectedTsSeconds) : null;

        var useHostedPath = advValidatorService.isBizHostedDevicesEnabled()
                && hasHostedDeviceAttribute(deviceListStanza);
        var validatedInfo = useHostedPath
                ? advValidatorService.verifySKeyIndexWithAccSigKey(signedKeyIndexBytes)
                : advValidatorService.decodeSignedKeyIndexBytes(userJid, signedKeyIndexBytes);
        if (validatedInfo.isEmpty()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "key index list signature verification failed for {0}, skipping", userJid);
            }
            return Stream.empty();
        }

        var info = validatedInfo.get();

        var keyIndexMap = keyIndexListStanza.streamChildren("device")
                .flatMap(this::parseKeyIndexEntry)
                .collect(Collectors.toUnmodifiableMap(KeyIndexEntry::deviceId, KeyIndexEntry::keyIndex));

        var devices = deviceListStanza.streamChildren("device")
                .flatMap(deviceNode -> parseDeviceEntry(deviceNode, keyIndexMap, info.validIndexes()))
                .toList();

        var deviceList = new DeviceListBuilder()
                .userJid(userJid)
                .devices(devices)
                .timestamp(info.timestamp())
                .rawId(String.valueOf(info.rawId()))
                .advAccountType(info.accountType())
                .expectedTimestamp(expectedTs)
                .currentIndex(info.currentIndex())
                .validIndexes(info.validIndexes())
                .build();

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "usync full device list parsed for {0}: {1} devices", userJid, devices.size());
        return Stream.of(new DeviceListResult.Full(deviceList, info.accountSignatureKey().orElse(null), username));
    }

    /**
     * Tests whether a {@code <device-list>} advertises at least one device with
     * {@code is_hosted="true"}.
     *
     * <p>This gating predicate is used by {@link #parseFullResult(Jid, String, Stanza, Stanza, byte[])} to
     * choose between the hosted-business signature path and the standard primary-identity path.
     *
     * @param deviceListStanza the {@code <device-list>} stanza
     * @return {@code true} if any {@code <device>} child carries the {@code is_hosted="true"}
     *         attribute
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvKeyIndexResultApi",
            exports = "handleKeyIndexResultSync",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean hasHostedDeviceAttribute(Stanza deviceListStanza) {
        return deviceListStanza.streamChildren("device")
                .anyMatch(device -> device.hasAttribute("is_hosted", true));
    }

    /**
     * Extracts a {@code (deviceId, keyIndex)} pair from a single {@code <key-index-list><device/>}
     * entry.
     *
     * <p>This is the inner worker for {@link #parseFullResult(Jid, String, Stanza, Stanza, byte[])}; the
     * resulting map is consulted by {@link #parseDeviceEntry(Stanza, Map, SequencedSet)} to attach the
     * correct key index to each device in the device-list.
     *
     * @implNote
     * This implementation drops entries missing either {@code jid} or {@code key-index}, matching the
     * JS code which silently ignores malformed nodes.
     *
     * @param deviceStanza the {@code <device>} child from the key-index-list
     * @return a stream carrying the parsed entry, or empty if attributes are missing
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvKeyIndexResultApi",
            exports = "handleKeyIndexResultSync",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stream<KeyIndexEntry> parseKeyIndexEntry(Stanza deviceStanza) {
        var jid = deviceStanza.getAttributeAsJid("jid");
        var keyIndex = deviceStanza.getAttributeAsInt("key-index");
        if (jid.isEmpty() || keyIndex.isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(new KeyIndexEntry(jid.get().device(), keyIndex.getAsInt()));
    }

    /**
     * Converts a single {@code <device-list><device/>} entry to a typed {@link DeviceInfo}.
     *
     * <p>This is the inner worker for {@link #parseFullResult(Jid, String, Stanza, Stanza, byte[])}; it
     * produces either a regular E2EE device or a hosted device when the AB prop is on and the entry
     * advertises {@code is_hosted="true"} with id {@link DeviceConstants#HOSTED_DEVICE_ID}.
     *
     * @implNote
     * This implementation skips devices whose key index is non-zero and not present in the
     * cryptographically signed {@code validIndexes} set, so the parser only emits devices the signed
     * key index list authorises. The primary device (key index 0) is always accepted, and an empty
     * {@code validIndexes} set disables the check entirely; both rules match WA Web's
     * {@code handleKeyIndexResultSync}.
     *
     * @param deviceStanza   the {@code <device>} stanza from the device-list
     * @param keyIndexMap  the {@code (deviceId, keyIndex)} map produced by
     *                     {@link #parseKeyIndexEntry(Stanza)}
     * @param validIndexes the cryptographically signed set of valid key indexes, or {@code null} when
     *                     unavailable
     * @return a stream carrying the parsed {@link DeviceInfo}, or empty when the device fails
     *         validation
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
            exports = "deviceParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stream<DeviceInfo> parseDeviceEntry(
            Stanza deviceStanza,
            Map<Integer, Integer> keyIndexMap,
            SequencedSet<Integer> validIndexes
    ) {
        var id = deviceStanza.getAttributeAsInt("id");
        if (id.isEmpty()) {
            return Stream.empty();
        }

        var deviceId = id.getAsInt();
        var keyIndex = keyIndexMap.getOrDefault(deviceId, 0);

        if (validIndexes != null && !validIndexes.isEmpty() && keyIndex != 0 && !validIndexes.contains(keyIndex)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "device {0} has keyIndex {1} not in validIndexes {2}, excluding", deviceId, keyIndex, validIndexes);
            }
            return Stream.empty();
        }

        var bizHostedDevicesEnabled = advValidatorService.isBizHostedDevicesEnabled();
        var isHosted = bizHostedDevicesEnabled
                && "true".equals(deviceStanza.getAttributeAsString("is_hosted").orElse(null));

        if (deviceId == DeviceConstants.HOSTED_DEVICE_ID && bizHostedDevicesEnabled) {
            return Stream.of(DeviceInfo.ofHosted(keyIndex));
        } else {
            return Stream.of(DeviceInfo.ofE2EE(deviceId, keyIndex, isHosted));
        }
    }

    /**
     * Extracts a {@code (userJid, username)} pair from a single {@code <user><username/></user>} entry.
     *
     * <p>This is the inner worker for {@link #parseUsernameMap(Stanza)}; it emits nothing for entries
     * whose username carries an error child or whose content is empty.
     *
     * @param userStanza the {@code <user>} entry
     * @return a stream carrying the parsed entry, or empty when the {@code <username>} child is absent,
     *         errored, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUsername",
            exports = "usernameParser",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Stream<UsernameEntry> parseUsernameEntry(Stanza userStanza) {
        var jid = userStanza.getAttributeAsJid("jid");
        if (jid.isEmpty()) {
            return Stream.empty();
        }

        var usernameNode = userStanza.getChild("username");
        if (usernameNode.isEmpty()) {
            return Stream.empty();
        }

        var node = usernameNode.get();

        if (node.getChild("error").isPresent()) {
            return Stream.empty();
        }

        var usernameContent = node.toContentString();
        if (usernameContent.isEmpty() || usernameContent.get().isEmpty()) {
            return Stream.empty();
        }

        return Stream.of(new UsernameEntry(jid.get().toUserJid(), usernameContent.get()));
    }

    /**
     * Carries a parsed {@code (deviceId, keyIndex)} pair.
     *
     * <p>This is the value type of the {@link Stream} produced by {@link #parseKeyIndexEntry(Stanza)}
     * before being collected into the {@code deviceId} to {@code keyIndex} map.
     *
     * @param deviceId the wire-level device id
     * @param keyIndex the wire-level key index
     */
    private record KeyIndexEntry(int deviceId, int keyIndex) {}

    /**
     * Carries a parsed {@code (userJid, username)} pair.
     *
     * <p>This is the value type of the {@link Stream} produced by {@link #parseUsernameEntry(Stanza)}
     * before being collected into the username map.
     *
     * @param userJid  the user JID
     * @param username the username string
     */
    private record UsernameEntry(Jid userJid, String username) {}

    /**
     * Carries a parsed {@code (phoneJid, lid)} pair.
     *
     * <p>This is the value type of the {@link Stream} produced by {@link #parseLidEntry(Stanza)} before
     * being collected into the phone-number-to-LID map.
     *
     * @param phoneJid the queried phone-number user JID
     * @param lid      the assigned LID user JID
     */
    private record LidEntry(Jid phoneJid, Jid lid) {}
}
