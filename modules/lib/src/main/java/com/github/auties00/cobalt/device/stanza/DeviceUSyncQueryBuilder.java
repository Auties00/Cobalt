package com.github.auties00.cobalt.device.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListHashInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import java.lang.System.Logger.Level;
import java.util.*;

/**
 * Builds the USync IQ stanzas used by the device-protocol query path.
 *
 * <p>Callers (typically {@link com.github.auties00.cobalt.device.DeviceService}) pass a set of user
 * {@link Jid}s plus an optional dhash-and-timestamp map and receive one or more IQ
 * {@link StanzaBuilder} instances they can send through the socket. Each IQ wraps the WAP
 * {@code <iq xmlns="usync" type="get">} shape that
 * {@link com.github.auties00.cobalt.device.adv.DeviceADVValidator} and
 * {@link DeviceUSyncResponseParser} consume on the response side. This is the wire-level entry point
 * for the device-list synchronisation flow; it emits only the device protocol (optionally paired
 * with the username probe) and does not cover the contact, business, picture, lid, or
 * disappearing-mode protocols that the broader WA Web {@code USyncQuery} chain supports.
 *
 * @implNote
 * This implementation flattens the JS fluent {@code USyncQuery} builder into a single static factory
 * because Cobalt only emits the device protocol (optionally paired with username) and never combines
 * several USync protocols in one query. Attribute insertion order matches WA Web's WAP object literal
 * so the encoded bytes are byte-identical to live traffic.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
@WhatsAppWebModule(moduleName = "WAWebUsyncDevice")
@WhatsAppWebModule(moduleName = "WAWebUsyncUsername")
@WhatsAppWebModule(moduleName = "WAWebUsyncLid")
public final class DeviceUSyncQueryBuilder {
    /**
     * The logger for {@link DeviceUSyncQueryBuilder}.
     */
    private static final System.Logger LOGGER = Log.get(DeviceUSyncQueryBuilder.class);

    /**
     * Caps the number of {@code <user>} entries that fit in a single USync IQ.
     *
     * <p>User sets larger than this value passed to {@link #build(Set, String, Map, boolean)} are
     * sliced into batches of this size so each individual IQ stays under the server-enforced stanza
     * budget the device-list synchronisation flow assumes.
     *
     * @implNote
     * This implementation hard-codes the cap because WA Web does the same splitting outside the
     * {@code USyncQuery} builder and the value is not exposed as an AB prop.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSyncDeviceListApi",
            exports = "syncDeviceList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static final int MAX_USERS_PER_QUERY = 500;

    /**
     * Holds the phone-number addressing literal for the {@code addressing_mode} attribute on a USync
     * contact-protocol query element.
     *
     * <p>This mirrors the {@code "pn"} entry of WA Web's frozen
     * {@code USYNC_ADDRESSING_MODE = {PN: "pn", LID: "lid"}} object. Callers write the value verbatim
     * into the {@code addressing_mode} attribute when the contact protocol is in use.
     *
     * @see #USYNC_ADDRESSING_MODE_LID
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USYNC_ADDRESSING_MODE",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String USYNC_ADDRESSING_MODE_PN = "pn";

    /**
     * Holds the long-identifier (LID) addressing literal for the {@code addressing_mode} attribute on
     * a USync contact-protocol query element.
     *
     * <p>This mirrors the {@code "lid"} entry of WA Web's frozen
     * {@code USYNC_ADDRESSING_MODE = {PN: "pn", LID: "lid"}} object. Callers write the value verbatim
     * into the {@code addressing_mode} attribute when the contact protocol is in use.
     *
     * @see #USYNC_ADDRESSING_MODE_PN
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USYNC_ADDRESSING_MODE",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String USYNC_ADDRESSING_MODE_LID = "lid";

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private DeviceUSyncQueryBuilder() {
        throw new AssertionError();
    }

    /**
     * Builds one or more USync device-protocol IQ stanzas for the given users.
     *
     * <p>The {@code userJids} set is sliced into batches of up to {@value MAX_USERS_PER_QUERY} entries
     * (see {@link #MAX_USERS_PER_QUERY}), and one IQ {@link StanzaBuilder} is returned per batch for the
     * caller to send in turn. The {@code context} value is written verbatim onto the {@code <usync>}
     * element and must be a valid USync context wire literal ({@code "interactive"}, {@code "message"},
     * {@code "voip"}, {@code "background"}, or {@code "notification"}); the relay does not recognise
     * caller-internal sync labels. When {@code includeUsernameProtocol} is {@code true} the
     * {@code <query>} element also carries the {@code <username/>} probe. At least one batch is always
     * returned, even for an empty {@code userJids} set, in which case the IQ carries an empty
     * {@code <list/>}.
     *
     * @implNote
     * This implementation always returns at least one batch, even when {@code userJids} is empty, so
     * callers do not have to special-case the zero-user path.
     *
     * @param userJids                the JIDs whose device lists must be queried
     * @param context                 the {@code context} attribute written onto the {@code <usync>}
     *                                element
     * @param hashInfos               per-user dhash-and-timestamp records that let the server reply
     *                                with {@code <devices/>} when the cached list is unchanged, or
     *                                {@code null} for an unconditional refresh
     * @param includeUsernameProtocol whether to add the {@code <username/>} probe to the
     *                                {@code <query>} element
     * @return the IQ {@link StanzaBuilder} instances, one per batch
     * @throws NullPointerException if {@code userJids} or {@code context} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static List<StanzaBuilder> build(Set<Jid> userJids, String context, Map<Jid, DeviceListHashInfo> hashInfos, boolean includeUsernameProtocol) {
        Objects.requireNonNull(userJids, "userJids cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        var userJidsCount = userJids.size();
        if (userJidsCount <= MAX_USERS_PER_QUERY) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building usync device query: users={0}, context={1}, batches=1", userJidsCount, context);
            return List.of(buildEntry(userJids, context, hashInfos, includeUsernameProtocol));
        } else {
            var iterator = userJids.iterator();
            var batch = new ArrayList<Jid>(MAX_USERS_PER_QUERY);
            var batches = new ArrayList<StanzaBuilder>(userJidsCount / MAX_USERS_PER_QUERY);
            while (iterator.hasNext()) {
                batch.add(iterator.next());
                if (batch.size() == MAX_USERS_PER_QUERY) {
                    batches.add(buildEntry(batch, context, hashInfos, includeUsernameProtocol));
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                batches.add(buildEntry(batch, context, hashInfos, includeUsernameProtocol));
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building usync device query: users={0}, context={1}, batches={2}", userJidsCount, context, batches.size());
            return batches;
        }
    }

    /**
     * Builds a USync IQ that resolves the long-identifier (LID) for a single phone-number user.
     *
     * <p>The query carries only the LID protocol ({@code <query><lid/></query>}) for one
     * {@code <user jid="...">} entry, mirroring WA Web's {@code USyncLidProtocol}: the server answers
     * with a {@code <lid val="...@lid"/>} child under the matching {@code <user>} when a LID is assigned
     * to the number. The {@code context} value is written verbatim onto the {@code <usync>} element and
     * must be a valid USync context wire literal; the call path uses {@code "voip"}. The device suffix
     * of {@code userJid} is stripped before it is written into the {@code jid} attribute.
     *
     * @implNote
     * This implementation issues a focused LID-only query rather than bundling the LID protocol into the
     * device-protocol sweep the way WA Web's contact sync does, because Cobalt resolves a peer LID
     * on demand at call placement and caches the result; the bundled form is not needed for the device
     * list itself.
     *
     * @param userJid the phone-number user JID whose LID must be resolved
     * @param context the {@code context} attribute written onto the {@code <usync>} element
     * @return the IQ {@link StanzaBuilder} ready for dispatch
     * @throws NullPointerException if {@code userJid} or {@code context} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "USyncLidProtocol",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static StanzaBuilder buildLidQuery(Jid userJid, String context) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building usync lid query for {0}, context={1}", userJid, context);

        var sessionId = RandomIdUtils.newId();

        var userNode = new StanzaBuilder()
                .description("user")
                .attribute("jid", userJid.toUserJid())
                .build();
        var listNode = new StanzaBuilder()
                .description("list")
                .content(userNode)
                .build();
        var lidNode = new StanzaBuilder()
                .description("lid")
                .build();
        var queryNode = new StanzaBuilder()
                .description("query")
                .content(lidNode)
                .build();

        var usyncNode = new StanzaBuilder()
                .description("usync")
                .attribute("sid", sessionId)
                .attribute("index", "0")
                .attribute("last", "true")
                .attribute("mode", "query")
                .attribute("context", context)
                .content(queryNode, listNode)
                .build();

        return new StanzaBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("xmlns", "usync")
                .attribute("type", "get")
                .content(usyncNode);
    }

    /**
     * Assembles a single batch IQ around a {@code <usync><query/><list/></usync>} envelope.
     *
     * <p>This is the per-batch worker called by {@link #build(Set, String, Map, boolean)} for every
     * batch slice. It drops the public-service-announcements account from the user list, builds one
     * {@code <user>} child per surviving JID, and wraps them in the {@code <usync>} envelope carrying
     * the {@code sid}, {@code index}, {@code last}, {@code mode}, and {@code context} attributes. When
     * {@code includeUsernameProtocol} is {@code true} a {@code <username/>} probe is added alongside
     * the {@code <devices/>} element inside {@code <query>}.
     *
     * @implNote
     * This implementation mirrors the JS attribute insertion order ({@code sid}, {@code index},
     * {@code last}, {@code mode}, {@code context}) so the serialised WAP bytes match live traffic. The
     * outer {@code <iq>} {@link Stanza} has no {@code id} attribute; the transport layer assigns one at
     * send time, which is why a {@link StanzaBuilder} is returned instead of a built {@link Stanza}.
     *
     * @param userJids                the JIDs forming this batch
     * @param context                 the {@code context} attribute
     * @param hashInfos               the per-user dhash map, or {@code null}
     * @param includeUsernameProtocol whether to add the {@code <username/>} probe
     * @return the partially built {@link StanzaBuilder} for the IQ
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebUsyncUsername",
            exports = "USyncUsernameProtocol",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static StanzaBuilder buildEntry(Collection<Jid> userJids, String context, Map<Jid, DeviceListHashInfo> hashInfos, boolean includeUsernameProtocol) {
        var sessionId = RandomIdUtils.newId();

        var userNodes = userJids.stream()
                .filter(jid -> !jid.toUserJid().equals(Jid.announcementsAccount()))
                .map(jid -> buildUserNode(jid, hashInfos))
                .toList();

        var listNode = new StanzaBuilder()
                .description("list")
                .content(userNodes)
                .build();

        var devicesNode = new StanzaBuilder()
                .description("devices")
                .attribute("version", "2")
                .build();

        Stanza queryStanza;
        if (includeUsernameProtocol) {
            var usernameNode = new StanzaBuilder()
                    .description("username")
                    .build();
            queryStanza = new StanzaBuilder()
                    .description("query")
                    .content(devicesNode, usernameNode)
                    .build();
        } else {
            queryStanza = new StanzaBuilder()
                    .description("query")
                    .content(devicesNode)
                    .build();
        }

        var usyncNode = new StanzaBuilder()
                .description("usync")
                .attribute("sid", sessionId)
                .attribute("index", "0")
                .attribute("last", "true")
                .attribute("mode", "query")
                .attribute("context", context)
                .content(queryStanza, listNode)
                .build();

        return new StanzaBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("xmlns", "usync")
                .attribute("type", "get")
                .content(usyncNode);
    }

    /**
     * Builds the per-user {@code <devices>} element carrying the cached dhash and timestamps.
     *
     * <p>This is the inner worker for {@link #buildUserNode(Jid, Map)}; it emits the optional
     * {@code <devices>} element only when the server may reply with the lighter omitted form (the
     * cached list is still current) instead of resending the full device list. The element carries the
     * {@code device_hash} attribute from {@link DeviceListHashInfo#hash()}, the {@code ts} attribute
     * from {@link DeviceListHashInfo#timestamp()}, and the {@code expected_ts} attribute from
     * {@link DeviceListHashInfo#expectedTimestamp()}, each only when present.
     *
     * @implNote
     * This implementation returns {@code null} (so the caller omits the element entirely) when every
     * field of {@code hashInfo} is unset, matching WA Web's
     * {@code USyncDeviceProtocol.getUserElement} which returns {@code null} in the same case.
     *
     * @param hashInfo the cached dhash record for the user, or {@code null}
     * @return the {@code <devices>} {@link Stanza}, or {@code null} when no attributes would be set
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
            exports = "USyncDeviceProtocol",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static Stanza buildUserDevicesElement(DeviceListHashInfo hashInfo) {
        if (hashInfo == null) {
            return null;
        }

        var hash = hashInfo.hash();
        var ts = hashInfo.timestamp();
        var expectedTs = hashInfo.expectedTimestamp().orElse(null);

        if (hash == null && ts == null && expectedTs == null) {
            return null;
        }

        var devicesBuilder = new StanzaBuilder()
                .description("devices");
        if (hash != null) {
            devicesBuilder.attribute("device_hash", hash);
        }
        if (ts != null) {
            devicesBuilder.attribute("ts", ts.getEpochSecond());
        }
        if (expectedTs != null) {
            devicesBuilder.attribute("expected_ts", expectedTs.getEpochSecond());
        }
        return devicesBuilder.build();
    }

    /**
     * Builds the {@code <user jid="...">} envelope for one entry of the USync batch.
     *
     * <p>This is the inner worker for {@link #buildEntry(Collection, String, Map, boolean)}; one
     * {@code <user>} stanza is emitted per JID that survives the announcements-account filter. When a
     * dhash map is supplied and holds a record for the user, the optional {@code <devices>} child
     * produced by {@link #buildUserDevicesElement(DeviceListHashInfo)} is attached.
     *
     * @implNote
     * This implementation normalises the JID written into the {@code jid} attribute via
     * {@link Jid#toUserJid()} so the device suffix is stripped, matching WA Web's user-JID attribute
     * helper.
     *
     * @param jid       the user JID
     * @param hashInfos the dhash map keyed by user JID, or {@code null}
     * @return the built {@code <user>} {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static Stanza buildUserNode(Jid jid, Map<Jid, DeviceListHashInfo> hashInfos) {
        var userJid = jid.toUserJid();
        var builder = new StanzaBuilder()
                .description("user")
                .attribute("jid", userJid);

        if (hashInfos != null) {
            var devicesElement = buildUserDevicesElement(hashInfos.get(userJid));
            if (devicesElement != null) {
                builder.content(devicesElement);
            }
        }

        return builder.build();
    }
}
