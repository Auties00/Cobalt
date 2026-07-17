package com.github.auties00.cobalt.device.fanout;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Computes the participant hash ({@code phash}) string that group-message stanzas carry so the
 * server and the sender can confirm they share the same view of the group's device membership.
 *
 * <p>The output of {@link #calculate} goes on the outgoing {@code <message phash="...">} stanza
 * attribute for group sends and on the {@code <message>} stanzas of broadcast and status messages.
 * The server compares it to its own hash of the participant device JIDs and rejects the stanza, or
 * instructs the client to resync, on mismatch. Both the legacy SHA-1 V1 format and the current
 * SHA-256 V2 format are supported via {@link DevicePhashVersion}; V2 additionally injects the Meta
 * AI bot account JIDs when the corresponding AB-prop gates are on.
 * {@link com.github.auties00.cobalt.device.DeviceService} also calls this calculator to
 * short-circuit a device-list sync when the server pre-announces an expected {@code phash}.
 */
@WhatsAppWebModule(moduleName = "WAWebPhashUtils")
public final class DevicePhashCalculator {

    /**
     * The logger for {@link DevicePhashCalculator}.
     */
    private static final System.Logger LOGGER = Log.get(DevicePhashCalculator.class);

    /**
     * Number of leading digest bytes retained before base64 encoding.
     *
     * @implNote
     * This implementation keeps six bytes because they encode to exactly eight base64 characters
     * with no padding ({@code 6 % 3 == 0}); the version prefix is prepended afterwards. The value
     * matches the {@code slice(0, 6)} applied to the raw digest in WA Web's phash exports.
     */
    @WhatsAppWebExport(moduleName = "WAWebPhashUtils",
            exports = {"phashV1", "phashV2"},
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int HASH_BYTES_TO_USE = 6;

    /**
     * The Meta AI TEE (Trusted Execution Environment) bot account JID.
     *
     * <p>This account is eligible for V2 phash injection when the TEE gating flag is on, as
     * decided by {@link #isTEEGroupBotParticipantAddEnabled()}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotUtils",
            exports = "META_BOT_TEE_FBID_WID",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final Jid META_AI_TEE_BOT_ACCOUNT = new Jid("1273596044787272", JidServer.bot());

    /**
     * The {@link ABPropsService} consulted by the Meta AI bot gating predicates.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a calculator bound to the given AB props service.
     *
     * <p>The same instance is wired by {@link com.github.auties00.cobalt.device.DeviceService} and
     * shared across all group sends from a single client.
     *
     * @param abPropsService the AB props service used by
     *                       {@link #isOpenGroupBotParticipantAddEnabled()} and
     *                       {@link #isTEEGroupBotParticipantAddEnabled()}
     * @throws NullPointerException if {@code abPropsService} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebPhashUtils",
            exports = {"phashV1", "phashV2"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    public DevicePhashCalculator(ABPropsService abPropsService) {
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
    }

    /**
     * Computes the {@code phash} for the given device JIDs without the TEE bot injection branch.
     *
     * <p>This overload is equivalent to
     * {@link #calculate(Set, DevicePhashVersion, boolean, boolean) calculate(deviceJids, version, allowIncludeOpenBot, false)},
     * fixing the TEE bot flag to {@code false}. It serves send paths that may add the open group
     * bot but never the TEE bot.
     *
     * @param deviceJids          the device JID set to hash
     * @param version             the {@link DevicePhashVersion} selecting algorithm, prefix, and bot-injection eligibility
     * @param allowIncludeOpenBot whether the open Meta AI group bot may be injected when V2 and {@link #isOpenGroupBotParticipantAddEnabled()} are both true
     * @return the encoded {@code phash} (prefix plus 8-char base64 of the first 6 digest bytes)
     * @throws NoSuchAlgorithmException if the JRE does not provide {@link DevicePhashVersion#algorithm()}
     */
    @WhatsAppWebExport(moduleName = "WAWebPhashUtils",
            exports = {"phashV1", "phashV2"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    public String calculate(
            Set<Jid> deviceJids,
            DevicePhashVersion version,
            boolean allowIncludeOpenBot
    ) throws NoSuchAlgorithmException {
        return calculate(deviceJids, version, allowIncludeOpenBot, false);
    }

    /**
     * Computes the {@code phash} for the given device JIDs.
     *
     * <p>V1 selects SHA-1 with the {@code "1:"} prefix for legacy interop; V2 selects SHA-256 with
     * the {@code "2:"} prefix for current group sends. The two boolean flags interact with the
     * AB-prop gates: the open Meta AI bot JID is added only when {@code allowIncludeOpenBot},
     * {@link DevicePhashVersion#supportsMetaBot()}, and
     * {@link #isOpenGroupBotParticipantAddEnabled()} are all true; the TEE Meta AI bot JID is added
     * only when {@code allowIncludeTeeBot}, {@link DevicePhashVersion#supportsMetaBot()}, and
     * {@link #isTEEGroupBotParticipantAddEnabled()} are all true. V1 never injects either bot
     * because {@link DevicePhashVersion#supportsMetaBot()} is {@code false} for it. A sample call:
     * {@snippet :
     *     var phash = calculator.calculate(deviceJids, DevicePhashVersion.V2, true, false);
     *     stanzaBuilder.attribute("phash", phash);
     * }
     *
     * @implNote
     * This implementation serialises every JID through {@link #toLegacyJidString} into its V1 or V2
     * legacy string, sorts the strings via {@link Comparator#naturalOrder()}, feeds each string's
     * UTF-8 bytes into the digest, keeps the first {@value #HASH_BYTES_TO_USE} bytes of the digest,
     * base64-encodes them, and prepends {@link DevicePhashVersion#prefix()}.
     *
     * @param deviceJids          the device JID set to hash
     * @param version             the {@link DevicePhashVersion} selecting algorithm, prefix, and bot-injection eligibility
     * @param allowIncludeOpenBot whether the open Meta AI group bot may be injected (V2 only)
     * @param allowIncludeTeeBot  whether the TEE Meta AI group bot may be injected (V2 only)
     * @return the encoded {@code phash} (prefix plus 8-char base64 of the first 6 digest bytes)
     * @throws NoSuchAlgorithmException if the JRE does not provide {@link DevicePhashVersion#algorithm()}
     */
    @WhatsAppWebExport(moduleName = "WAWebPhashUtils",
            exports = {"phashV1", "phashV2"},
            adaptation = WhatsAppAdaptation.DIRECT)
    public String calculate(
            Set<Jid> deviceJids,
            DevicePhashVersion version,
            boolean allowIncludeOpenBot,
            boolean allowIncludeTeeBot
    ) throws NoSuchAlgorithmException {
        var jidsToHash = new ArrayList<>(deviceJids);

        if (allowIncludeOpenBot && version.supportsMetaBot() && isOpenGroupBotParticipantAddEnabled()) {
            jidsToHash.add(Jid.metaAiBotAccount());
        }

        if (allowIncludeTeeBot && version.supportsMetaBot() && isTEEGroupBotParticipantAddEnabled()) {
            jidsToHash.add(META_AI_TEE_BOT_ACCOUNT);
        }

        var legacyJids = jidsToHash.stream()
                .map(jid -> toLegacyJidString(jid, version))
                .sorted(Comparator.naturalOrder())
                .toList();

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "[{0}] calculating phash for {1} jids", version.prefix(), legacyJids.size());
        }

        var digest = MessageDigest.getInstance(version.algorithm());
        for (var legacyJid : legacyJids) {
            digest.update(legacyJid.getBytes(StandardCharsets.UTF_8));
        }
        var hash = digest.digest();

        var truncated = new byte[HASH_BYTES_TO_USE];
        System.arraycopy(hash, 0, truncated, 0, HASH_BYTES_TO_USE);

        var base64 = Base64.getEncoder().encodeToString(truncated);
        return version.prefix() + base64;
    }

    /**
     * Returns whether the open Meta AI group bot may be added to the V2 hashed set.
     *
     * <p>Gates the conditional inclusion of the open Meta AI bot JID during V2 phash computation.
     *
     * @implNote
     * This implementation requires both {@link ABProp#WEB_AI_GROUP_OPEN_SUPPORT} and
     * {@link ABProp#AI_GROUP_PARTICIPATION_ENABLED} to be set; either being unset returns
     * {@code false}.
     *
     * @return {@code true} when both gating AB props are set
     */
    @WhatsAppWebExport(moduleName = "WAWebBotGroupGatingUtils",
            exports = "isOpenGroupBotParticipantAddEnabled",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isOpenGroupBotParticipantAddEnabled() {
        var webAiGroupOpenSupport = abPropsService.getBool(ABProp.WEB_AI_GROUP_OPEN_SUPPORT);
        var aiGroupParticipationEnabled = abPropsService.getBool(ABProp.AI_GROUP_PARTICIPATION_ENABLED);
        return webAiGroupOpenSupport && aiGroupParticipationEnabled;
    }

    /**
     * Returns whether the TEE Meta AI group bot may be added to the V2 hashed set.
     *
     * <p>Gates the conditional inclusion of the TEE Meta AI bot JID during V2 phash computation.
     *
     * @implNote
     * This implementation requires both {@link ABProp#WEB_AI_GROUP_OPEN_SUPPORT} and
     * {@link ABProp#AI_GROUP_PARTICIPATION_ADD_TEE_ENABLED} to be set; either being unset returns
     * {@code false}.
     *
     * @return {@code true} when both gating AB props are set
     */
    @WhatsAppWebExport(moduleName = "WAWebBotGroupGatingUtils",
            exports = "isTEEGroupBotParticipantAddEnabled",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isTEEGroupBotParticipantAddEnabled() {
        var webAiGroupOpenSupport = abPropsService.getBool(ABProp.WEB_AI_GROUP_OPEN_SUPPORT);
        var aiGroupParticipationAddTeeEnabled = abPropsService.getBool(ABProp.AI_GROUP_PARTICIPATION_ADD_TEE_ENABLED);
        return webAiGroupOpenSupport && aiGroupParticipationAddTeeEnabled;
    }

    /**
     * Serialises a {@link Jid} into the legacy string form fed to the phash digest.
     *
     * <p>V1 emits the bare {@code user@server} with no agent and no device. V2 emits the full
     * {@code user.0:device@server} with the agent fixed at {@code 0} and the device id included. In
     * both forms the server is mapped through {@link #toLegacyServer} so {@code c.us} becomes
     * {@code s.whatsapp.net}.
     *
     * @param jid     the JID to serialise
     * @param version the {@link DevicePhashVersion} selecting the format
     * @return the legacy-form string fed to the digest
     */
    @WhatsAppWebExport(moduleName = "WAWebPhashUtils",
            exports = {"phashV1", "phashV2"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String toLegacyJidString(Jid jid, DevicePhashVersion version) {
        var user = jid.user();
        var device = jid.device();
        var legacyServer = toLegacyServer(jid.server());
        return switch (version) {
            case V1 -> user + "@" + legacyServer;
            case V2 -> user + ".0:" + device + "@" + legacyServer;
        };
    }

    /**
     * Maps a {@link JidServer} to its legacy wire-form name.
     *
     * <p>Used only by {@link #toLegacyJidString} to substitute {@code c.us} with
     * {@code s.whatsapp.net}; all other servers pass through unchanged.
     *
     * @implNote
     * This implementation compares the input to {@link JidServer#legacyUser()} ({@code c.us}) and,
     * on a match, returns {@link JidServer#user()} ({@code s.whatsapp.net}).
     *
     * @param server the input server
     * @return the legacy server name to use in the digest input
     */
    @WhatsAppWebExport(moduleName = "WAWebWid",
            exports = "toString",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static JidServer toLegacyServer(JidServer server) {
        if (server.equals(JidServer.legacyUser())) {
            return JidServer.user();
        }
        return server;
    }
}
