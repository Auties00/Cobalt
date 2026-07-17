package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.result.UsyncProtocolError;
import com.github.auties00.cobalt.wire.stanza.usync.result.UsyncUserResult;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncBotProfileProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncBusinessProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncContactProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncDeviceProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncDisappearingModeProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncFeatureProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncLidProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncPictureProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncStatusProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncTextStatusProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncUsernameProtocol;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Builder and parser for a USync IQ exchange.
 *
 * <p>A USync IQ is WhatsApp's batch user-data query: one request can ask for
 * several pieces of information (contact membership, device list, profile
 * picture, status, business verified-name, disappearing-mode timer, LID
 * resolution, bot profile, username, modern text status, feature support)
 * about a list of peers in one round trip. Cobalt models the query as a
 * mutable fluent builder so call sites can compose the protocol mix and the
 * user list incrementally; {@link #toNode()} renders the IQ for dispatch and
 * {@link #parseResponse(Stanza)} converts the relay's reply into the typed
 * {@link UsyncResult} surface. Backoff side effects (waiting before send via
 * {@link UsyncBackoff#waitForBackoff(UsyncQuery)} and recording any
 * {@code error_backoff} attached to a per-protocol error via
 * {@link UsyncBackoff#setProtocolBackoffMs(String, long)}) sit outside this
 * class so the builder can stay pure.
 *
 * <p>The "must have at least one protocol" invariant is enforced by the type
 * system: every entry point requires a starting protocol, so it is impossible
 * to build a query with zero protocols.
 *
 * <p><strong>Thread safety.</strong> A single instance is not safe to share
 * across threads. Construct the builder on the thread that dispatches it,
 * configure it once, and call {@link #toNode()} once; the internal
 * {@code protocols} and {@code users} lists and the {@code lidProtocolAdded}
 * flag are unsynchronised, so concurrent {@code with*} mutations produce
 * {@link java.util.ConcurrentModificationException} or otherwise undefined
 * behaviour. Different threads may dispatch their own independent instances in
 * parallel because the underlying transport and the shared {@link UsyncBackoff}
 * registry are concurrency-safe.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public final class UsyncQuery {
    /**
     * Logger that mirrors the parse-success trace emitted by the JS parser
     * callback.
     */
    private static final Logger LOGGER = Logger.getLogger(UsyncQuery.class.getName());

    /**
     * Ordered list of protocols to query; always non-empty by construction.
     */
    private final List<UsyncProtocol> protocols;

    /**
     * Ordered list of user entries to query.
     */
    private final List<UsyncUser> users;

    /**
     * Value emitted on the {@code context} attribute of the {@code <usync>}
     * stanza.
     */
    private UsyncContext context;

    /**
     * Value emitted on the {@code mode} attribute of the {@code <usync>}
     * stanza.
     */
    private UsyncMode mode;

    /**
     * Tracks whether the LID protocol has already been added; subsequent
     * {@link #withLidProtocol()} calls are no-ops.
     */
    private boolean lidProtocolAdded;

    /**
     * Hidden constructor invoked through the {@link #of(UsyncProtocol)} factory
     * and the protocol-specific {@code of*} convenience factories.
     *
     * <p>Forced through a factory so the "at least one protocol" invariant is
     * impossible to violate at compile time.
     *
     * @param firstProtocol the first protocol attached to the query
     */
    private UsyncQuery(UsyncProtocol firstProtocol) {
        this.protocols = new ArrayList<>();
        this.protocols.add(firstProtocol);
        this.users = new ArrayList<>();
        this.context = UsyncContext.INTERACTIVE;
        this.mode = UsyncMode.QUERY;
        if (firstProtocol instanceof UsyncLidProtocol) {
            this.lidProtocolAdded = true;
        }
    }

    /**
     * Creates a new query starting with the supplied protocol.
     *
     * <p>Lowest-level factory; one of the {@code of*} convenience factories is
     * preferred when the starting protocol is a fixed type.
     *
     * @param firstProtocol the first protocol; must not be {@code null}
     * @return a fresh query
     * @throws NullPointerException if {@code firstProtocol} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.ADAPTED)
    public static UsyncQuery of(UsyncProtocol firstProtocol) {
        Objects.requireNonNull(firstProtocol, "firstProtocol cannot be null");
        return new UsyncQuery(firstProtocol);
    }

    /**
     * Creates a query that starts with the {@code contact} protocol.
     *
     * <p>Used by contact-import flows; the addressing mode picks between
     * phone-number and LID identifier spaces and is reflected on the wire's
     * {@code addressing_mode} attribute.
     *
     * @param addressingMode the addressing mode the contact protocol applies to
     * @return a fresh query
     */
    public static UsyncQuery ofContact(UsyncAddressingMode addressingMode) {
        return of(new UsyncContactProtocol(addressingMode));
    }

    /**
     * Creates a query that starts with the {@code devices} protocol.
     *
     * <p>Used by device-list sync; each {@link UsyncUser} is paired with the
     * cached device hash so the relay can return an omit response when the
     * local cache is still in sync.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofDevices() {
        return of(new UsyncDeviceProtocol());
    }

    /**
     * Creates a query that starts with the {@code business} protocol.
     *
     * <p>Used by verified-name fetch flows.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofBusiness() {
        return of(new UsyncBusinessProtocol());
    }

    /**
     * Creates a query that starts with the {@code picture} protocol.
     *
     * <p>Returns the peer's profile-picture id only; the JPEG payload is
     * fetched separately through the media URL.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofPicture() {
        return of(new UsyncPictureProtocol());
    }

    /**
     * Creates a query that starts with the {@code status} protocol.
     *
     * <p>Used by the legacy status string fetch; each {@link UsyncUser} is
     * paired with a trusted-contact token through
     * {@link UsyncUser#withTrustedContactToken(byte[])} when the relay requires
     * one.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofStatus() {
        return of(new UsyncStatusProtocol());
    }

    /**
     * Creates a query that starts with the {@code disappearing_mode} protocol.
     *
     * <p>Used by the disappearing-message timer fetch.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofDisappearingMode() {
        return of(new UsyncDisappearingModeProtocol());
    }

    /**
     * Creates a query that starts with the {@code lid} protocol.
     *
     * <p>Used to resolve a peer's LID identifier in isolation; most call sites
     * combine the LID protocol with another via {@link #withLidProtocol()}
     * instead.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofLid() {
        return of(new UsyncLidProtocol());
    }

    /**
     * Creates a query that starts with the {@code bot} protocol.
     *
     * <p>Used by bot-profile fetches; each {@link UsyncUser} is paired with a
     * persona id through {@link UsyncUser#withPersonaId(String)} when the bot
     * exposes multiple personas.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofBotProfile() {
        return of(new UsyncBotProfileProtocol());
    }

    /**
     * Creates a query that starts with the {@code username} protocol.
     *
     * <p>Used by username-lookup flows.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofUsername() {
        return of(new UsyncUsernameProtocol());
    }

    /**
     * Creates a query that starts with the {@code text_status} protocol.
     *
     * <p>Used by the modern text-status fetch; gated server-side.
     *
     * @return a fresh query
     */
    public static UsyncQuery ofTextStatus() {
        return of(new UsyncTextStatusProtocol());
    }

    /**
     * Creates a query that starts with the {@code feature} protocol restricted
     * to the named feature keys.
     *
     * <p>Used by debug and VoIP capability checks; the keys are the feature
     * wire names the relay understands (see
     * {@link UsyncFeatureProtocol.FeatureQuery}).
     *
     * @param queries the features to request
     * @return a fresh query
     */
    public static UsyncQuery ofFeatures(List<UsyncFeatureProtocol.FeatureQuery> queries) {
        return of(new UsyncFeatureProtocol(queries));
    }

    /**
     * Sets the {@code mode} attribute emitted on the {@code <usync>} stanza.
     *
     * <p>Defaults to {@link UsyncMode#QUERY}; switches to {@link UsyncMode#DELTA}
     * for diff-only contact syncs.
     *
     * @param mode the mode
     * @return this builder
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withMode", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withMode(UsyncMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode cannot be null");
        return this;
    }

    /**
     * Sets the {@code context} attribute emitted on the {@code <usync>} stanza.
     *
     * <p>Defaults to {@link UsyncContext#INTERACTIVE}; switches to
     * {@link UsyncContext#BACKGROUND} for idle batch syncs or to
     * {@link UsyncContext#MESSAGE} / {@link UsyncContext#VOIP} when the
     * resulting device list is needed to encrypt an outbound send.
     *
     * @param context the context
     * @return this builder
     * @throws NullPointerException if {@code context} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withContext", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withContext(UsyncContext context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        return this;
    }

    /**
     * Adds another protocol to this query.
     *
     * <p>Generic counterpart of the protocol-specific {@code with*Protocol}
     * methods; delegates to {@link #withLidProtocol()} when the supplied
     * protocol is a {@link UsyncLidProtocol} so the LID-already-added flag stays
     * consistent.
     *
     * @param protocol the protocol
     * @return this builder
     * @throws NullPointerException if {@code protocol} is {@code null}
     */
    public UsyncQuery withProtocol(UsyncProtocol protocol) {
        Objects.requireNonNull(protocol, "protocol cannot be null");
        if (protocol instanceof UsyncLidProtocol) {
            return withLidProtocol();
        }
        protocols.add(protocol);
        return this;
    }

    /**
     * Adds the {@code contact} protocol to this query.
     *
     * @param addressingMode the addressing mode the contact protocol applies to
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withContactProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withContactProtocol(UsyncAddressingMode addressingMode) {
        protocols.add(new UsyncContactProtocol(addressingMode));
        return this;
    }

    /**
     * Adds the {@code business} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withBusinessProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withBusinessProtocol() {
        protocols.add(new UsyncBusinessProtocol());
        return this;
    }

    /**
     * Adds the {@code devices} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withDeviceProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withDeviceProtocol() {
        protocols.add(new UsyncDeviceProtocol());
        return this;
    }

    /**
     * Adds the {@code disappearing_mode} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withDisappearingModeProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withDisappearingModeProtocol() {
        protocols.add(new UsyncDisappearingModeProtocol());
        return this;
    }

    /**
     * Adds the {@code picture} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withPictureProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withPictureProtocol() {
        protocols.add(new UsyncPictureProtocol());
        return this;
    }

    /**
     * Adds the {@code status} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withStatusProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withStatusProtocol() {
        protocols.add(new UsyncStatusProtocol());
        return this;
    }

    /**
     * Adds the {@code text_status} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withTextStatusProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withTextStatusProtocol() {
        protocols.add(new UsyncTextStatusProtocol());
        return this;
    }

    /**
     * Adds the {@code feature} protocol to this query restricted to the named
     * feature keys.
     *
     * @param queries the features to request
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withFeaturesProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withFeaturesProtocol(List<UsyncFeatureProtocol.FeatureQuery> queries) {
        protocols.add(new UsyncFeatureProtocol(queries));
        return this;
    }

    /**
     * Adds the {@code lid} protocol to this query.
     *
     * <p>Idempotent: subsequent calls are no-ops, preventing duplicate
     * {@code <lid/>} elements inside the {@code <query>}.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withLidProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withLidProtocol() {
        if (!lidProtocolAdded) {
            protocols.add(new UsyncLidProtocol());
            lidProtocolAdded = true;
        }
        return this;
    }

    /**
     * Adds the {@code bot} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withBotProfileProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withBotProfileProtocol() {
        protocols.add(new UsyncBotProfileProtocol());
        return this;
    }

    /**
     * Adds the {@code username} protocol to this query.
     *
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withUsernameProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withUsernameProtocol() {
        protocols.add(new UsyncUsernameProtocol());
        return this;
    }

    /**
     * Adds a user entry.
     *
     * <p>The two-arg {@link #withUser(UsyncUser, Jid)} overload additionally
     * pre-populates the LID slot when the LID protocol is part of the query.
     *
     * @param user the user
     * @return this builder
     * @throws NullPointerException if {@code user} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withUser", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncQuery withUser(UsyncUser user) {
        users.add(Objects.requireNonNull(user, "user cannot be null"));
        return this;
    }

    /**
     * Adds a user entry and pre-populates its LID slot from the supplied hint
     * when the LID protocol is part of this query.
     *
     * <p>When the LID protocol has been added and the user's canonical id is
     * already a LID, that id is copied into the LID slot; otherwise, if the id
     * is a regular phone-number user {@link Jid} and the caller supplied a
     * {@code currentLid} hint, that hint is copied into the LID slot so the
     * relay's LID resolution can proceed without a second round trip.
     *
     * @param user       the user entry
     * @param currentLid the LID currently associated with the user's PN, or
     *                   {@code null} if unknown
     * @return this builder
     * @throws NullPointerException if {@code user} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery.withUser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncQuery withUser(UsyncUser user, Jid currentLid) {
        Objects.requireNonNull(user, "user cannot be null");
        if (lidProtocolAdded && user.id().isPresent()) {
            var id = user.id().get();
            if (id.hasServer(JidServer.lid())) {
                user.withLid(id);
            } else if (id.hasServer(JidServer.user()) && currentLid != null) {
                user.withLid(currentLid);
            }
        }
        users.add(user);
        return this;
    }

    /**
     * Returns the configured {@link UsyncContext}.
     *
     * <p>Read by {@link UsyncBackoff#waitForBackoff(UsyncQuery)} to decide
     * whether per-protocol backoff windows apply.
     *
     * @return the context
     */
    public UsyncContext context() {
        return context;
    }

    /**
     * Returns the configured {@link UsyncMode}.
     *
     * @return the mode
     */
    public UsyncMode mode() {
        return mode;
    }

    /**
     * Returns an immutable snapshot of the registered protocols.
     *
     * <p>Returned in registration order; consumed by
     * {@link UsyncBackoff#waitForBackoff(UsyncQuery)} and by
     * {@link #parseResponse(Stanza)} when matching per-protocol response children.
     *
     * @return the protocols
     */
    public List<UsyncProtocol> protocols() {
        return List.copyOf(protocols);
    }

    /**
     * Returns an immutable snapshot of the registered user entries.
     *
     * @return the users, in registration order
     */
    public List<UsyncUser> users() {
        return List.copyOf(users);
    }

    /**
     * Builds the outbound USync IQ stanza.
     *
     * <p>The returned {@link StanzaBuilder} is ready to dispatch; the caller is
     * expected to call {@link UsyncBackoff#waitForBackoff(UsyncQuery)} first to
     * honour any active per-protocol backoff window.
     *
     * @implNote
     * This implementation filters out users whose id is a legacy server
     * {@link JidServer#legacyUser()} JID before building the {@code <list>}
     * element; an empty post-filter list logs a warning rather than throwing so
     * the relay still receives a well-formed IQ.
     *
     * @return the IQ builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toNode() {
        var validUsers = new ArrayList<UsyncUser>();
        for (var user : users) {
            var id = user.id().orElse(null);
            if (id != null && id.hasServer(JidServer.legacyUser())) {
                continue;
            }
            validUsers.add(user);
        }
        if (validUsers.isEmpty() && !users.isEmpty()) {
            LOGGER.warning("Usync warning: every supplied user was filtered out before dispatch");
        }

        var queryChildren = new ArrayList<Stanza>(protocols.size());
        for (var protocol : protocols) {
            queryChildren.add(protocol.buildQueryElement());
        }
        var queryNode = new StanzaBuilder().description("query").content(queryChildren).build();

        var userNodes = new ArrayList<Stanza>(validUsers.size());
        for (var user : validUsers) {
            var userBuilder = new StanzaBuilder().description("user");
            user.id().ifPresent(jid -> userBuilder.attribute("jid", jid.toString()));
            user.phoneJid().ifPresent(jid -> userBuilder.attribute("pn_jid", jid.toString()));
            var children = new ArrayList<Stanza>(protocols.size());
            for (var protocol : protocols) {
                protocol.buildUserElement(user).ifPresent(children::add);
            }
            userBuilder.content(children);
            userNodes.add(userBuilder.build());
        }
        var listNode = new StanzaBuilder().description("list").content(userNodes).build();

        var usyncNode = new StanzaBuilder()
                .description("usync")
                .attribute("sid", RandomIdUtils.newId())
                .attribute("index", "0")
                .attribute("last", "true")
                .attribute("mode", mode.wireValue())
                .attribute("context", context.wireValue())
                .content(List.of(queryNode, listNode))
                .build();

        return new StanzaBuilder()
                .description("iq")
                .attribute("to", JidServer.user().toString())
                .attribute("xmlns", "usync")
                .attribute("type", "get")
                .attribute("id", RandomIdUtils.newId())
                .content(List.of(usyncNode));
    }

    /**
     * Parses the relay's IQ response into a {@link UsyncResult}.
     *
     * <p>Pure: no backoff registry is touched. The caller is expected to
     * inspect each {@link UsyncProtocolError#errorBackoff()} on the returned
     * result and call {@link UsyncBackoff#setProtocolBackoffMs(String, long)} as
     * needed.
     *
     * @implNote
     * This implementation routes every response whose {@code type} is not
     * {@code "result"} to a top-level error envelope. For success responses,
     * per-protocol errors are populated from each {@code <error/>} child of the
     * {@code <result>} block and per-protocol refresh windows from each
     * {@code refresh} attribute; per-user parsing then delegates to each
     * protocol's {@link UsyncProtocol#parseUserResult(Stanza)}.
     *
     * @param response the relay's IQ response
     * @return the aggregated parse result
     * @throws NullPointerException  if {@code response} is {@code null}
     * @throws IllegalStateException if a successful response is missing the
     *                               required {@code <usync>} envelope
     */
    @WhatsAppWebExport(moduleName = "WAWebUsync",
            exports = "USyncQuery", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncResult parseResponse(Stanza response) {
        Objects.requireNonNull(response, "response cannot be null");
        var type = response.getAttributeAsString("type", "");
        if (!"result".equals(type)) {
            return new UsyncResult(List.of(), Map.of(), Map.of(), parseTopLevelError(response));
        }

        var usyncNode = response.getChild("usync")
                .orElseThrow(() -> new IllegalStateException("usync stanza missing in response"));
        var resultNode = usyncNode.getChild("result");
        var listNode = usyncNode.getChild("list");

        var protocolErrors = new HashMap<String, UsyncProtocolError>();
        var protocolRefreshes = new HashMap<String, Duration>();
        if (resultNode.isPresent()) {
            for (var protocol : protocols) {
                resultNode.get().getChild(protocol.name()).ifPresent(node -> {
                    var err = node.getChild("error");
                    if (err.isPresent()) {
                        var e = err.get();
                        var code = e.getRequiredAttributeAsInt("code");
                        var text = e.getAttributeAsString("text", "");
                        var backoff = e.getAttributeAsLong("backoff").stream()
                                .mapToObj(Duration::ofSeconds).findFirst().orElse(null);
                        protocolErrors.put(protocol.name(), new UsyncProtocolError(code, text, backoff));
                    } else {
                        node.getAttributeAsLong("refresh").stream()
                                .mapToObj(Duration::ofSeconds).findFirst()
                                .ifPresent(d -> protocolRefreshes.put(protocol.name(), d));
                    }
                });
            }
        }

        var users = new ArrayList<UsyncUserResult>();
        if (listNode.isPresent()) {
            listNode.get().streamChildren("user").forEach(userNode -> {
                var perProtocol = new LinkedHashMap<String, UsyncProtocolResult>();
                for (var protocol : protocols) {
                    userNode.getChild(protocol.name())
                            .ifPresent(child -> perProtocol.put(protocol.name(), protocol.parseUserResult(child)));
                }
                var jid = userNode.getAttributeAsJid("jid").orElse(null);
                var phoneJid = userNode.getAttributeAsJid("pn_jid").orElse(null);
                users.add(new UsyncUserResult(jid, phoneJid, perProtocol));
            });
        }

        LOGGER.fine("usync query success!");
        return new UsyncResult(users, protocolErrors, protocolRefreshes, null);
    }

    /**
     * Builds a top-level error from a response whose {@code type} attribute is
     * not {@code "result"}.
     *
     * <p>Missing {@code <error/>} children fall back to {@code -1} and empty
     * strings rather than throwing.
     *
     * @param response the IQ response carrying the error envelope
     * @return the parsed top-level error
     */
    private UsyncTopLevelError parseTopLevelError(Stanza response) {
        var errorNode = response.getChild("error");
        var code = errorNode.map(n -> n.getAttributeAsInt("code", -1)).orElse(-1);
        var text = errorNode.map(n -> n.getAttributeAsString("text", "")).orElse("");
        var typeAttr = errorNode.map(n -> n.getAttributeAsString("type", "")).orElse("");
        return new UsyncTopLevelError(code, text, typeAttr);
    }
}
