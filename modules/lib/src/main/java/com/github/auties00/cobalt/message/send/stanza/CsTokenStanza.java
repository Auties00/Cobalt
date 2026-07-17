package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Builds the optional {@code <cstoken>} child of an outgoing {@code <message>} stanza carrying the non-contact-token
 * (NCT) HMAC tag.
 * <p>
 * The NCT tag lets the server flag an outgoing message as sent to a non-contact recipient without revealing whom; it is
 * keyed by the server-issued NCT salt and the recipient's account LID. The tag is emitted only when the
 * {@link ABProp#WA_NCT_TOKEN_SEND_ENABLED} prop is on, the recipient is a regular user (not bot, not group, not
 * broadcast), the store holds an NCT salt, and the chat carries an {@code accountLid} for the recipient. Composed by
 * {@link ChatFanoutStanza} after the more authoritative {@code <tctoken>} fallback in {@link TcTokenStanza}.
 *
 * @implNote This implementation caches up to {@value #MAX_CACHE_SIZE} HMAC results per salt: the salt is cached
 * alongside its results and the cache is cleared whenever the salt changes. The cache is intentionally small;
 * regenerating an HMAC-SHA-256 is cheap, but caching avoids the per-send cost in steady state.
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgCreateFanoutStanza")
public final class CsTokenStanza {
    /**
     * The logger for {@link CsTokenStanza}.
     */
    private static final System.Logger LOGGER = Log.get(CsTokenStanza.class);

    /**
     * Caps the number of cached HMAC results per salt.
     */
    private static final int MAX_CACHE_SIZE = 5;

    /**
     * Names the HMAC algorithm used for NCT token derivation.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Holds the store consulted for the NCT salt and the chat's {@code accountLid}.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Gates token emission via the {@link ABProp#WA_NCT_TOKEN_SEND_ENABLED} prop.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the salt bytes currently bound to {@link #hmacCache}; the cache is cleared whenever this reference moves.
     */
    private byte[] cachedSalt;

    /**
     * Caches HMAC results keyed by the recipient LID's string form.
     */
    private final LinkedHashMap<String, byte[]> hmacCache;

    /**
     * Constructs a builder bound to a store and AB-props service.
     * <p>
     * The per-salt HMAC cache lives on this instance, so reusing the builder across sends amortises the HMAC cost.
     *
     * @param store          the {@link LinkedWhatsAppStore} used to retrieve the NCT salt and chat account LID
     * @param abPropsService the {@link ABPropsService} used to gate token emission
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "genCsTokenBody",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public CsTokenStanza(LinkedWhatsAppStore store, ABPropsService abPropsService) {
        this.store = Objects.requireNonNull(store, "store");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService");
        this.hmacCache = new LinkedHashMap<>();
    }

    /**
     * Builds the {@code <cstoken>} stanza for the recipient chat, or {@code null} when emission is gated off.
     * <p>
     * Returns {@code null} when any of the four gates fails: {@link ABProp#WA_NCT_TOKEN_SEND_ENABLED} is disabled, the
     * recipient is not a regular user, the store has no NCT salt, or the chat has no {@code accountLid}. The returned
     * stanza carries the raw HMAC-SHA-256 bytes as its content with no attributes.
     *
     * @implNote This implementation computes {@code HMAC-SHA-256(salt, accountLid.toString())} over the salt held in the
     * store, which is already decoded so no base64 decode step is needed inside {@code build}.
     *
     * @param chatJid the recipient chat {@link Jid}
     * @return the {@code <cstoken>} {@link Stanza}, or {@code null} when not applicable
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "genCsTokenBody",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza build(Jid chatJid) {
        if (!abPropsService.getBool(ABProp.WA_NCT_TOKEN_SEND_ENABLED)) {
            return null;
        }

        if (!isRegularUser(chatJid)) {
            return null;
        }

        var salt = store.accountStore().notificationContentTokenSalt().orElse(null);
        if (salt == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cstoken build skipped: no nct salt in store");
            return null;
        }

        var chat = store.chatStore().findChatByJid(chatJid).orElse(null);
        var recipientLid = chat != null ? chat.accountLid().orElse(null) : null;
        if (recipientLid == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cstoken build skipped: no account lid for chat {0}", chatJid);
            return null;
        }

        try {
            if (cachedSalt == null || !Arrays.equals(cachedSalt, salt)) {
                cachedSalt = salt;
                hmacCache.clear();
            }

            var lidString = recipientLid.toString();

            var cached = hmacCache.get(lidString);
            if (cached != null) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "cstoken hmac cache hit for chat {0}", chatJid);
                return new StanzaBuilder()
                        .description("cstoken")
                        .content(cached)
                        .build();
            }

            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(salt, HMAC_ALGORITHM));
            var hmacResult = mac.doFinal(lidString.getBytes(StandardCharsets.UTF_8));

            if (hmacCache.size() >= MAX_CACHE_SIZE) {
                var firstKey = hmacCache.keySet().iterator().next();
                if (firstKey != null) {
                    hmacCache.remove(firstKey);
                }
            }
            hmacCache.put(lidString, hmacResult);

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cstoken hmac computed for chat {0}", chatJid);
            return new StanzaBuilder()
                    .description("cstoken")
                    .content(hmacResult)
                    .build();
        } catch (GeneralSecurityException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cstoken hmac generation failed for chat " + new LogRedactable.User(String.valueOf(chatJid)), e);
            return null;
        }
    }

    /**
     * Returns whether the given {@link Jid} identifies a regular human user eligible for NCT tagging.
     * <p>
     * Bots, groups, broadcasts, newsletters, and PSAs are all excluded. Hosted-business JIDs ({@code @hosted},
     * {@code @hosted.lid}) are also excluded because they do not satisfy {@link Jid#hasUserServer()} or
     * {@link Jid#hasLidServer()}.
     *
     * @implNote This implementation treats a regular user as a user or LID JID that is not a bot.
     *
     * @param jid the {@link Jid} to test
     * @return {@code true} when the JID is a regular user
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isRegularUser",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isRegularUser(Jid jid) {
        return (jid.hasUserServer() || jid.hasLidServer()) && !jid.isBot();
    }
}
