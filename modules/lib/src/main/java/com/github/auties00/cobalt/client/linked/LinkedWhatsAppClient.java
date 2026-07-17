package com.github.auties00.cobalt.client.linked;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.listener.NewMessageListener;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.client.WhatsAppClient;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.emoji.WhatsAppEmoji;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallInteraction;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.exception.*;
import com.github.auties00.cobalt.exception.linked.*;
import com.github.auties00.cobalt.exception.linked.web.*;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.listener.linked.*;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.bot.profile.BotDirectory;
import com.github.auties00.cobalt.wire.linked.bot.profile.BotProfile;
import com.github.auties00.cobalt.wire.linked.business.*;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialIssuance;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialIssuanceRequest;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialServiceConfig;
import com.github.auties00.cobalt.wire.linked.business.ads.*;
import com.github.auties00.cobalt.wire.linked.business.ai.*;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelAgentStatus;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelCommand;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelIdentity;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelLinkedStatus;
import com.github.auties00.cobalt.wire.linked.business.auth.*;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartRefresh;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartRefreshOptions;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessRefreshedCart;
import com.github.auties00.cobalt.wire.linked.business.catalog.*;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessMerchantCompliance;
import com.github.auties00.cobalt.wire.linked.business.compliance.MerchantComplianceEdit;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingEligibility;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingEligibilityQuery;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingServiceData;
import com.github.auties00.cobalt.wire.linked.business.ctwa.*;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlowMetadata;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessAccountNonce;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessEligibility;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAccounts;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAdAccounts;
import com.github.auties00.cobalt.wire.linked.business.marketing.*;
import com.github.auties00.cobalt.wire.linked.business.order.BusinessOrder;
import com.github.auties00.cobalt.wire.linked.business.order.BusinessOrderItem;
import com.github.auties00.cobalt.wire.linked.business.order.OrderLifecycleStatus;
import com.github.auties00.cobalt.wire.linked.business.order.OrderPaymentStatus;
import com.github.auties00.cobalt.wire.linked.chat.ChatExportOptions;
import com.github.auties00.cobalt.wire.linked.business.postcode.BusinessPostcodeVerification;
import com.github.auties00.cobalt.wire.linked.business.postcode.BusinessPostcodeVerificationResult;
import com.github.auties00.cobalt.wire.linked.business.profile.*;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionActionLog;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionLogAcknowledgement;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionSurfaceBatch;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionTriggerContext;
import com.github.auties00.cobalt.wire.linked.business.webgraphql.WhatsAppWebGraphQlSession;
import com.github.auties00.cobalt.wire.linked.business.subscription.BusinessSubscriptionEntryPoints;
import com.github.auties00.cobalt.wire.linked.business.subscription.BusinessSubscriptions;
import com.github.auties00.cobalt.wire.linked.business.support.*;
import com.github.auties00.cobalt.wire.linked.business.waa.*;
import com.github.auties00.cobalt.wire.linked.call.CallLink;
import com.github.auties00.cobalt.wire.linked.call.CallLinkCreate;
import com.github.auties00.cobalt.wire.linked.call.CallLinkMedia;
import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.linked.chat.*;
import com.github.auties00.cobalt.wire.linked.chat.community.*;
import com.github.auties00.cobalt.wire.linked.chat.group.*;
import com.github.auties00.cobalt.wire.linked.contact.*;
import com.github.auties00.cobalt.wire.linked.federated.*;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.jid.LidChange;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.stanza.model.SizedInputStream;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.call.ScheduledCallCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.call.ScheduledCallEditMessage;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.group.GroupInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.*;
import com.github.auties00.cobalt.wire.linked.payment.BrazilCustomPaymentMethod;
import com.github.auties00.cobalt.wire.linked.payment.BrazilCustomPaymentMethodCreate;
import com.github.auties00.cobalt.wire.linked.payment.PaymentsTosV3ConsumerVariant;
import com.github.auties00.cobalt.wire.linked.preference.*;
import com.github.auties00.cobalt.wire.linked.privacy.*;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.linked.reporting.*;
import com.github.auties00.cobalt.wire.linked.setting.*;
import com.github.auties00.cobalt.wire.linked.setting.notice.UserNoticeBundle;
import com.github.auties00.cobalt.wire.linked.setting.notice.UserNoticeStage;
import com.github.auties00.cobalt.wire.linked.setting.notice.UserNoticeStageQuery;
import com.github.auties00.cobalt.wire.linked.setting.privacy.ContactBlacklistAddressingMode;
import com.github.auties00.cobalt.wire.linked.setting.privacy.OptOutListUpdate;
import com.github.auties00.cobalt.wire.linked.setting.push.PushConfig;
import com.github.auties00.cobalt.wire.linked.signal.*;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeight;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethod;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncQuery;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncResult;
import com.github.auties00.cobalt.store.linked.*;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.util.BusinessLabelConstants;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Public contract for the WhatsApp client surface that mirrors WA Web's
 * top-level XMPP session: encrypted socket, signed pre-key uploads, app-state
 * sync, IQ/SMAX/MEX/GraqlQL dispatch, business catalog jobs, signalling for VoIP,
 * and the full set of conversation operations the Web UI exposes.
 *
 * <p>This is the consumer-facing interface; every caller in the codebase
 * depends on it (never on a concrete implementation). The default production
 * implementation is {@link LiveLinkedWhatsAppClient}, wired up by
 * {@link LinkedWhatsAppClientBuilder}. Tests can supply their own implementation
 * through the {@code client.test} package without standing up a real socket.
 *
 * <p>Lifecycle: callers obtain a builder via {@link #builder()}, configure
 * the store and the {@link WhatsAppLinkedClientErrorHandler}, call {@link #connect()}
 * to bring the socket up, drive feature operations on this interface, and
 * shut down with {@link #disconnect()}, {@link #reconnect()}, or
 * {@link #logout()}. {@link #waitForDisconnection()} blocks the caller until
 * a non-reconnect disconnect lands. Session events are delivered through
 * {@link LinkedWhatsAppClientListener}.
 *
 * <p>Method-level javadoc lives here on the interface; the implementation
 * in {@link LiveLinkedWhatsAppClient} inherits docs via {@code {@inheritDoc}} so
 * each concrete method does not duplicate the contract. Implementation-only
 * notes (WA-source mappings, timing, adaptation comments) remain on the impl
 * via {@code com.github.auties00.cobalt.meta.*} annotations and {@code @implNote}.
 */
public non-sealed interface LinkedWhatsAppClient extends WhatsAppClient<LinkedWhatsAppClient> {
    /**
     * Returns the entry point for assembling a configured
     * {@link LinkedWhatsAppClient}.
     *
     * @apiNote
     * Embedders call this once at startup and chain a flavour selector
     * ({@link LinkedWhatsAppClientBuilder#webClient()},
     * {@link LinkedWhatsAppClientBuilder#mobileClient()}, or
     * {@link LinkedWhatsAppClientBuilder#customClient()}) followed by the
     * connection and verification steps to obtain a ready
     * {@code LinkedWhatsAppClient}.
     *
     * @return a fresh {@link LinkedWhatsAppClientBuilder}
     */
    static LinkedWhatsAppClientBuilder builder() {
        return new LinkedWhatsAppClientBuilder();
    }

    /**
     * Returns the {@link LinkedWhatsAppStore} that backs this session.
     *
     * @apiNote
     * The store is the single source of truth for every persisted
     * entity this session knows about: chats, contacts, messages,
     * Signal keys, app-state versions, AB-prop overrides, and presence.
     * Embedders read state directly via its accessors and subscribe to
     * live updates with {@link LinkedWhatsAppStore#addListener}.
     *
     * @return the live store backing this client
     */
    LinkedWhatsAppStore store();

    /**
     * Brings the encrypted socket up and starts the stanza pump.
     *
     * @apiNote
     * This is the entry point that turns a configured but inert
     * {@link LinkedWhatsAppClient} into a live session: it opens the encrypted
     * tunnel, installs a JVM shutdown hook so an abrupt process exit
     * still flushes pending work, and starts dispatching events to
     * {@link LinkedWhatsAppClientListener} subscribers. The method returns as
     * soon as the socket is up; subsequent handshake, pairing, and login
     * events arrive asynchronously through
     * {@link LinkedWhatsAppClientListener#onLoggedIn(LinkedWhatsAppClient)} and
     * related callbacks. Use {@link #waitForDisconnection()} on a
     * separate thread when the caller needs to block until the session
     * ends.
     *
     * @return {@code this}, for fluent chaining
     * @throws IllegalStateException if the client is already connected
     */
    LinkedWhatsAppClient connect();

    /**
     * Completes the pending request whose {@code id} attribute matches
     * the inbound stanza.
     *
     * @apiNote
     * Internal plumbing for request/response correlation: every
     * request carries an id that the matching response echoes back,
     * and this call wakes the parked
     * {@link #sendNode(StanzaBuilder)} caller whose id corresponds to
     * the inbound stanza. Embedders driving custom stanzas through
     * {@link #sendNode(StanzaBuilder)} do not call this directly.
     *
     * @param stanza the inbound stanza that may carry a response to a
     *             pending request
     */
    void resolvePendingRequest(Stanza stanza);

    /**
     * Tears down the session for the given reason and propagates that
     * reason to every registered listener.
     *
     * @apiNote
     * The chosen
     * {@link WhatsAppClientDisconnectReason} drives store-level side
     * effects: {@link WhatsAppClientDisconnectReason#LOGGED_OUT} and
     * {@link WhatsAppClientDisconnectReason#BANNED} purge the persisted
     * credentials so the next connect needs a fresh pairing ceremony,
     * {@link WhatsAppClientDisconnectReason#RECONNECTING} leaves them
     * intact and triggers an immediate reconnect, and
     * {@link WhatsAppClientDisconnectReason#DISCONNECTED} simply closes
     * the socket. Listeners observe the transition via
     * {@link LinkedWhatsAppClientListener#onDisconnected(LinkedWhatsAppClient, WhatsAppClientDisconnectReason)}.
     *
     * @param reason the disconnection reason
     */
    void disconnect(WhatsAppClientDisconnectReason reason);

    /**
     * Sends the given stanza on the current socket without waiting for a
     * response.
     *
     * @apiNote
     * Use this for fire-and-forget messages: presence updates, ack
     * stanzas, receipts, and analytics broadcasts that either do not
     * require an acknowledgment or whose acknowledgment arrives as a
     * separate inbound notification. For request/response exchanges
     * call {@link #sendNode(StanzaBuilder)} instead.
     *
     * @param stanza the stanza to send
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    void sendNodeWithNoResponse(Stanza stanza);

    /**
     * Sends a request stanza and blocks until the corresponding response
     * arrives.
     *
     * @apiNote
     * Convenience overload that matches the first inbound stanza carrying
     * the same {@code id} attribute as the outgoing request. Equivalent
     * to calling {@link #sendNode(StanzaBuilder, Function)} with a
     * {@code null} filter.
     *
     * @param node the outgoing request builder
     * @return the response stanza
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    Stanza sendNode(StanzaBuilder node);

    /**
     * Sends a request stanza and blocks the calling virtual thread until a
     * response matching the supplied filter arrives.
     *
     * @apiNote
     * Lowest-level escape hatch for callers that need to dispatch a
     * hand-built request and pick a specific response out of a stream
     * of inbound nodes (for example, a multi-stage device-list
     * exchange or a vendor extension). If the builder has no
     * correlation id one is generated and injected before
     * serialisation so the response matcher always has something to
     * correlate on. The outgoing stanza is also delivered to listeners
     * through
     * {@link LinkedWhatsAppClientListener#onNodeSent(LinkedWhatsAppClient, Stanza)}
     * before this method returns.
     *
     * @param node   the outgoing request builder; may be mutated to
     *               inject an {@code id} attribute
     * @param filter an optional predicate restricting the accepted
     *               responses; {@code null} accepts any response
     * @return the response stanza
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    Stanza sendNode(StanzaBuilder node, Function<Stanza, Boolean> filter);

    /**
     * Dispatches a typed MEX (GraphQL-over-XMPP) request and returns the
     * parsed response stanza.
     *
     * @apiNote
     * Lowest-level entry point for the MEX (GraphQL-over-XMPP)
     * request family: newsletter mutations, business catalog queries,
     * conversation lock toggles, and similar typed operations. All
     * routing metadata (query id, operation name, encoding) is read
     * off the typed request, so callers only assemble the request and
     * let this method handle dispatch and the matching success or
     * failure telemetry.
     *
     * @param request the typed MEX request to dispatch
     * @return the response stanza from the WhatsApp relay
     * @throws NullPointerException            if {@code request} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    Stanza sendNode(MexStanza.Request request);

    /**
     * Dispatches a typed MEX request whose response is discarded while
     * still emitting the round-trip WAM telemetry.
     *
     * @apiNote
     * Use this for MEX mutations whose response carries no payload the
     * caller cares about (for example a newsletter join or leave). The
     * method still blocks on the response and records the same
     * success/failure telemetry as
     * {@link #sendNode(MexStanza.Request)}, minus the value
     * return.
     *
     * @param request the typed MEX request to dispatch
     * @throws NullPointerException            if {@code request} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    void sendNodeWithNoResponse(MexStanza.Request request);

    /**
     * Dispatches a typed {@code http_relay} GraphQL request over HTTP and returns the unwrapped
     * GraphQL {@code data} object.
     *
     * <p>Unlike {@link #sendNode(MexStanza.Request)}, which carries a GraphQL document over the
     * encrypted socket inside a {@code w:mex} stanza, the WhatsApp Web GraphQL transport issues a same-origin
     * {@code POST https://web.whatsapp.com/graphql/} authenticated by the WhatsApp Web browser
     * session. The returned {@link JSONObject} is the GraphQL {@code data} map; callers project it
     * through the matching {@code *WhatsAppWebGraphQlResponse.of(JSONObject)} parser.
     *
     * @apiNote The WhatsApp Web GraphQL transport authenticates with the WhatsApp Web session cookie established by
     * the canonical {@code /auth/token/} exchange (a browser flow) plus the {@code lsd} anti-CSRF
     * token from the page bootstrap. Cobalt is a linked socket client and does not perform that
     * exchange itself, so the caller must supply both out of band; prefer {@link #sendNode(MexStanza.Request)}
     * when an operation is available over MEX.
     *
     * @param request       the typed WhatsApp Web GraphQL request to dispatch
     * @param sessionCookie the {@code Cookie} header value carrying the WhatsApp Web session cookie
     * @param lsdToken      the {@code lsd} anti-CSRF token from the page bootstrap
     * @return the unwrapped GraphQL {@code data} object from the relay
     * @throws NullPointerException           if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports GraphQL
     *                                        errors
     */
    JSONObject sendGraphQl(WhatsAppWebGraphQlOperation.Request request, String sessionCookie, String lsdToken);

    /**
     * Dispatches a typed {@code http_relay} GraphQL request using the WhatsApp Web GraphQL session credentials stored
     * in {@link LinkedWebSessionStore}, and returns the unwrapped GraphQL
     * {@code data} object.
     *
     * <p>This is the convenience form of {@link #sendGraphQl(WhatsAppWebGraphQlOperation.Request, String, String)}:
     * the session cookie and {@code lsd} token are read from the store, where they are placed
     * automatically after a successful connection on a WhatsApp Web client. When no WhatsApp Web GraphQL session is
     * stored the client attempts a {@link #refreshWhatsAppWebGraphQlSession() refresh} once before failing.
     *
     * @apiNote Prefer this form; the WhatsApp Web GraphQL session is established and refreshed for you. Use
     * {@link #sendGraphQl(WhatsAppWebGraphQlOperation.Request, String, String)} only to supply credentials extracted
     * out of band from a browser session.
     *
     * @param request the typed WhatsApp Web GraphQL request to dispatch
     * @return the unwrapped GraphQL {@code data} object from the relay
     * @throws NullPointerException           if {@code request} is {@code null}
     * @throws WhatsAppServerRuntimeException if no WhatsApp Web GraphQL session can be established or the relay reports
     *                                        GraphQL errors
     */
    JSONObject sendGraphQl(WhatsAppWebGraphQlOperation.Request request);

    /**
     * Re-bootstraps the WhatsApp Web GraphQL session credentials and stores them in
     * {@link LinkedWebSessionStore}.
     *
     * <p>Re-runs the canonical {@code /auth/token/} exchange from the durable canonical credentials
     * seeded at pairing, minting a fresh session cookie and {@code lsd} token without re-pairing, and
     * fires {@link LinkedWhatsAppClientListener#onGraphQlSessionChanged}. Invoked automatically after a
     * successful connection on a WhatsApp Web client; also callable directly. Concurrent callers are
     * serialised: the second caller blocks on the first one's outcome rather than racing the exchange.
     *
     * @apiNote Pattern-match the thrown {@link WhatsAppWebGraphQlException} subtype to decide whether to
     * retry the refresh ({@link WhatsAppWebGraphQlException.LsdFetchFailed},
     * {@link WhatsAppWebGraphQlException.ExchangeFailed}) or to surface a fresh-pairing UI
     * ({@link WhatsAppWebGraphQlException.SessionUnseeded}); the messaging channel itself stays usable.
     *
     * @return an {@link Optional} carrying the refreshed {@link WhatsAppWebGraphQlSession}
     * @throws WhatsAppWebGraphQlException.SessionUnseeded if no durable canonical access-token seed is
     *                                                available; the caller must pair on a WhatsApp Web
     *                                                client or call
     *                                                {@link #establishWhatsAppWebGraphQlSession(String, String)}
     *                                                with credentials extracted out of band
     * @throws WhatsAppWebGraphQlException.LsdFetchFailed  if the page-bootstrap HTTP call that yields the
     *                                                {@code lsd} anti-CSRF token raised an underlying
     *                                                transport failure
     * @throws WhatsAppWebGraphQlException.ExchangeFailed  if the canonical {@code /auth/token/} exchange was
     *                                                rejected by the server
     */
    Optional<WhatsAppWebGraphQlSession> refreshWhatsAppWebGraphQlSession();

    /**
     * Supplies the WhatsApp Web GraphQL session credentials used by the {@code http_relay} transport.
     *
     * <p>The WhatsApp Web GraphQL transport authenticates with the WhatsApp Web browser session, not with the
     * already-authenticated socket session: a session cookie deposited by the canonical
     * {@code /auth/token/} exchange paired with the {@code lsd} anti-CSRF token from the page
     * bootstrap. This method records both so every relay-backed method (the business catalog and order
     * queries) can replay them on its {@code POST https://web.whatsapp.com/graphql/}. It must be called
     * before any such method runs; otherwise those methods fail fast with a
     * {@link WhatsAppServerRuntimeException}.
     *
     * <p>Cobalt is a linked socket client and does not acquire these credentials automatically: it
     * performs neither the {@code /auth/token/} HTTP exchange nor the page bootstrap, so the caller
     * supplies the cookie and token out of band.
     *
     * @apiNote Call this once after login, before {@link #queryBusinessCatalog(JidProvider)},
     * {@link #queryBusinessCollections(JidProvider)} or {@link #queryOrder(String, String)}; those
     * methods are the only ones that consume the WhatsApp Web GraphQL session. Re-invoking it replaces the stored
     * credentials, which is the supported way to refresh a stale cookie.
     *
     * @param sessionCookie the {@code Cookie} header value carrying the WhatsApp Web session cookie
     *                      from the {@code /auth/token/} exchange
     * @param lsdToken      the {@code lsd} anti-CSRF token from the page bootstrap
     * @throws NullPointerException if either argument is {@code null}
     */
    void establishWhatsAppWebGraphQlSession(String sessionCookie, String lsdToken);

    /**
     * Dispatches a typed {@code http_comet} GraphQL request to Meta's graph endpoint and returns the
     * unwrapped GraphQL {@code data} object.
     *
     * <p>The Facebook GraphQL transport serves the WhatsApp Business ads and click-to-WhatsApp surface, which is
     * hosted by Meta rather than by a WhatsApp server. The request is a
     * {@code POST https://graph.facebook.com/graphql} authenticated by the Facebook access token
     * minted for the linked account; that token is obtained over the WhatsApp socket through
     * {@link #queryAccessTokenAndSessionCookies(String, JidProvider)}, whose
     * {@link CtwaAccessTokenSession} this method consumes directly.
     *
     * @apiNote Unrelated to core messaging; most embedders never need it. Acquire the
     * {@link CtwaAccessTokenSession} via {@link #queryAccessTokenAndSessionCookies(String, JidProvider)}
     * first, then pass it here.
     *
     * @param request the typed Facebook GraphQL request to dispatch
     * @param session the CTWA access-token session whose Facebook access token authenticates the call
     * @return the unwrapped GraphQL {@code data} object from the graph endpoint
     * @throws NullPointerException           if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the graph endpoint reports
     *                                        GraphQL errors
     */
    JSONObject sendGraphQl(FacebookGraphQlOperation.Request request, CtwaAccessTokenSession session);

    /**
     * Dispatches a typed {@code http_comet} GraphQL request using the Facebook GraphQL session stored in
     * {@link LinkedWebSessionStore}, and returns the unwrapped GraphQL
     * {@code data} object.
     *
     * <p>This is the convenience form of {@link #sendGraphQl(FacebookGraphQlOperation.Request, CtwaAccessTokenSession)}:
     * the Facebook access token is read from the store, where it is placed automatically after a
     * successful connection on a WhatsApp Web client. When no Facebook GraphQL session is stored the client
     * attempts a {@link #refreshFacebookGraphQlSession() silent refresh} once before failing.
     *
     * @apiNote Unrelated to core messaging; most embedders never need it. The Facebook GraphQL session is minted
     * through a Facebook email-recovery handshake; the silent refresh only succeeds once that handshake
     * has been completed at least once via {@link #queryAccessTokenAndSessionCookies(String, JidProvider)}.
     *
     * @param request the typed Facebook GraphQL request to dispatch
     * @return the unwrapped GraphQL {@code data} object from the graph endpoint
     * @throws NullPointerException           if {@code request} is {@code null}
     * @throws WhatsAppServerRuntimeException if no Facebook GraphQL session can be established or the graph
     *                                        endpoint reports GraphQL errors
     */
    JSONObject sendGraphQl(FacebookGraphQlOperation.Request request);

    /**
     * Dispatches a typed {@code graph.whatsapp.com} GraphQL request over HTTP and returns the unwrapped
     * GraphQL {@code data} object.
     *
     * <p>Unlike {@link #sendGraphQl(WhatsAppWebGraphQlOperation.Request)} and
     * {@link #sendGraphQl(FacebookGraphQlOperation.Request)}, which need a browser session cookie or a
     * per-user Facebook access token, this transport is session-independent: it authenticates with a
     * shared, hardcoded app-level access token baked into the release, so it needs neither login nor
     * credentials of its own. WhatsApp Web uses it for reads that must run before or outside a signed-in
     * session (signup and pre-login, guest, public catalog, ML-model, and Meta-AI search). The request
     * is a {@code POST} to a {@code graph.whatsapp.com} endpoint selected by the operation's
     * {@link WhatsAppGraphQlEnvironment environment};
     * the returned {@link JSONObject} is the GraphQL {@code data} map, which callers project through the
     * matching response parser.
     *
     * @apiNote No session is required; the shared app token authenticates the call, so this is safe to
     * dispatch before login. The token is resolved from the operation's environment, except for the
     * {@code GUEST} environment, whose requests must supply an explicit token through
     * {@link WhatsAppGraphQlOperation.Request#accessToken()}.
     *
     * @param request the typed {@code graph.whatsapp.com} GraphQL request to dispatch
     * @return the unwrapped GraphQL {@code data} object from the endpoint
     * @throws NullPointerException           if {@code request} is {@code null}
     * @throws WhatsAppServerRuntimeException if a guest request supplies no access token, the transport
     *                                        fails, or the endpoint reports GraphQL errors
     */
    JSONObject sendGraphQl(WhatsAppGraphQlOperation.Request request);

    /**
     * Refreshes the WhatsApp Business Facebook GraphQL session credentials and stores them in
     * {@link LinkedWebSessionStore}.
     *
     * <p>Drives a four-stage non-interactive refresh: snapshot the current business account nonce,
     * request a silent nonce from the relay, correlate the asynchronous nonce-push notification
     * against the snapshot to lift the fresh value, and trade it through the access-token endpoint
     * for a fresh Facebook access token. On success fires
     * {@link LinkedWhatsAppClientListener#onFacebookGraphQlSessionChanged}. Invoked automatically after a
     * successful connection on a WhatsApp Web client; also callable directly. Concurrent callers are
     * serialised: the second caller blocks on the first one's outcome rather than racing the refresh.
     *
     * @apiNote Pattern-match the thrown {@link WhatsAppFacebookGraphQlException} subtype to decide whether to
     * surface the interactive recovery prompt
     * ({@link WhatsAppFacebookGraphQlException.SilentNonceRecoveryRequired#emailMask()}), retry with backoff
     * ({@link WhatsAppFacebookGraphQlException.SilentNonceServerError},
     * {@link WhatsAppFacebookGraphQlException.SilentNonceTimeout}), or abandon the refresh and seed a fresh
     * session ({@link WhatsAppFacebookGraphQlException.SessionUnseeded},
     * {@link WhatsAppFacebookGraphQlException.TokenExchangeFailed}); the messaging channel itself stays usable.
     *
     * @return an {@link Optional} carrying the refreshed {@link CtwaAccessTokenSession}
     * @throws WhatsAppFacebookGraphQlException.SessionUnseeded             if no account jid is available to
     *                                                            scope the refresh against; the
     *                                                            caller must first run the
     *                                                            interactive recovery flow via
     *                                                            {@link #queryAccessTokenAndSessionCookies(String, JidProvider)}
     * @throws WhatsAppFacebookGraphQlException.SilentNonceRecoveryRequired if the relay refused the silent path
     *                                                            and dispatched a recovery email the
     *                                                            user must confirm before retrying
     * @throws WhatsAppFacebookGraphQlException.SilentNonceClientError      if the relay rejected the silent-nonce
     *                                                            request with a documented {@code 4xx}
     *                                                            error envelope
     * @throws WhatsAppFacebookGraphQlException.SilentNonceServerError      if the relay returned a transient
     *                                                            {@code 5xx} failure or the reply was
     *                                                            unparseable
     * @throws WhatsAppFacebookGraphQlException.SilentNonceTimeout          if the granted silent-nonce push did
     *                                                            not arrive within the configured wait
     *                                                            window
     * @throws WhatsAppFacebookGraphQlException.TokenExchangeFailed         if the downstream call that trades
     *                                                            the fresh nonce for a Facebook access
     *                                                            token returned no session
     */
    Optional<CtwaAccessTokenSession> refreshFacebookGraphQlSession();

    /**
     * Dispatches a typed SMAX request and returns the parsed response
     * stanza.
     *
     * @apiNote
     * Lowest-level entry point for the SMAX RPC family, modelled as
     * typed {@link SmaxStanza.Request} values. Use this for
     * endpoints whose response carries data the caller consumes (for
     * example call-link queries); use
     * {@link #sendNodeWithNoResponse(SmaxStanza.Request)} for
     * fire-and-forget mutations whose payload is ignored.
     *
     * @param request the typed SMAX request to dispatch
     * @return the inbound response stanza
     * @throws NullPointerException            if {@code request} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    Stanza sendNode(SmaxStanza.Request request);

    /**
     * Dispatches a typed SMAX request whose response is discarded.
     *
     * @apiNote
     * Use this for fire-and-forget SMAX RPCs where the side effect of
     * sending the request is the whole contract and the response
     * carries no payload the caller cares about. Compare
     * {@link #sendNode(SmaxStanza.Request)} for the value-returning
     * variant.
     *
     * @param request the typed SMAX request to dispatch
     * @throws NullPointerException            if {@code request} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    void sendNodeWithNoResponse(SmaxStanza.Request request);

    /**
     * Dispatches a USync query and returns the parsed result.
     *
     * @apiNote
     * Canonical entry point for USync operations: contact sync,
     * device-list lookup, status-privacy contact resolution, bot
     * profile fetch, and similar batched lookups. The query carries
     * its own protocol mix and user list; this method honours any
     * active backoff window the server has signalled for those
     * protocols, dispatches the request, parses the response, and
     * records any returned backoff hint so subsequent queries observe
     * the rate limit. Concurrent calls from different threads are
     * supported provided each call uses its own {@link UsyncQuery}
     * instance.
     *
     * @param query the query; must not be shared across threads while
     *              still being configured
     * @return the parsed result
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     * @throws InterruptedException            if the thread is
     *                                         interrupted while waiting
     *                                         for an active backoff to
     *                                         elapse
     */
    UsyncResult sendNode(UsyncQuery query) throws InterruptedException;

    /**
     * Disconnects this client from the WhatsApp servers, preserving the
     * persisted credentials for future reconnections.
     *
     * @apiNote
     * Convenience shorthand for
     * {@link #disconnect(WhatsAppClientDisconnectReason)
     * disconnect(DISCONNECTED)}. Use this when the embedder wants to
     * release the socket without surrendering the pairing; the next
     * {@link #connect()} resumes without a fresh QR scan.
     */
    void disconnect();

    /**
     * Disconnects and immediately re-establishes the connection.
     *
     * @apiNote
     * Convenience shorthand for
     * {@link #disconnect(WhatsAppClientDisconnectReason)
     * disconnect(RECONNECTING)}. Use this to recover from a transient
     * network issue, force a fresh handshake, or apply a credential
     * change that takes effect only on the next login. The
     * reconnect-reason disconnect notification fires once, then a new
     * login flow runs end to end and listeners observe a fresh
     * {@link LinkedWhatsAppClientListener#onLoggedIn(WhatsAppClient)}.
     *
     * @return this client
     */
    LinkedWhatsAppClient reconnect();

    /**
     * Logs this companion out of WhatsApp, invalidating the persisted
     * credentials for this session.
     *
     * @apiNote
     * Performs the same "Log out" action a user invokes from the
     * Linked Devices menu: the primary device detaches this companion
     * and the persisted credentials are discarded. After this returns,
     * the next {@link #connect()} requires a fresh authentication
     * ceremony (QR scan, pairing code, or phone-number registration).
     * Sessions that have not yet learned their own JID short-circuit
     * to a local {@link #disconnect(WhatsAppClientDisconnectReason)}
     * with {@link WhatsAppClientDisconnectReason#LOGGED_OUT}. To
     * detach a different linked device while staying connected, use
     * {@link #logoutCompanion(JidProvider)} instead.
     */
    void logout();

    /**
     * Detaches the given companion device from this account.
     *
     * @apiNote
     * Drives the "Log out" affordance shown against another linked
     * device in the Linked Devices panel: the primary device detaches
     * the supplied companion while leaving this session connected. The
     * companion must belong to this account; the server rejects JIDs
     * that do not appear in the caller's own device list. To log out
     * the currently-connected companion itself, use {@link #logout()}.
     *
     * @param companion the companion JID to detach; must include an
     *                  agent index (device slot) and must be a device
     *                  JID belonging to this account
     * @throws NullPointerException            if {@code companion} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    void logoutCompanion(JidProvider companion);

    /**
     * Reconciles the local view of the Linked Devices panel with the
     * server.
     *
     * @apiNote
     * Use after pairing or unpairing a companion from another device,
     * or whenever the Linked Devices settings surface should redraw
     * against an authoritative copy. The primary device (slot 0)
     * appears first; companions follow in server order. The new list
     * replaces {@link LinkedWhatsAppStore#linkedDevices()} and
     * {@link LinkedWhatsAppClientListener#onLinkedDevices} fires with the
     * new authoritative set.
     *
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    void refreshLinkedDevices();

    /**
     * Returns whether a live socket to the WhatsApp servers is currently
     * open.
     *
     * @apiNote
     * Reports the transport-level state only: returns {@code true}
     * between the moment {@link #connect()} brings the socket up and
     * the moment {@link #disconnect()} or a remote close tears it down.
     * It does not gate on login or pairing state; for those, observe
     * the {@link LinkedWhatsAppClientListener} callbacks.
     *
     * @return {@code true} if the socket is open and the handshake has
     *         not been torn down, {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Blocks the calling thread until this session is disconnected.
     *
     * @apiNote
     * Use this as the canonical "park the main thread" idiom in
     * applications whose lifetime is bounded by a single WhatsApp
     * session. The wait completes only when
     * {@link LinkedWhatsAppClientListener#onDisconnected(LinkedWhatsAppClient, WhatsAppClientDisconnectReason)}
     * fires with a reason other than
     * {@link WhatsAppClientDisconnectReason#RECONNECTING}, so transient
     * reconnect cycles do not wake the caller and a long-running app
     * stays parked across them.
     *
     * @return {@code this}, for fluent chaining
     */
    LinkedWhatsAppClient waitForDisconnection();

    /**
     * Routes a session-fatal failure through the configured
     * {@link WhatsAppLinkedClientErrorHandler} and applies its decision.
     *
     * @apiNote
     * The central choke point for every error that bubbles out of the
     * socket layer, a sync round trip, or any other in-flight
     * operation. The handler's returned
     * {@link WhatsAppLinkedClientErrorResult} is mapped to a
     * concrete session-control action: discard, disconnect, reconnect,
     * log out, or ban. App-state fatal failures are additionally
     * mirrored to the telemetry pipeline so dashboards can correlate
     * the error with the failing sync collection. Embedders normally
     * do not call this directly; library internals invoke it when they
     * raise a {@link WhatsAppException}.
     *
     * @param exception the exception to handle
     */
    void handleFailure(WhatsAppException exception);

    /**
     * Hands a batch of outgoing sync mutations to the companion
     * app-state pipeline for upload.
     *
     * @apiNote
     * Every mutation that needs to propagate to other linked devices
     * (archive, mute, pin, label, quick-reply add or edit, etc.)
     * enters the app-state pipeline through this method. The patches
     * are grouped per collection, encrypted, uploaded under the
     * appropriate {@link SyncPatchType} version, and finally
     * re-applied locally so the optimistic store update is reconciled
     * with the server's authoritative version. Embedders only call
     * this when driving a sync action that has no dedicated
     * client-method wrapper; the typed action wrappers (archive, mute,
     * etc.) invoke it internally.
     *
     * @param type    the sync patch type being updated
     * @param patches the ordered mutations to apply
     */
    void pushWebAppState(SyncPatchType type, List<SyncPendingMutation> patches);

    /**
     * Pulls the latest server patches for the given collections and
     * reports whether any state actually changed.
     *
     * @apiNote
     * Used both during initial post-login bootstrap and as the manual
     * reconciliation hook the server signals when an app-state
     * dirty-bit notification arrives. The boolean return distinguishes
     * a no-op sync (no patches, no snapshot) from one that actually
     * applied remote updates; embedders that surface dirty-bit
     * telemetry use it to detect false-positive notifications. Other
     * callers may ignore the return value.
     *
     * @param patches the patch types to pull; an empty array is
     *                tolerated
     * @return {@code true} if any synced collection had patches or a
     *         snapshot, {@code false} when every collection sync
     *         completed without applying any state changes or when
     *         {@code patches} is empty
     */
    boolean pullWebAppState(SyncPatchType... patches);

    /**
     * Acknowledges an inbound stanza using its own {@code id} attribute
     * as the ack id.
     *
     * @apiNote
     * Convenience over {@link #sendAck(String, Stanza)} for callers
     * that need to confirm receipt of a stanza without substituting
     * their own correlation id. The library invokes this internally
     * on every received message, receipt, IQ result, and notification
     * so the server stops retransmitting.
     *
     * @param stanza the inbound stanza to acknowledge
     */
    void sendAck(Stanza stanza);

    /**
     * Generates and uploads a fresh batch of Signal pre-keys so remote
     * devices can establish new end-to-end sessions with this client.
     *
     * @apiNote
     * Replenishes the server-side pre-key bundle that remote devices
     * use to establish new end-to-end sessions. The post-login
     * bootstrap calls this with the full initial budget; later top-ups
     * happen whenever the server announces the bundle is running low.
     * The requested count is clamped upward so a degenerate small
     * request still ships a useful batch; on success the generated
     * keys are appended to the store so they can be reused when the
     * server hands the bundle back during sender-key fan-out.
     *
     * @param keysCount the number of additional pre-keys to generate
     *                  and upload; internally clamped upward
     */
    void sendPreKeys(long keysCount);

    /**
     * Sends a delivery, read, or played receipt for a single message
     * id.
     *
     * @apiNote
     * Per-message receipt entry point: most embedders only need to
     * mark one message at a time and this helper covers that case
     * directly. The {@code type} accepts the standard receipt names
     * ({@code "read"}, {@code "played"}, {@code "read-self"}, etc.);
     * pass {@code null} for a plain delivery receipt. The call is a
     * no-op when the client does not yet know its own JID so the
     * early-bootstrap window cannot leak unauthenticated receipts
     * during pairing.
     *
     * @param id   the message id to acknowledge
     * @param from the JID of the remote party to receipt
     * @param type the receipt type (for example {@code "read"} or
     *             {@code "played"}); {@code null} for a delivery
     *             receipt
     */
    void sendReceipt(String id, JidProvider from, String type);

    /**
     * Queries the server-side metadata of a group or community chat.
     *
     * @apiNote
     * Drives the panel that opens when a user taps a group or
     * community header: subject, description, creation timestamp,
     * participant list with admin flags, ephemeral-message setting,
     * and (for communities) the linked child groups. Embedders call
     * this on demand rather than prefetching on every chat switch.
     *
     * @param chat the target group or community
     * @return the parsed metadata
     * @throws IllegalArgumentException        if the JID is not a group
     *                                         or community
     * @throws NoSuchElementException          if the server response is
     *                                         malformed
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    ChatMetadata queryChatMetadata(JidProvider chat);

    /**
     * Queries the WhatsApp Business profile published by the given
     * contact.
     *
     * @apiNote
     * Drives the business-info pane that opens when a user taps a
     * merchant chat header: address, business hours, website list,
     * cover photo id, declared categories, optional cart toggle, and
     * so on. The server returns the full profile when the merchant
     * has one and signals an empty profile when the contact is not a
     * business; the latter surfaces as an empty {@link Optional}.
     *
     * @param contact the contact whose business profile should be
     *                fetched
     * @return the parsed profile, or {@link Optional#empty()} if the
     *         contact is not a business
     * @throws NullPointerException            if {@code contact} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    Optional<BusinessProfile> queryBusinessProfile(JidProvider contact);

    /**
     * Parses a single {@code <category>} stanza into a
     * {@link BusinessCategory}.
     *
     * @apiNote
     * Helper used by every code path that consumes a business-category
     * payload directly off the wire (business-profile responses,
     * category typeahead responses, catalog product nodes). It
     * URL-decodes the human-readable name so callers receive the
     * localized text verbatim. Embedders rarely call this directly; it
     * is exposed so extensions parsing custom stanzas can reuse the
     * same logic.
     *
     * @param stanza the {@code category} stanza
     * @return the parsed category
     * @throws NoSuchElementException if the category content is missing
     */
    BusinessCategory parseBusinessCategory(Stanza stanza);

    /**
     * Persists an updated WhatsApp Business profile for the
     * authenticated user.
     *
     * @apiNote
     * Writer counterpart to {@link #queryBusinessProfile(JidProvider)}:
     * drives the merchant's "Edit business info" form. Every field on
     * the supplied {@link BusinessProfile} that is non-{@code null} is
     * sent as a delta mutation; absent fields are left untouched
     * server-side. Embedders pass a profile carrying only the fields
     * they want to change.
     *
     * @param profile the new business-profile metadata
     * @throws NullPointerException            if {@code profile} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editBusinessProfile(BusinessProfile profile);

    /**
     * Enables the in-chat shopping cart feature on the authenticated
     * user's business profile.
     *
     * @apiNote
     * Drives the "Shopping cart" toggle inside the business profile
     * settings: enabling it lets customers compose a multi-product
     * order inside the chat before sending it through to the merchant.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #disableBusinessCart()
     */
    void enableBusinessCart();

    /**
     * Disables the in-chat shopping cart feature on the authenticated
     * user's business profile.
     *
     * @apiNote
     * Drives the "Shopping cart" toggle inside the business profile
     * settings: disabling it stops customers from composing a
     * multi-product order inside the chat.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #enableBusinessCart()
     */
    void disableBusinessCart();

    /**
     * Removes the current cover photo from the authenticated user's
     * business profile.
     *
     * @apiNote
     * Drives the "Remove cover photo" action in the business profile
     * editor: the merchant strips the masthead image and the profile
     * falls back to the platform default cover.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void deleteBusinessCoverPhoto();

    /**
     * Fetches the long-lived public verification certificate a WhatsApp
     * Business account publishes to its catalog clients.
     *
     * @apiNote
     * Business accounts sign sensitive catalog payloads (the merchant
     * phone-number envelope, address-verification responses, and
     * direct-connection-encrypted payloads attached to product orders)
     * with a server-issued certificate. This call returns the PEM
     * that backs those signatures so a Cobalt-driven storefront can
     * prove the payload it received actually came from the advertised
     * business and was not relayed through an impersonating peer.
     * Typical use: fetch once on first interaction with a business,
     * cache for the merchant's lifetime, and re-validate every signed
     * catalog response against it. Pair with
     * {@link #queryBusinessSignedUserInfo(JidProvider)} to obtain the
     * matching signed phone-number envelope.
     *
     * @param businessJid the JID of the business whose certificate is
     *                    being fetched
     * @return the PEM string when the server returned a certificate, or
     *         {@link Optional#empty()} when the server replied with no
     *         certificate (typically because the business has not been
     *         provisioned with one yet)
     * @throws NullPointerException            if {@code businessJid} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    Optional<String> queryBusinessPublicKey(JidProvider businessJid);

    /**
     * Fetches the cryptographically signed phone-number envelope a
     * WhatsApp Business account exposes to its catalog clients.
     *
     * @apiNote
     * The envelope binds the business JID to its merchant phone
     * number and notional business domain, all signed by the
     * business's private key. Callers verify the signature with the
     * matching certificate returned by
     * {@link #queryBusinessPublicKey(JidProvider)} to prove the
     * displayed contact details actually belong to the business they
     * are talking to. The {@code ttl_timestamp} carried alongside is
     * the server-recommended expiry after which clients should
     * re-fetch. Most fields are {@link Optional} because the server
     * omits the entire envelope (and any individual sub-field) when
     * the business has not been provisioned with a signed identity;
     * applications should treat an all-empty result as "no verifiable
     * identity available" rather than a hard error.
     *
     * @param businessJid the JID of the business whose signed
     *                    phone-number envelope should be fetched
     * @return the parsed envelope, with each field exposed as an
     *         {@link Optional} since the server may omit any or all of
     *         them
     * @throws NullPointerException            if {@code businessJid} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    BusinessSignedUserInfo queryBusinessSignedUserInfo(JidProvider businessJid);

    /**
     * Fetches the legal-entity disclosure one or more WhatsApp
     * Business accounts publish to their customers.
     *
     * @apiNote
     * Several jurisdictions (most notably India under the IT Rules
     * 2021) require business accounts to register the legal name of
     * the operating entity, the entity type, and dedicated
     * customer-care and grievance-officer contact channels. WhatsApp
     * surfaces those fields in the in-app "Business info" panel; this
     * call returns the same data so a Cobalt-driven catalog browser
     * can render the disclosure before letting the user place an
     * order, or a compliance dashboard can audit the merchant's
     * registration status. The call is batched: one round trip
     * resolves the disclosure for every JID, with each result returned
     * in the same order as the input list. Businesses that have not
     * registered compliance information come back with empty or
     * default-valued fields rather than being dropped.
     *
     * @param jids the business JIDs whose compliance metadata should
     *             be fetched
     * @return the parsed compliance entries in server order
     * @throws NullPointerException            if {@code jids} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code jids} is empty
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    List<BusinessMerchantCompliance> queryMerchantCompliance(List<? extends JidProvider> jids);

    /**
     * Publishes or updates the legal-entity disclosure shown to
     * customers on the authenticated business account.
     *
     * @apiNote
     * Writer counterpart to
     * {@link #queryMerchantCompliance(List)}: overwrites the registered
     * entity name, type, custom type description, registration flag,
     * and contact channels (customer care and grievance officer) shown
     * in the business profile's compliance panel. The disclosure is
     * server persisted, propagates to every linked device, and becomes
     * visible to anyone querying the same business JID. Every textual
     * field on the {@link MerchantComplianceEdit} is nullable: a
     * non-{@code null} value overwrites the server-side state, a
     * {@code null} leaves the existing value untouched. The server
     * echoes the post-update state back, which the method returns so
     * callers confirm what was actually persisted without an extra
     * round trip.
     *
     * @param edit the disclosure changes to apply
     * @return the merchant-compliance entry the server returned after
     *         applying the update
     * @throws NullPointerException            if {@code edit} is
     *                                         {@code null}
     * @throws NoSuchElementException          if the response carries
     *                                         no merchant-compliance
     *                                         data
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    BusinessMerchantCompliance editMerchantCompliance(MerchantComplianceEdit edit);

    /**
     * Searches the WhatsApp Business category catalog for entries whose
     * localized name matches the given partial query.
     *
     * @apiNote
     * Every WhatsApp Business profile must declare a category
     * ({@code "Restaurant"}, {@code "Clothing store"}, etc.) used both
     * in the in-app business info panel and for discoverability. This
     * autocomplete endpoint backs the onboarding "Business category"
     * picker: the server returns the localized categories whose name
     * matches the typed prefix. Embedders driving a merchant
     * onboarding flow call this on each keystroke. The result also
     * surfaces a "not a business" placeholder id: categories that
     * should be presented as the sentinel option carry their
     * {@code notABiz} flag set so the caller renders them distinctly.
     *
     * @param query the partial text the user has typed
     * @return the matched categories with their server-issued id,
     *         localized display name, and "not a business" flag
     * @throws NullPointerException            if {@code query} is
     *                                         {@code null}
     * @throws NoSuchElementException          if the server reply is
     *                                         malformed
     * @throws WhatsAppSessionException.Closed if the socket is no
     *                                         longer open
     */
    BusinessCategoryTypeahead queryBusinessCategoryTypeahead(String query);

    /**
     * Fetches the full detail of a business order identified by a
     * message id and a server-issued token.
     *
     * @apiNote
     * Drives the order-detail panel a recipient sees after tapping
     * through an order message to view the cart: creation timestamp,
     * currency, subtotal, total, and the list of ordered products with
     * quantities and per-line prices. The sensitive token shipped
     * alongside the order message authenticates the request, so a
     * third party that only knows the order id cannot enumerate the
     * cart. The result is projected into a {@link BusinessOrder} value.
     *
     * @param messageId   the server-issued order id (typically the id
     *                    of the {@code OrderMessage})
     * @param tokenBase64 the sensitive base64-encoded token shipped
     *                    with the order message
     * @return the parsed order, or {@link Optional#empty()} when the
     *         relay returns no order payload
     * @throws NullPointerException            if {@code messageId} or
     *                                         {@code tokenBase64} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<BusinessOrder> queryOrder(String messageId, String tokenBase64);

    /**
     * Creates a new quick reply template and propagates it to every
     * linked device through app-state sync.
     *
     * @apiNote
     * Drives the "Add quick reply" affordance inside the Business
     * Tools menu: the user picks a short shortcut ({@code "/menu"})
     * and a longer canned response ({@code "Here is our menu: ..."}),
     * with optional keyword tags driving the autocomplete surface. A
     * random client-generated id is minted for the new template; it
     * serves as the primary key under which the entry is filed in the
     * local store. The store is updated eagerly so the caller observes
     * the new template immediately, without waiting for the round trip
     * through the server and the inbound sync patch.
     *
     * @param create the new template content
     * @return the newly minted quick reply id
     * @throws NullPointerException            if {@code create} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    String createQuickReply(QuickReplyCreate create);

    /**
     * Replaces the content of an existing quick reply template and propagates the change through app-state sync.
     *
     * @apiNote
     * Drives the "Edit quick reply" affordance in the Business Tools
     * menu: the new shortcut, message body, and keyword list overwrite
     * the existing entry under the supplied id, and a quick-reply
     * sync patch is pushed so every linked device picks up the
     * change. The call returns {@link Optional#empty()} when the id is
     * unknown locally; the method does not synthesise a placeholder
     * entry for an id the local store has never seen.
     *
     * @param edit the new content keyed by the existing template id; never {@code null}
     * @return the pre-edit {@link QuickReply} snapshot, or {@link Optional#empty()} when no template with that id existed locally
     * @throws NullPointerException            if {@code edit} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<QuickReply> editQuickReply(QuickReplyEdit edit);

    /**
     * Removes a quick reply template and propagates the removal through app-state sync.
     *
     * @apiNote
     * Drives the "Delete quick reply" affordance in the Business
     * Tools menu: a removal mutation is pushed against the quick-reply
     * collection so every linked device drops the entry, and the
     * local store is updated eagerly so the caller observes the
     * deletion immediately.
     *
     * @param quickReplyId the id of the quick reply to delete; never {@code null}
     * @return the removed {@link QuickReply} as it existed before deletion, or {@link Optional#empty()} when no template with that id existed locally
     * @throws NullPointerException            if {@code quickReplyId} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<QuickReply> deleteQuickReply(String quickReplyId);

    /**
     * Queries the public profile of a Meta AI or third-party bot for its default persona.
     *
     * @apiNote
     * Single-argument convenience for
     * {@link #queryBotProfile(JidProvider, String)} that omits the
     * persona selector so the server returns the bot's default
     * persona (the one shown in the bot's chat header).
     *
     * @param botJid the bot JID to query; never {@code null}
     * @return the parsed {@link BotProfile}, or {@link Optional#empty()} when the server returns no profile for the bot
     * @throws NullPointerException            if {@code botJid} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #queryBotProfile(JidProvider, String)
     */
    Optional<BotProfile> queryBotProfile(JidProvider botJid);

    /**
     * Queries the public profile of a Meta AI or third-party bot for a specific persona.
     *
     * @apiNote
     * Drives the bot info header rendered at the top of a bot chat:
     * display name, attributes blob, description, registered slash
     * commands, suggested prompts, and classification flags
     * (Meta-created, creator name and link, professional-status type).
     * The supplied {@code personaId} selects a specific persona;
     * passing {@code null} requests the bot's default persona.
     *
     * @param botJid    the bot JID to query; never {@code null}
     * @param personaId the persona id, or {@code null} to omit the {@code persona_id} attribute and request the default persona
     * @return the parsed {@link BotProfile}, or {@link Optional#empty()} when the server returns no profile for the bot
     * @throws NullPointerException            if {@code botJid} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<BotProfile> queryBotProfile(JidProvider botJid, String personaId);

    /**
     * Places a voice or video call to {@code target} carrying the supplied media streams and returns a
     * live session, routing to a one-to-one or a group call based on {@code target}.
     *
     * <p>When {@code target} is a user JID this places a one-to-one call addressed at the callee's
     * device list. When {@code target} is a group or community JID this initiates a group call: the
     * participants are resolved by querying the group's metadata through
     * {@link #queryGroupInfo(JidProvider)} and taking every member other than this account, so the
     * caller never enumerates them, and the offer is addressed at the group rather than at a peer.
     *
     * <p>The call is a video call when {@code videoOut} is non-{@code null} and audio-only otherwise.
     * The two outbound sources supply the local audio and video the call transmits, and the two inbound
     * sinks receive the remote audio and video; the streams are owned by the call engine and ended
     * automatically when the call ends, so the application never closes them.
     *
     * @apiNote
     * Drives the "Call" / "Video call" affordance in a user or group chat header: sends a call offer
     * addressed at the callee's device list (one-to-one) or at the group with its participant device
     * list inlined (group), registers the resulting {@link Call} in the in-flight call store, and
     * returns it to the caller so further signalling (mute, video state, termination) can be driven
     * through the session handle. Supply the streams with the stream factories such as
     * {@link AudioOutput#fromMicrophone()} and {@link AudioInput#toSpeaker()}, or pump frames
     * yourself for a call-to-call bridge or a bot. For an audio-only call prefer the
     * {@link #startCall(JidProvider, AudioOutput, AudioInput)} overload.
     *
     * @param target   the JID of the callee (a user JID for a one-to-one call) or of the group or
     *                 community to call (a group or community JID for a group call)
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission, or {@code null}
     *                 for an audio-only call
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} for an
     *                 audio-only call
     * @return the live {@link Call} session bound to the negotiated call id
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only
     *                                         supported on the WhatsApp Web flavour
     * @throws NullPointerException            if {@code target}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code target} is a group or community JID whose
     *                                         metadata cannot be resolved or that has no member other
     *                                         than this account to call
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #startCall(JidProvider, AudioOutput, AudioInput)
     * @see #queryGroupInfo(JidProvider)
     * @see #addCallParticipants(Call, Collection)
     * @see #removeCallParticipants(Call, Collection)
     * @see #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)
     * @see #terminateCall(Call, CallEndReason)
     */
    Call startCall(JidProvider target, AudioOutput audioOut, AudioInput audioIn,
                   VideoOutput videoOut, VideoInput videoIn);

    /**
     * Places an audio-only call to {@code target} carrying the supplied audio streams and returns a
     * live session, routing to a one-to-one or a group call based on {@code target}.
     *
     * @apiNote
     * Convenience for {@link #startCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * with no video; the answered leg can still be upgraded to video later.
     *
     * @param target   the JID of the callee (a user JID) or of the group or community to call (a group
     *                 or community JID)
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @return the live {@link Call} session bound to the negotiated call id
     * @throws NullPointerException            if {@code target}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code target} is a group or community JID whose
     *                                         metadata cannot be resolved or that has no member other
     *                                         than this account to call
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call startCall(JidProvider target, AudioOutput audioOut, AudioInput audioIn);

    /**
     * Places a call to {@code target} that draws both the audio and the video from a single video source
     * and a single video sink, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #startCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)} that
     * passes {@code videoOut} as both the audio and video source and {@code videoIn} as both the audio and
     * video sink, for a caller whose video device already carries the audio.
     *
     * @param target   the JID of the callee (a user JID) or of the group or community to call
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the negotiated call id
     * @throws NullPointerException            if {@code target}, {@code videoOut}, or {@code videoIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code target} is a group or community JID whose metadata
     *                                         cannot be resolved or that has no member other than this
     *                                         account to call
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call startCall(JidProvider target, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Places a call to {@code target} that draws both the audio and the video input from a single video
     * sink while transmitting from separate audio and video sources, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #startCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)} that
     * passes {@code videoIn} as both the audio and video sink, for a caller whose video device also renders
     * the remote audio.
     *
     * @param target   the JID of the callee (a user JID) or of the group or community to call
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the negotiated call id
     * @throws NullPointerException            if {@code target}, {@code audioOut}, {@code videoOut}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code target} is a group or community JID whose metadata
     *                                         cannot be resolved or that has no member other than this
     *                                         account to call
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call startCall(JidProvider target, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Places a call to {@code target} that draws both the audio and the video output from a single video
     * source while receiving into separate audio and video sinks, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #startCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)} that
     * passes {@code videoOut} as both the audio and video source, for a caller whose video device also
     * captures the local audio.
     *
     * @param target   the JID of the callee (a user JID) or of the group or community to call
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoIn  the sink the engine fills with received remote video; never {@code null}
     * @return the live {@link Call} session bound to the negotiated call id
     * @throws NullPointerException            if {@code target}, {@code videoOut}, {@code audioIn}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code target} is a group or community JID whose metadata
     *                                         cannot be resolved or that has no member other than this
     *                                         account to call
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call startCall(JidProvider target, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn);

    /**
     * Accepts a pending {@link IncomingCall} offer with the supplied media streams and returns a live
     * session.
     *
     * <p>The answered leg is a video call when {@code videoOut} is non-{@code null} and audio-only
     * otherwise. The two outbound sources supply the local audio and video the call transmits, and the two
     * inbound sinks receive the remote audio and video; the streams are owned by the call engine and
     * ended automatically when the call ends, so the application never closes them.
     *
     * @apiNote
     * Drives the green "Accept" button on the incoming-call sheet: marks the offer as responded so a
     * later reject or terminate becomes a no-op, accepts the call, and parks the resulting session in
     * the connecting state until transport setup completes. Passing {@code null} {@code videoOut}
     * answers an offered video call as audio-only; an audio-only offer cannot be upgraded to video here,
     * so for that place a fresh video call through
     * {@link #startCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)}.
     * For an audio-only answer prefer the {@link #acceptCall(IncomingCall, AudioOutput, AudioInput)}
     * overload.
     *
     * @param offer    the incoming offer to accept; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission, or {@code null}
     *                 to answer audio-only
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} to answer
     *                 audio-only
     * @return the live {@link Call} session for the accepted call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only
     *                                         supported on the WhatsApp Web flavour
     * @throws NullPointerException            if {@code offer}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalStateException           if the offer has already been responded to
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call acceptCall(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn,
                    VideoOutput videoOut, VideoInput videoIn);

    /**
     * Accepts a pending {@link IncomingCall} offer as audio-only with the supplied audio streams and
     * returns a live session.
     *
     * @apiNote
     * Convenience for {@link #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * with no video, answering even an offered video call audio-only.
     *
     * @param offer    the incoming offer to accept; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @return the live {@link Call} session for the accepted call
     * @throws NullPointerException            if {@code offer}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalStateException           if the offer has already been responded to
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call acceptCall(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn);

    /**
     * Accepts a pending {@link IncomingCall} offer that draws both the audio and the video from a single
     * video source and a single video sink, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoOut} as both the audio and video source and {@code videoIn} as both the audio
     * and video sink, for a caller whose video device already carries the audio.
     *
     * @param offer    the incoming offer to accept; never {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session for the accepted call
     * @throws NullPointerException            if {@code offer}, {@code videoOut}, or {@code videoIn} is
     *                                         {@code null}
     * @throws IllegalStateException           if the offer has already been responded to
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call acceptCall(IncomingCall offer, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Accepts a pending {@link IncomingCall} offer that draws both the audio and the video input from a
     * single video sink while transmitting from separate audio and video sources, and returns a live
     * session.
     *
     * @apiNote
     * Convenience for {@link #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoIn} as both the audio and video sink, for a caller whose video device also
     * renders the remote audio.
     *
     * @param offer    the incoming offer to accept; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session for the accepted call
     * @throws NullPointerException            if {@code offer}, {@code audioOut}, {@code videoOut}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalStateException           if the offer has already been responded to
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call acceptCall(IncomingCall offer, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Accepts a pending {@link IncomingCall} offer that draws both the audio and the video output from a
     * single video source while receiving into separate audio and video sinks, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoOut} as both the audio and video source, for a caller whose video device also
     * captures the local audio.
     *
     * @param offer    the incoming offer to accept; never {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoIn  the sink the engine fills with received remote video; never {@code null}
     * @return the live {@link Call} session for the accepted call
     * @throws NullPointerException            if {@code offer}, {@code videoOut}, {@code audioIn}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalStateException           if the offer has already been responded to
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call acceptCall(IncomingCall offer, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn);

    /**
     * Rejects a pending {@link IncomingCall} offer with the supplied end-call reason.
     *
     * @apiNote
     * Drives the red "Decline" button on the incoming-call sheet:
     * marks the offer as responded so a subsequent accept or
     * terminate becomes a no-op, signals the rejection with the
     * supplied {@link CallEndReason}, drops the offer from the call
     * store, and fires {@link LinkedWhatsAppClientListener#onCallEnded} on
     * every registered listener so the UI can dismiss its ringtone
     * and dialog.
     *
     * @param offer  the incoming offer to reject; never {@code null}
     * @param reason the reason to communicate to the peer; placed on the {@code reason} attribute as {@link CallEndReason#wireValue()}
     * @throws NullPointerException            if {@code offer} or {@code reason} is {@code null}
     * @throws IllegalStateException           if the offer has already been responded to
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    void rejectCall(IncomingCall offer, CallEndReason reason);

    /**
     * Terminates an in-progress call with the supplied reason.
     *
     * @apiNote
     * Drives every voluntary call-end path (hang up, timeout, mic
     * permission denial, blocked-by-callee, network lost, etc.): looks
     * up the call peer from the in-flight call store keyed by
     * {@code callId}, then signals call termination to the peer with
     * the chosen {@link CallEndReason}.
     *
     * @param callId the identifier of the call to terminate; never {@code null}
     * @param reason the end-call reason; placed on the {@code reason} attribute as {@link CallEndReason#wireValue()}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws NoSuchElementException          if no call with the given id is cached in the store
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #terminateCall(Call, CallEndReason)
     */
    void terminateCall(String callId, CallEndReason reason);

    /**
     * Terminates an in-progress call, taking the call id and peer JID directly from the supplied {@link Call}.
     *
     * @apiNote
     * Use this when the caller already holds the live
     * {@link Call} handle returned by
     * {@link #startCall(JidProvider, AudioOutput, AudioInput)} or
     * {@link #acceptCall(IncomingCall, AudioOutput, AudioInput)}, so the store
     * lookup performed by {@link #terminateCall(String, CallEndReason)}
     * is unnecessary.
     *
     * @param call   the in-progress call to terminate; never {@code null}
     * @param reason the end-call reason; placed on the {@code reason} attribute as {@link CallEndReason#wireValue()}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #terminateCall(String, CallEndReason)
     */
    void terminateCall(Call call, CallEndReason reason);

    /**
     * Announces to the relay that this device has consumed an incoming offer and is ringing the local user.
     *
     * @apiNote
     * Issues the preaccept signal that sits between the inbound offer
     * and the eventual accept, telling the relay that this device
     * passed receive-side validation and is now alerting the user.
     * The peer is looked up out of the in-flight call store using
     * {@code callId}.
     *
     * @param callId the identifier carried by the original offer; never {@code null}
     * @throws NullPointerException            if {@code callId} is {@code null}
     * @throws NoSuchElementException          if no call with the given id is cached in the store
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #preacceptCall(IncomingCall)
     */
    void preacceptCall(String callId);

    /**
     * Announces preaccept on an incoming offer, taking the call id and peer JID directly from the supplied {@link IncomingCall}.
     *
     * @apiNote
     * Use this when the caller already holds the {@link IncomingCall}
     * value delivered through
     * {@link LinkedWhatsAppClientListener#onCall(LinkedWhatsAppClient, IncomingCall)}, so the store
     * lookup performed by {@link #preacceptCall(String)} is
     * unnecessary.
     *
     * @param call the incoming offer to preaccept; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #preacceptCall(String)
     */
    void preacceptCall(IncomingCall call);

    /**
     * Announces a mic-mute on an in-progress call.
     *
     * @apiNote
     * Drives the mute toggle shown in the in-call control bar: signals
     * the mute state to the peer so the remote UI can render the
     * muted-mic icon next to the local participant.
     *
     * @param callId the identifier of the in-progress call; never {@code null}
     * @throws NullPointerException            if {@code callId} is {@code null}
     * @throws NoSuchElementException          if no call with the given id is cached in the store
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #unmuteCall(String)
     * @see #muteCall(Call)
     */
    void muteCall(String callId);

    /**
     * Announces a mic-unmute on an in-progress call.
     *
     * @apiNote
     * Drives the unmute toggle shown in the in-call control bar: signals
     * the unmuted state to the peer so the remote UI can clear the
     * muted-mic icon next to the local participant.
     *
     * @param callId the identifier of the in-progress call; never {@code null}
     * @throws NullPointerException            if {@code callId} is {@code null}
     * @throws NoSuchElementException          if no call with the given id is cached in the store
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #muteCall(String)
     * @see #unmuteCall(Call)
     */
    void unmuteCall(String callId);

    /**
     * Announces a mic-mute on an in-progress call, taking the call id and peer JID directly from the supplied {@link Call}.
     *
     * @apiNote
     * Use this when the caller already holds the live
     * {@link Call} handle returned by
     * {@link #startCall(JidProvider, AudioOutput, AudioInput)} or
     * {@link #acceptCall(IncomingCall, AudioOutput, AudioInput)}, so the store
     * lookup performed by {@link #muteCall(String)} is
     * unnecessary.
     *
     * @param call the in-progress call; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #unmuteCall(Call)
     * @see #muteCall(String)
     */
    void muteCall(Call call);

    /**
     * Announces a mic-unmute on an in-progress call, taking the call id and peer JID directly from the supplied {@link Call}.
     *
     * @apiNote
     * Use this when the caller already holds the live
     * {@link Call} handle returned by
     * {@link #startCall(JidProvider, AudioOutput, AudioInput)} or
     * {@link #acceptCall(IncomingCall, AudioOutput, AudioInput)}, so the store
     * lookup performed by {@link #unmuteCall(String)} is
     * unnecessary.
     *
     * @param call the in-progress call; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #muteCall(Call)
     * @see #unmuteCall(String)
     */
    void unmuteCall(Call call);

    /**
     * Announces a camera-on transition on an in-progress call.
     *
     * @apiNote
     * Drives the camera toggle shown in the in-call control bar and
     * also drives the mid-call video-upgrade flow: announces video-on
     * for the local participant and surfaces the upgrade-to-video
     * request to the peer (which sees it as
     * {@link LinkedWhatsAppClientListener#onCallVideoStateChanged
     * onCallVideoStateChanged(..., true)}).
     *
     * @param call the in-progress call to update; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #disableCallVideo(Call)
     */
    void enableCallVideo(Call call);

    /**
     * Announces a camera-off transition on an in-progress call.
     *
     * @apiNote
     * Drives the camera toggle shown in the in-call control bar and
     * also drives the mid-call video-downgrade flow: announces video-off
     * for the local participant and surfaces the downgrade request to the
     * peer (which sees it as
     * {@link LinkedWhatsAppClientListener#onCallVideoStateChanged
     * onCallVideoStateChanged(..., false)}).
     *
     * @param call the in-progress call to update; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #enableCallVideo(Call)
     */
    void disableCallVideo(Call call);

    /**
     * Requests a mid-call upgrade of an audio-only call to audio plus video.
     *
     * @apiNote
     * Drives the local "turn on video" affordance during an audio-only
     * call: signals the upgrade request to the peer and starts the
     * local camera track so video frames flow once the peer accepts.
     * The peer observes the request as
     * {@link LinkedWhatsAppClientListener#onCallVideoUpgradeRequest}.
     *
     * @param call the in-progress call to upgrade; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #acceptCallVideoUpgrade(Call)
     * @see #rejectCallVideoUpgrade(Call)
     */
    void requestCallVideoUpgrade(Call call);

    /**
     * Accepts a peer's request to upgrade an audio-only call to audio plus video.
     *
     * @apiNote
     * Drives the "accept video" response to an inbound
     * {@link LinkedWhatsAppClientListener#onCallVideoUpgradeRequest}:
     * announces video-on to the peer and starts the local camera track
     * so the call carries video in both directions.
     *
     * @param call the in-progress call whose upgrade is accepted; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #requestCallVideoUpgrade(Call)
     * @see #rejectCallVideoUpgrade(Call)
     */
    void acceptCallVideoUpgrade(Call call);

    /**
     * Rejects a peer's request to upgrade an audio-only call to audio plus video.
     *
     * @apiNote
     * Drives the "decline video" response to an inbound
     * {@link LinkedWhatsAppClientListener#onCallVideoUpgradeRequest}:
     * signals the rejection to the peer and keeps the call audio-only.
     *
     * @param call the in-progress call whose upgrade is rejected; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #requestCallVideoUpgrade(Call)
     * @see #acceptCallVideoUpgrade(Call)
     */
    void rejectCallVideoUpgrade(Call call);

    /**
     * Starts sharing the local screen on an in-progress call.
     *
     * @apiNote
     * Drives the "share screen" affordance in the in-call control bar:
     * adds a screen-capture video track to the call so the peer renders
     * the shared screen distinctly from a camera feed.
     *
     * @param call the in-progress call to share the screen on; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #stopCallScreenShare(Call)
     */
    void startCallScreenShare(Call call);

    /**
     * Stops sharing the local screen on an in-progress call.
     *
     * @apiNote
     * Drives the "stop sharing" affordance: removes the screen-capture
     * video track previously added by {@link #startCallScreenShare(Call)}.
     *
     * @param call the in-progress call to stop sharing on; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #startCallScreenShare(Call)
     */
    void stopCallScreenShare(Call call);

    /**
     * Broadcasts an emoji reaction to every other participant of an in-progress call.
     *
     * @apiNote
     * Drives the in-call reactions tray: ships the chosen emoji over the
     * call's data channel so every other participant sees a transient
     * reaction. Peers observe it as
     * {@link LinkedWhatsAppClientListener#onCallInteraction} carrying a
     * {@link CallInteraction.Reaction}.
     *
     * @param call  the in-progress call; never {@code null}
     * @param emoji the emoji to broadcast, typically a single grapheme; never {@code null} or empty
     * @throws NullPointerException            if {@code call} or {@code emoji} is {@code null}
     * @throws IllegalArgumentException        if {@code emoji} is empty
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    void sendCallReaction(Call call, String emoji);

    /**
     * Broadcasts a typed emoji reaction to every other participant of an in-progress call.
     *
     * <p>Convenience overload of {@link #sendCallReaction(Call, String)} that broadcasts the
     * {@linkplain WhatsAppEmoji#value() canonical value} of {@code emoji}.
     *
     * @param call  the in-progress call; never {@code null}
     * @param emoji the emoji to broadcast; never {@code null}
     * @throws NullPointerException            if {@code call} or {@code emoji} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #sendCallReaction(Call, String)
     */
    default void sendCallReaction(Call call, WhatsAppEmoji emoji) {
        Objects.requireNonNull(emoji, "emoji cannot be null");
        sendCallReaction(call, emoji.value());
    }

    /**
     * Raises the local participant's hand in an in-progress call.
     *
     * @apiNote
     * Drives the "raise hand" affordance, typically used in group calls
     * to ask to be unmuted: ships the gesture over the call's data
     * channel so peers observe it as
     * {@link LinkedWhatsAppClientListener#onCallInteraction} carrying a
     * {@link CallInteraction.RaiseHand}.
     *
     * @param call the in-progress call; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #lowerCallHand(Call)
     */
    void raiseCallHand(Call call);

    /**
     * Lowers the local participant's previously-raised hand in an in-progress call.
     *
     * @apiNote
     * Drives the "lower hand" affordance that clears a hand raised with
     * {@link #raiseCallHand(Call)}: ships the gesture over the call's
     * data channel so peers observe it as
     * {@link LinkedWhatsAppClientListener#onCallInteraction} carrying a
     * {@link CallInteraction.LowerHand}.
     *
     * @param call the in-progress call; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #raiseCallHand(Call)
     */
    void lowerCallHand(Call call);

    /**
     * Requests that a participant mute themselves in an in-progress call.
     *
     * @apiNote
     * Drives the host-only "mute participant" affordance: ships the
     * request over the call's data channel so the targeted participant
     * observes it as
     * {@link LinkedWhatsAppClientListener#onCallInteraction} carrying a
     * {@link CallInteraction.PeerMuteRequest} and may mute itself.
     *
     * @param call        the in-progress call; never {@code null}
     * @param participant the participant asked to mute; never {@code null}
     * @throws NullPointerException            if {@code call} or {@code participant} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    void requestCallPeerMute(Call call, JidProvider participant);

    /**
     * Requests that the peer emit an immediate video keyframe on an in-progress call.
     *
     * @apiNote
     * Used by the renderer to recover from a lost or corrupt video
     * stream: ships the request over the call's data channel so the peer
     * drives its encoder to emit a keyframe the local decoder can
     * resynchronize on.
     *
     * @param call the in-progress call; never {@code null}
     * @throws NullPointerException            if {@code call} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    void requestCallKeyFrame(Call call);

    /**
     * Invites additional participants to an in-progress group call.
     *
     * @apiNote
     * Drives the "Add participant" affordance on the in-call
     * participant sheet: signals the join request to the group call,
     * listing the JIDs being added. The host JID is taken from the
     * in-flight call store entry keyed by {@code callId}.
     *
     * @param callId       the identifier of the in-progress call; never {@code null}
     * @param participants the participants to invite; must be non-empty
     * @throws NullPointerException            if any argument is {@code null}
     * @throws NoSuchElementException          if no call with the given id is cached in the store
     * @throws IllegalArgumentException        if the call's chat is not a group or community JID, or {@code participants} is empty
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #addCallParticipants(Call, Collection)
     */
    void addCallParticipants(String callId, Collection<? extends JidProvider> participants);

    /**
     * Invites additional participants to an in-progress group call, taking the call id and group JID directly from the supplied {@link Call}.
     *
     * @apiNote
     * Use this when the caller already holds the live
     * {@link Call} handle returned by
     * {@link #startCall(JidProvider, AudioOutput, AudioInput)}, so
     * the store lookup performed by
     * {@link #addCallParticipants(String, Collection)} is
     * unnecessary.
     *
     * @param call         the in-progress group call; never {@code null}
     * @param participants the participants to invite; must be non-empty
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalArgumentException        if the call's chat is not a group or community JID, or {@code participants} is empty
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #addCallParticipants(String, Collection)
     */
    void addCallParticipants(Call call, Collection<? extends JidProvider> participants);

    /**
     * Removes participants from an in-progress group call.
     *
     * @apiNote
     * Drives the "Remove from call" affordance on the in-call
     * participant sheet: signals the removal to the group call,
     * listing the JIDs being removed. The host JID is taken from the
     * in-flight call store entry keyed by {@code callId}.
     *
     * @param callId       the identifier of the in-progress call; never {@code null}
     * @param participants the participants to remove; must be non-empty
     * @throws NullPointerException            if any argument is {@code null}
     * @throws NoSuchElementException          if no call with the given id is cached in the store
     * @throws IllegalArgumentException        if the call's chat is not a group or community JID, or {@code participants} is empty
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #removeCallParticipants(Call, Collection)
     */
    void removeCallParticipants(String callId, Collection<? extends JidProvider> participants);

    /**
     * Removes participants from an in-progress group call, taking the call id and group JID directly from the supplied {@link Call}.
     *
     * @apiNote
     * Use this when the caller already holds the live
     * {@link Call} handle returned by
     * {@link #startCall(JidProvider, AudioOutput, AudioInput)}, so
     * the store lookup performed by
     * {@link #removeCallParticipants(String, Collection)} is
     * unnecessary.
     *
     * @param call         the in-progress group call; never {@code null}
     * @param participants the participants to remove; must be non-empty
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalArgumentException        if the call's chat is not a group or community JID, or {@code participants} is empty
     * @throws IllegalStateException           if this client is not logged in
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #removeCallParticipants(String, Collection)
     */
    void removeCallParticipants(Call call, Collection<? extends JidProvider> participants);

    /**
     * Hands the local user's trusted-contact token to the given peer, fire-and-forget.
     *
     * <p>A trusted-contact token is the privacy-tier handshake behind WhatsApp's "verified contact"
     * trust: each side periodically gives the other a token that survives security-code (identity)
     * changes, so a contact pair stays mutually trusted even after a phone swap or app reinstall and
     * features gated on that trust keep working. This vouches by sending or refreshing the token the
     * peer holds for this account; it does not wait for or return the peer's reciprocal token. To avoid
     * resending too often the token is only handed over when it is actually due, so calling this
     * routinely is cheap.
     *
     * @apiNote
     * Done automatically on every one-to-one message and call to the peer, so applications rarely call
     * it directly; reach for it to refresh the trust the peer holds for this account, for example after
     * the peer's security code changed. To read the peer's token for an outgoing call offer, use
     * {@link #queryTrustedContactToken(JidProvider)} instead.
     *
     * @param peer the JID of the peer to hand the token to; never {@code null}
     * @throws NullPointerException            if {@code peer} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    void issueTrustedContactToken(JidProvider peer);

    /**
     * Returns the peer's trusted-contact token, the value an outgoing call offer carries in its
     * {@code <privacy>} so the peer can validate that the caller is a trusted contact.
     *
     * <p>Returns the peer's cached token when it is present and still within its validity window. When
     * no usable token is cached yet, this vouches for the peer (which prompts the server to deliver the
     * peer's reciprocal token) and waits briefly for that token to arrive before returning it.
     *
     * @apiNote
     * The returned token is the one a call offer must carry; an empty result means the offer is sent
     * without a {@code <privacy>} token, which the peer still accepts.
     *
     * @param peer the JID of the peer whose token to read; never {@code null}
     * @return the peer's trusted-contact token bytes, or empty if none is available in time
     * @throws NullPointerException            if {@code peer} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #queryTrustedContactToken(JidProvider, Duration)
     */
    Optional<byte[]> queryTrustedContactToken(JidProvider peer);

    /**
     * Returns the peer's trusted-contact token, waiting up to {@code timeout} for it when none is
     * cached.
     *
     * <p>Behaves as {@link #queryTrustedContactToken(JidProvider)} but bounds the wait for the
     * server-pushed reciprocal token by the supplied duration instead of the client default.
     *
     * @param peer    the JID of the peer whose token to read; never {@code null}
     * @param timeout the maximum time to wait for the reciprocal token; never {@code null}
     * @return the peer's trusted-contact token bytes, or empty if none is available in time
     * @throws NullPointerException            if {@code peer} or {@code timeout} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Optional<byte[]> queryTrustedContactToken(JidProvider peer, Duration timeout);

    /**
     * Posts a {@link ScheduledCallCreationMessage} announcing a scheduled voice or video call inside the given chat.
     *
     * @apiNote
     * Drives the "Schedule call" affordance inside a chat: posts a
     * chat message carrying the chosen title, future timestamp, and
     * audio-vs-video flag that other participants can opt into. The
     * actual call is placed later via
     * {@link #startCall(JidProvider, AudioOutput, AudioInput)}; the
     * announcement is purely an in-chat coordination message.
     *
     * @param chat        the chat to post the announcement in; never {@code null}
     * @param title       the human-readable title of the scheduled call; never {@code null}
     * @param scheduledAt the future {@link Instant} the call is scheduled for; never {@code null}
     * @param video       {@code true} for a video call, {@code false} for voice-only
     * @throws NullPointerException                           if any argument is {@code null}
     * @throws WhatsAppMessageException.Send.InvalidRecipient if the JID does not match a supported chat type
     */
    void sendScheduledCall(JidProvider chat, String title, Instant scheduledAt, boolean video);

    /**
     * Cancels a previously announced scheduled call.
     *
     * @apiNote
     * Drives the "Cancel scheduled call" affordance on a posted
     * scheduled-call announcement: posts a
     * {@link ScheduledCallEditMessage} with edit type
     * {@link ScheduledCallEditMessage.EditType#CANCEL} keyed by the
     * original creation message so participants see the announcement
     * marked as cancelled. The destination chat is taken from
     * {@link MessageKey#parentJid() creationKey.parentJid()}.
     *
     * @param creationKey the {@link MessageKey} of the original {@link ScheduledCallCreationMessage}; never {@code null}
     * @throws NullPointerException                           if {@code creationKey} is {@code null}
     * @throws NoSuchElementException                         if {@code creationKey} has no parent JID
     * @throws WhatsAppMessageException.Send.InvalidRecipient if the JID does not match a supported chat type
     */
    void cancelScheduledCall(MessageKey creationKey);

    /**
     * Joins the shared call link {@code link} carrying the supplied media streams and returns a live
     * session.
     *
     * <p>The {@code link} is a {@code https://call.whatsapp.com/voice/<token>} or
     * {@code .../video/<token>} URL; its path supplies both the opaque link token and the link's
     * configured media kind ({@code voice/} maps to {@link CallLinkMedia#AUDIO}, {@code video/} maps to
     * {@link CallLinkMedia#VIDEO}), which is carried on the link query and join so the relay can confirm
     * it against the link's configuration.
     *
     * <p>The local user joins with video when {@code videoOut} is non-{@code null} and audio-only
     * otherwise, independently of the link's media kind. The two outbound sources supply the local audio
     * and video the call transmits, and the two inbound sinks receive the remote audio and video; the
     * streams are owned by the call engine and ended automatically when the call ends, so the application
     * never closes them.
     *
     * @apiNote
     * Drives the "Join via link" flow exposed by following a {@code call.whatsapp.com} link: resolves the
     * link through a preview query and a join request, registers the resulting {@link Call} in the
     * in-flight call store, and returns it with the session connecting rather than connected. Supply the
     * streams with the stream factories such as {@link AudioOutput#fromMicrophone()} and
     * {@link AudioInput#toSpeaker()}, or pump frames yourself for a call-to-call bridge or a bot. For an
     * audio-only join prefer the {@link #joinCallLink(URI, AudioOutput, AudioInput)} overload.
     *
     * @param link     the {@code https://call.whatsapp.com/{voice|video}/<token>} call-link URL; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} to
     *                 join audio-only
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} to join
     *                 audio-only
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported
     *                                         on the WhatsApp Web flavour
     * @throws NullPointerException            if {@code link}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code link} is not a well-formed
     *                                         {@code call.whatsapp.com/{voice|video}/<token>} call-link URL
     * @throws IllegalStateException           if this client is not logged in, or call links are disabled
     *                                         for this account by the server feature gate
     * @throws WhatsAppServerRuntimeException  if the relay rejects the link query
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #joinCallLink(URI, AudioOutput, AudioInput)
     * @see #startCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)
     */
    Call joinCallLink(URI link, AudioOutput audioOut, AudioInput audioIn,
                      VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the shared call link {@code link} as audio-only carrying the supplied audio streams and
     * returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinCallLink(URI, AudioOutput, AudioInput, VideoOutput, VideoInput)} with no
     * video; the joined leg can still be upgraded to video later.
     *
     * @param link     the {@code https://call.whatsapp.com/{voice|video}/<token>} call-link URL; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported
     *                                         on the WhatsApp Web flavour
     * @throws NullPointerException            if {@code link}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code link} is not a well-formed
     *                                         {@code call.whatsapp.com/{voice|video}/<token>} call-link URL
     * @throws IllegalStateException           if this client is not logged in, or call links are disabled
     *                                         for this account by the server feature gate
     * @throws WhatsAppServerRuntimeException  if the relay rejects the link query
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinCallLink(URI link, AudioOutput audioOut, AudioInput audioIn);

    /**
     * Joins the call behind {@code link} drawing both the audio and the video from a single video source
     * and a single video sink, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinCallLink(URI, AudioOutput, AudioInput, VideoOutput, VideoInput)} that
     * passes {@code videoOut} as both the audio and video source and {@code videoIn} as both the audio and
     * video sink, for a caller whose video device already carries the audio.
     *
     * @param link     the {@code https://call.whatsapp.com/{voice|video}/<token>} call-link URL; never {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported
     *                                         on the WhatsApp Web flavour
     * @throws NullPointerException            if {@code link}, {@code videoOut}, or {@code videoIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code link} is not a well-formed
     *                                         {@code call.whatsapp.com/{voice|video}/<token>} call-link URL
     * @throws IllegalStateException           if this client is not logged in, or call links are disabled
     *                                         for this account by the server feature gate
     * @throws WhatsAppServerRuntimeException  if the relay rejects the link query
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinCallLink(URI link, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the call behind {@code link} drawing both the audio and the video input from a single video
     * sink while transmitting from separate audio and video sources, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinCallLink(URI, AudioOutput, AudioInput, VideoOutput, VideoInput)} that
     * passes {@code videoIn} as both the audio and video sink, for a caller whose video device also renders
     * the remote audio.
     *
     * @param link     the {@code https://call.whatsapp.com/{voice|video}/<token>} call-link URL; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported
     *                                         on the WhatsApp Web flavour
     * @throws NullPointerException            if {@code link}, {@code audioOut}, {@code videoOut}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code link} is not a well-formed
     *                                         {@code call.whatsapp.com/{voice|video}/<token>} call-link URL
     * @throws IllegalStateException           if this client is not logged in, or call links are disabled
     *                                         for this account by the server feature gate
     * @throws WhatsAppServerRuntimeException  if the relay rejects the link query
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinCallLink(URI link, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the call behind {@code link} drawing both the audio and the video output from a single video
     * source while receiving into separate audio and video sinks, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinCallLink(URI, AudioOutput, AudioInput, VideoOutput, VideoInput)} that
     * passes {@code videoOut} as both the audio and video source, for a caller whose video device also
     * captures the local audio.
     *
     * @param link     the {@code https://call.whatsapp.com/{voice|video}/<token>} call-link URL; never {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoIn  the sink the engine fills with received remote video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported
     *                                         on the WhatsApp Web flavour
     * @throws NullPointerException            if {@code link}, {@code videoOut}, {@code audioIn}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code link} is not a well-formed
     *                                         {@code call.whatsapp.com/{voice|video}/<token>} call-link URL
     * @throws IllegalStateException           if this client is not logged in, or call links are disabled
     *                                         for this account by the server feature gate
     * @throws WhatsAppServerRuntimeException  if the relay rejects the link query
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinCallLink(URI link, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn);

    /**
     * Joins the in-progress group call described by {@code call} carrying the supplied media streams and
     * returns a live session.
     *
     * <p>Late-joining an ongoing group call is a group-only operation, distinct from
     * {@link #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)} only in that the join
     * enters the call's lobby before answering: accepting answers a ringing offer directly, while joining
     * answers the same offer from the ongoing-call banner after the ring was dismissed. It applies only to a
     * group {@link IncomingCall} the engine still tracks from its inbound offer (one surfaced through
     * {@link LinkedWhatsAppClientListener#onCall}); a one-to-one {@code call} has no join and is rejected, and an
     * offer whose ring has already ended is no longer joinable.
     *
     * <p>The local user joins with video when {@code videoOut} is non-{@code null} and audio-only otherwise,
     * independently of whether video was offered. The two outbound sources supply the local audio and video the
     * call transmits, and the two inbound sinks receive the remote audio and video; the streams are owned by
     * the call engine and ended automatically when the call ends, so the application never closes them.
     *
     * @apiNote
     * Drives the "Join" affordance on an ongoing group-call banner. Prefer
     * {@link #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)} to answer a call that
     * is actively ringing this device; use this to join a group call that is already in progress. For an
     * audio-only join prefer the {@link #joinGroupCall(IncomingCall, AudioOutput, AudioInput)} overload.
     *
     * @param call     the ongoing group call to join; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} to join
     *                 audio-only
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} to join audio-only
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code call}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code call} is not a group call, or the engine no longer
     *                                         tracks an offer for it
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #joinGroupCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)
     * @see #acceptCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)
     */
    Call joinGroupCall(IncomingCall call, AudioOutput audioOut, AudioInput audioIn,
                       VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the in-progress group call described by {@code call} as audio-only carrying the supplied audio
     * streams and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * with no video; the joined leg can still be upgraded to video later.
     *
     * @param call     the ongoing group call to join; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code call}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code call} is not a group call, or the engine no longer
     *                                         tracks an offer for it
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(IncomingCall call, AudioOutput audioOut, AudioInput audioIn);

    /**
     * Joins the in-progress group call described by {@code call} drawing both the audio and the video from
     * a single video source and a single video sink, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoOut} as both the audio and video source and {@code videoIn} as both the audio
     * and video sink, for a caller whose video device already carries the audio.
     *
     * @param call     the ongoing group call to join; never {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code call}, {@code videoOut}, or {@code videoIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code call} is not a group call, or the engine no longer
     *                                         tracks an offer for it
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(IncomingCall call, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the in-progress group call described by {@code call} drawing both the audio and the video input
     * from a single video sink while transmitting from separate audio and video sources, and returns a live
     * session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoIn} as both the audio and video sink, for a caller whose video device also
     * renders the remote audio.
     *
     * @param call     the ongoing group call to join; never {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code call}, {@code audioOut}, {@code videoOut}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code call} is not a group call, or the engine no longer
     *                                         tracks an offer for it
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(IncomingCall call, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the in-progress group call described by {@code call} drawing both the audio and the video
     * output from a single video source while receiving into separate audio and video sinks, and returns a
     * live session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoOut} as both the audio and video source, for a caller whose video device also
     * captures the local audio.
     *
     * @param call     the ongoing group call to join; never {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoIn  the sink the engine fills with received remote video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code call}, {@code videoOut}, {@code audioIn}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code call} is not a group call, or the engine no longer
     *                                         tracks an offer for it
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(IncomingCall call, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn);

    /**
     * Joins the single in-progress group call in {@code group} carrying the supplied media streams and returns
     * a live session.
     *
     * <p>A group hosts at most one call at a time, so naming the group is enough to join it: the single received
     * offer this client is tracking for {@code group} is resolved and joined. This is the offer-less counterpart
     * of {@link #joinGroupCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}, convenient when
     * the caller holds the group but not the specific {@link IncomingCall}; the group must have a call this
     * device was offered and is still tracking.
     *
     * <p>The local user joins with video when {@code videoOut} is non-{@code null} and audio-only otherwise.
     * The two outbound sources supply the local audio and video the call transmits, and the two inbound sinks
     * receive the remote audio and video; the streams are owned by the call engine and ended automatically when
     * the call ends, so the application never closes them.
     *
     * @apiNote
     * Drives the "Join" affordance in a group header when a call is in progress. For an audio-only join prefer
     * the {@link #joinGroupCall(JidProvider, AudioOutput, AudioInput)} overload.
     *
     * @param group    the group whose ongoing call to join; must be a group or community JID; never
     *                 {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} to join
     *                 audio-only
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} to join audio-only
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code group}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code group} is not a group or community JID
     * @throws NoSuchElementException          if {@code group} has no ongoing group call this device is tracking
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     * @see #joinGroupCall(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)
     * @see #startCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)
     */
    Call joinGroupCall(JidProvider group, AudioOutput audioOut, AudioInput audioIn,
                       VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the single in-progress group call in {@code group} as audio-only carrying the supplied audio
     * streams and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * with no video; the joined leg can still be upgraded to video later.
     *
     * @param group    the group whose ongoing call to join; must be a group or community JID; never
     *                 {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code group}, {@code audioOut}, or {@code audioIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code group} is not a group or community JID
     * @throws NoSuchElementException          if {@code group} has no ongoing group call this device is tracking
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(JidProvider group, AudioOutput audioOut, AudioInput audioIn);

    /**
     * Joins the single in-progress group call in {@code group} drawing both the audio and the video from a
     * single video source and a single video sink, and returns a live session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoOut} as both the audio and video source and {@code videoIn} as both the audio
     * and video sink, for a caller whose video device already carries the audio.
     *
     * @param group    the group whose ongoing call to join; must be a group or community JID; never
     *                 {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code group}, {@code videoOut}, or {@code videoIn} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code group} is not a group or community JID
     * @throws NoSuchElementException          if {@code group} has no ongoing group call this device is tracking
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(JidProvider group, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the single in-progress group call in {@code group} drawing both the audio and the video input
     * from a single video sink while transmitting from separate audio and video sources, and returns a live
     * session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoIn} as both the audio and video sink, for a caller whose video device also
     * renders the remote audio.
     *
     * @param group    the group whose ongoing call to join; must be a group or community JID; never
     *                 {@code null}
     * @param audioOut the source the engine drains local audio from for transmission; never {@code null}
     * @param videoOut the source the engine drains local video from for transmission; never {@code null}
     * @param videoIn  the sink the engine fills with received remote audio and video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code group}, {@code audioOut}, {@code videoOut}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code group} is not a group or community JID
     * @throws NoSuchElementException          if {@code group} has no ongoing group call this device is tracking
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(JidProvider group, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn);

    /**
     * Joins the single in-progress group call in {@code group} drawing both the audio and the video output
     * from a single video source while receiving into separate audio and video sinks, and returns a live
     * session.
     *
     * @apiNote
     * Convenience for {@link #joinGroupCall(JidProvider, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * that passes {@code videoOut} as both the audio and video source, for a caller whose video device also
     * captures the local audio.
     *
     * @param group    the group whose ongoing call to join; must be a group or community JID; never
     *                 {@code null}
     * @param videoOut the source the engine drains local audio and video from; never {@code null}
     * @param audioIn  the sink the engine fills with received remote audio; never {@code null}
     * @param videoIn  the sink the engine fills with received remote video; never {@code null}
     * @return the live {@link Call} session bound to the joined call
     * @throws UnsupportedOperationException   if this client is not a web client; calls are only supported on
     *                                         the WhatsApp Web flavour
     * @throws NullPointerException            if {@code group}, {@code videoOut}, {@code audioIn}, or
     *                                         {@code videoIn} is {@code null}
     * @throws IllegalArgumentException        if {@code group} is not a group or community JID
     * @throws NoSuchElementException          if {@code group} has no ongoing group call this device is tracking
     * @throws IllegalStateException           if the call is not in an answerable state
     * @throws WhatsAppSessionException.Closed if the socket has been closed
     */
    Call joinGroupCall(JidProvider group, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn);

    /**
     * Reconciles the local view of the Channels tab with the server.
     *
     * @apiNote
     * Use to redraw the Channels surface against an authoritative copy
     * of the newsletters this account follows. Every followed
     * newsletter is merged into {@link LinkedWhatsAppStore} keyed by its
     * JID, and {@link LinkedWhatsAppClientListener#onNewsletters} fires once
     * with the new authoritative set the first time the refresh
     * succeeds for the session.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void refreshNewsletters();

    /**
     * Reconciles the local view of the Groups section of the chat list
     * with the server.
     *
     * @apiNote
     * Use to redraw the groups surface against an authoritative copy
     * of the groups this account participates in. Every group is
     * merged into {@link LinkedWhatsAppStore} (chat record plus parsed
     * {@link GroupMetadata}), and
     * {@link LinkedWhatsAppClientListener#onGroups} fires once with the new
     * authoritative set.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void refreshGroups();

    /**
     * Reads the current invite code attached to the given group without rotating it.
     *
     * @apiNote
     * The invite code is the opaque scalar backing the shareable
     * {@code chat.whatsapp.com/<code>} link. Use
     * {@link #createGroupInviteCode(JidProvider, String)} to rotate it
     * and invalidate any previously distributed link.
     *
     * @param group the target group JID; never {@code null}
     * @return the current invite-code scalar, or {@link Optional#empty()} when the relay returned no payload
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws IllegalArgumentException        if the JID is not a group or community JID
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<String> queryGroupInviteCode(JidProvider group);

    /**
     * Revokes the current invite code for the given group and returns the freshly minted replacement.
     *
     * @apiNote
     * Drives the "Reset link" affordance on the group invite-link
     * sheet: the server rotates the invite code, and the new value is
     * returned so the caller can display or re-share it.
     *
     * @param group the target group JID; never {@code null}
     * @return the freshly minted invite code
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws IllegalArgumentException        if the JID is not a group or community JID
     * @throws NoSuchElementException          if the response carries no invite code
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    String revokeGroupInviteCode(JidProvider group);

    /**
     * Joins a group using a public invite code parsed from a {@code chat.whatsapp.com/XYZ} link.
     *
     * @apiNote
     * Drives the "Join group" affordance on the invite-link landing
     * sheet. The server returns the joined group JID immediately
     * for open groups; for approval-gated groups it returns the
     * gated group's JID and creates a pending membership request.
     * The returned JID identifies the group in both cases.
     *
     * @param inviteCode the invite code (the path segment after {@code chat.whatsapp.com/}); never {@code null}
     * @return the JID of the joined group (or the gated group for which a request was created)
     * @throws NullPointerException            if {@code inviteCode} is {@code null}
     * @throws NoSuchElementException          if the server response is malformed
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Jid joinGroupViaInvite(String inviteCode);

    /**
     * Fetches the full-resolution profile picture of a group through a public invite link, without joining the group first.
     *
     * @apiNote
     * Backs the icon shown on the invite-link landing sheet alongside
     * the group subject and member count: returns the picture
     * identity, MIME type, download URL, and direct-path tuple. The
     * call works even when the caller is not a member of the group
     * and does not modify local group membership.
     *
     * @param group      the JID of the group the invite refers to; must be a group JID
     * @param inviteCode the public invite code parsed from the {@code chat.whatsapp.com} URL; never {@code null}
     * @return the picture identity, type, download URL and direct-path tuple
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws NoSuchElementException          if the server reply carries no picture entry
     * @see #queryGroupInvitePicturePreview(JidProvider, String)
     */
    GroupInvitePicture queryGroupInvitePicture(JidProvider group, String inviteCode);

    /**
     * Fetches the low-resolution thumbnail of a group icon through a public invite link.
     *
     * @apiNote
     * Variant of {@link #queryGroupInvitePicture(JidProvider, String)}
     * that requests the preview thumbnail used in the invite-link
     * preview card rather than the full-resolution picture used on
     * the invite-link landing sheet.
     *
     * @param group      the JID of the group the invite refers to; must be a group JID
     * @param inviteCode the public invite code; never {@code null}
     * @return the picture identity, type, download URL and direct-path tuple
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws NoSuchElementException          if no picture entry is returned
     * @see #queryGroupInvitePicture(JidProvider, String)
     */
    GroupInvitePicture queryGroupInvitePicturePreview(JidProvider group, String inviteCode);

    /**
     * Queries group metadata using a v4 invite received in-band via a {@link GroupInviteMessage}.
     *
     * @apiNote
     * Backs the preview rendered when a user receives an in-chat
     * group invite (the v4 flow, distinct from public invite-link
     * previews). The invite code, expiration, and inviting
     * administrator are forwarded to the server, which returns the
     * group's metadata.
     *
     * @param invite the in-band group invite, carrying the invitee JID, sender JID, expiration and invite code; never {@code null}
     * @return the parsed group metadata
     * @throws NullPointerException            if {@code invite} is {@code null}
     * @throws NoSuchElementException          if the server response is not a group subtree
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    GroupMetadata queryGroupInfoByInvite(GroupInvite invite);

    /**
     * Accepts an in-band group invite (v4) sent by an administrator and joins the referenced group.
     *
     * @apiNote
     * Drives the "Accept" button on the in-chat group-invite preview.
     * The expiration timestamp and inviting administrator are
     * forwarded to the server. Both the immediate-join and
     * approval-pending paths resolve to the same group JID, so the
     * method returns nothing.
     *
     * @param group           the JID of the group being joined; must be a group or community JID
     * @param target          the inviting administrator JID; never {@code null}
     * @param inviteTimestamp the invite expiration time; never {@code null}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalArgumentException        if {@code group} is not a group or community JID
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void sendGroupInvite(JidProvider group, JidProvider target, Instant inviteTimestamp);

    /**
     * Queries the list of pending join-request applicants for an approval-gated group.
     *
     * @apiNote
     * Backs the "Pending requests" admin sheet shown for
     * approval-gated groups: returns the applicant JIDs in server
     * order and emits the corresponding view-pending-participants
     * telemetry event.
     *
     * @param group the target group JID; never {@code null}
     * @return the pending applicants in server order
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws IllegalArgumentException        if the JID is not a group or community JID
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    List<Jid> queryGroupJoinRequests(JidProvider group);

    /**
     * Approves a pending join request and admits the applicant into the group.
     *
     * @apiNote
     * Drives the "Approve" affordance on a pending-request entry in
     * the group admin sheet: admits the applicant into the group and
     * emits the corresponding membership-request-approve telemetry
     * event carrying the success flag and server response time.
     *
     * @param group     the target group JID; never {@code null}
     * @param applicant the applicant JID to admit; never {@code null}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalArgumentException        if the JID is not a group or community JID
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #rejectGroupJoinRequest(JidProvider, JidProvider)
     */
    void acceptGroupJoinRequest(JidProvider group, JidProvider applicant);

    /**
     * Rejects a pending join request and keeps the applicant out of the group.
     *
     * @apiNote
     * Drives the "Reject" affordance on a pending-request entry in
     * the group admin sheet: keeps the applicant out of the group and
     * emits the corresponding membership-request-reject telemetry
     * event carrying the success flag and server response time.
     *
     * @param group     the target group JID; never {@code null}
     * @param applicant the applicant JID to reject; never {@code null}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalArgumentException        if the JID is not a group or community JID
     * @throws WhatsAppServerRuntimeException  if the server rejects the IQ
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #acceptGroupJoinRequest(JidProvider, JidProvider)
     */
    void rejectGroupJoinRequest(JidProvider group, JidProvider applicant);

    /**
     * Encrypts and dispatches a peer protocol message to one of the current account's own devices.
     *
     * @apiNote
     * Used for the device-to-device traffic that never reaches another
     * user: app-state-sync key shares, sender-key resets,
     * fatal-exception notifications, and other peer protocol payloads
     * exchanged between a user's linked devices. The destination
     * {@code chatJid} is typically the primary device JID owned by
     * this session.
     *
     * @param chatJid  the destination device JID; never {@code null}
     * @param response the fully-populated peer message to encrypt and send; never {@code null}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void sendPeerMessage(JidProvider chatJid, ChatMessageInfo response);

    /**
     * Records one thread-interaction activity against the ctlv2 thread-logging aggregator for the
     * given thread.
     *
     * @apiNote
     * This is the internal entry point that receive-path and call-end producer hooks outside the
     * client package (the inbound message stream handler and the calls service) use to report
     * activity the client cannot observe directly. The aggregator buckets it per thread per day and
     * uploads it once daily as the {@code ThreadInteractionData} WAM events; application code never
     * calls it directly.
     *
     * @param chat     the thread the activity occurred in; never {@code null}
     * @param activity the activity to record; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    void recordThreadActivity(JidProvider chat, ThreadLoggingActivity activity);

    /**
     * Asks the primary device to resend a specific message that this companion holds only as an
     * undecryptable placeholder.
     *
     * <p>Issues a placeholder-resend peer data operation request for the message identified by
     * {@code key}. The primary looks up the plaintext and delivers it back through the on-demand
     * history pipeline, where it surfaces to the registered history-sync listeners. This is the only
     * way to recover an individual message by key in a regular chat; whole messages are not otherwise
     * addressable on the server.
     *
     * @apiNote
     * Use this for messages received as placeholder stubs, for example after a missed retry.
     * Newsletter messages are not delivered through this mechanism; query them with
     * {@link #queryNewsletterMessages(JidProvider, int)} instead.
     *
     * @param key the key of the message to resend; never {@code null}
     * @throws NullPointerException            if {@code key} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #queryChatMessages(JidProvider, int)
     */
    void queryChatMessage(MessageKey key);

    /**
     * Asks the primary device to deliver a slice of older messages for a regular chat, ending at the
     * oldest message this companion currently holds.
     *
     * <p>Issues an on-demand history sync request for up to {@code count} messages older than the
     * chat's oldest locally stored message. The delivered messages arrive asynchronously through the
     * registered history-sync listeners. Newsletters use a different mechanism and are rejected.
     *
     * @apiNote
     * This is the scroll-back primitive: call it repeatedly to page further into a chat's history.
     * The number actually delivered may be smaller than {@code count} and is bounded by the user's
     * per-device history setting on their phone. For a newsletter use
     * {@link #queryNewsletterMessages(JidProvider, int)} instead.
     *
     * @param chat  the regular chat to page; never {@code null}
     * @param count the maximum number of older messages to request; must be positive
     * @throws NullPointerException            if {@code chat} is {@code null}
     * @throws IllegalArgumentException        if {@code count} is not positive, or {@code chat} is a newsletter
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #queryChatMessages(JidProvider, MessageKey, int)
     */
    void queryChatMessages(JidProvider chat, int count);

    /**
     * Asks the primary device to deliver a slice of older messages for a regular chat, ending at a
     * specific message.
     *
     * <p>Behaves like {@link #queryChatMessages(JidProvider, int)} but uses {@code before} as the
     * boundary instead of the oldest locally stored message, requesting up to {@code count} messages
     * older than it. When this companion holds {@code before}, its timestamp is included so the
     * primary can resolve the boundary precisely.
     *
     * @apiNote
     * Use this to page from an arbitrary anchor rather than from the oldest message held.
     *
     * @param chat   the regular chat to page; never {@code null}
     * @param before the message to page back from; never {@code null}
     * @param count  the maximum number of older messages to request; must be positive
     * @throws NullPointerException            if {@code chat} or {@code before} is {@code null}
     * @throws IllegalArgumentException        if {@code count} is not positive, or {@code chat} is a newsletter
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #queryChatMessages(JidProvider, int)
     */
    void queryChatMessages(JidProvider chat, MessageKey before, int count);

    /**
     * Asks the primary device to deliver the account's full message history over the given window.
     *
     * <p>Issues a full on-demand history sync request covering the last {@code days} days across all
     * chats. The history is replayed asynchronously through the registered history-sync listeners. The
     * request is only issued when the primary has granted this device complete history access; that
     * grant is delivered to desktop companions when the user selects "All chat history" for the device
     * on their phone.
     *
     * @apiNote
     * This is a heavyweight, account-wide pull. It consults the complete-access grant and fails fast
     * when the grant is absent rather than issuing a request the primary would refuse. WhatsApp Web
     * itself only requests up to one year; larger windows are honoured at the primary's discretion.
     *
     * @param days the length of the requested window in days; must be positive
     * @throws IllegalArgumentException        if {@code days} is not positive
     * @throws IllegalStateException           if the primary has not granted complete history access
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #queryChatMessages(JidProvider, int)
     */
    void queryFullChatsHistory(int days);

    /**
     * Resolves a batch of phone-number JIDs to whether each one corresponds to a registered WhatsApp account.
     *
     * @apiNote
     * Backs the in-product contact picker's "is on WhatsApp" probe:
     * batches every supplied JID into one server lookup and reports,
     * per input, whether the corresponding phone number is registered
     * to a WhatsApp account.
     *
     * @param phoneNumbers the user JIDs to look up; never {@code null}
     * @return an unmodifiable map from the server-echoed phone JID to whether that user is registered
     * @throws NullPointerException           if {@code phoneNumbers} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay rejects the USync query
     */
    Map<Jid, Boolean> hasWhatsapp(Collection<? extends JidProvider> phoneNumbers);

    /**
     * Resolves a single phone-number JID to whether it corresponds to a registered WhatsApp account.
     *
     * @apiNote
     * Single-input convenience over {@link #hasWhatsapp(Collection)}
     * that returns {@code true} when any returned entry is positive
     * and {@code false} otherwise, including the case where the JID
     * does not map to a phone number.
     *
     * @param phone the user JID to look up; never {@code null}
     * @return {@code true} when the server reports the phone as registered, {@code false} otherwise
     * @throws NullPointerException           if {@code phone} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay rejects the USync query
     */
    boolean hasWhatsapp(JidProvider phone);

    /**
     * Queries the push name associated with the given user JID.
     *
     * @apiNote
     * Backs every UI surface that needs to render a contact's display
     * name when the local store has nothing on file: consults the
     * cached {@link Contact#chosenName() chosen name} first and only
     * falls back to a remote lookup when no usable cached value
     * exists. The remote lookup returns the push name the contact has
     * chosen to publish.
     *
     * @param jid the user JID to resolve; never {@code null}
     * @return the resolved push name, or {@link Optional#empty()} when neither the store nor the server knows one
     * @throws NullPointerException           if {@code jid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay rejects the USync query
     */
    Optional<String> queryName(JidProvider jid);

    /**
     * Uploads a batch of phone-number contacts and returns the phone-to-JID mapping the server resolves for them.
     *
     * @apiNote
     * Backs the "Add to WhatsApp from system contacts" flow: extracts
     * the default cell numbers from every supplied contact card,
     * batches them into a background contact-sync lookup, and returns
     * only the entries the server acknowledges as registered. The
     * returned map is keyed by the phone-number JID sent and valued by
     * the server-normalised JID (the two differ only when the server
     * rewrites the identifier, for example during LID migration).
     *
     * @param contacts the contact cards to synchronise; never {@code null}
     * @return an unmodifiable map from phone-number JID to the server-returned JID for every successfully resolved contact
     * @throws NullPointerException           if {@code contacts} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay rejects the USync query
     */
    Map<Jid, Jid> syncContacts(Collection<ContactCard> contacts);

    /**
     * Queries the full metadata envelope for a newsletter with the supplied viewer role.
     *
     * @apiNote
     * Two-argument convenience for
     * {@link #queryNewsletter(JidProvider, NewsletterViewerRole, boolean)}
     * that passes {@code false} for {@code dehydrated}, returning
     * full thread metadata plus viewer-scoped settings (the default
     * path used when opening a newsletter from the chat list).
     *
     * @param newsletterJid the newsletter JID to query; never {@code null}
     * @param role          the viewer role to assert during the query, or {@code null} to omit the {@code view_role} field
     * @return the parsed {@link Newsletter}, or {@link Optional#empty()} when the response carries no usable payload
     * @throws NullPointerException            if {@code newsletterJid} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #queryNewsletter(JidProvider, NewsletterViewerRole, boolean)
     */
    Optional<Newsletter> queryNewsletter(JidProvider newsletterJid, NewsletterViewerRole role);

    /**
     * Queries metadata for a single newsletter, choosing between the full and the lightweight projection.
     *
     * @apiNote
     * Backs both the channel header opened on a newsletter and the
     * lightweight background refresh that keeps subscriber counts
     * current. When {@code dehydrated} is {@code false}, the server
     * returns full thread metadata plus the viewer-scoped settings;
     * when {@code true}, it returns only the subscriber count,
     * verification flag, reaction-codes setting, and any associated
     * WAMO subscription plan id. The result is folded into the
     * store-resident newsletter so subsequent reads observe it.
     *
     * @param newsletterJid the newsletter JID to query; never {@code null}
     * @param role          the viewer role to assert during the query, or {@code null} to omit the {@code view_role} field
     * @param dehydrated    {@code true} for the lightweight projection, {@code false} for the full one
     * @return the parsed {@link Newsletter}, or {@link Optional#empty()} when the response carries no usable payload
     * @throws NullPointerException            if {@code newsletterJid} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<Newsletter> queryNewsletter(JidProvider newsletterJid, NewsletterViewerRole role, boolean dehydrated);

    /**
     * Creates a brand-new newsletter owned by this account.
     *
     * @apiNote
     * Drives the "Create channel" flow in the channels surface:
     * publishes a new newsletter with the supplied name, optional
     * description, and optional JPEG picture bytes. On success the
     * server returns the freshly allocated newsletter id, which is
     * resolved to a local {@link Newsletter} and inserted into the
     * store.
     *
     * @param create the new newsletter content (name, optional description, optional picture bytes); never {@code null}
     * @return the newly created {@link Newsletter}
     * @throws NullPointerException            if {@code create} is {@code null}
     * @throws NoSuchElementException          if the server response does not contain a newsletter id
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Newsletter createNewsletter(NewsletterCreate create);

    /**
     * Edits the mutable metadata (name, description, picture, reaction policy) of a newsletter owned by this account.
     *
     * @apiNote
     * Drives the "Edit channel info" affordance on the newsletter
     * admin sheet. Each of name, description, picture, and reaction
     * policy is sent only when the corresponding field on
     * {@link NewsletterMetadataEdit} is present; absent fields are
     * omitted from the request so the server leaves them untouched.
     * Setting {@link NewsletterMetadataEdit#reactionSetting()} drives
     * the "Reactions" picker (allowed reaction code set, all-emoji vs
     * none, blocklist) alongside any other pending metadata change.
     *
     * @param edit the newsletter JID together with the fields to edit; never {@code null}
     * @throws NullPointerException            if {@code edit} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editNewsletterMetadata(NewsletterMetadataEdit edit);

    /**
     * Permanently deletes a newsletter owned by this account.
     *
     * @apiNote
     * Drives the "Delete channel" affordance on the newsletter admin
     * sheet: removes the newsletter both server-side and from the
     * local store. Returns the pre-deletion snapshot, or
     * {@link Optional#empty()} when the newsletter is unknown
     * locally.
     *
     * @param newsletter the newsletter JID to delete; never {@code null}
     * @return the removed {@link Newsletter} as it existed before deletion, or {@link Optional#empty()} when the newsletter was unknown locally
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<Newsletter> deleteNewsletter(JidProvider newsletter);

    /**
     * Subscribes this account to the given newsletter so future updates are delivered.
     *
     * @apiNote
     * Drives the "Follow channel" button on a newsletter header:
     * subscribes this account to the newsletter and registers it in
     * the local store so subsequent lookups succeed.
     *
     * @param newsletter the newsletter JID to follow; never {@code null}
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void joinNewsletter(JidProvider newsletter);

    /**
     * Unsubscribes this account from the given newsletter so updates stop being delivered.
     *
     * @apiNote
     * Drives the "Unfollow channel" affordance on a newsletter
     * header: unsubscribes this account and removes the newsletter
     * from the local store on success.
     *
     * @param newsletter the newsletter JID to unfollow; never {@code null}
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void leaveNewsletter(JidProvider newsletter);

    /**
     * Mutes admin-activity notifications for the given newsletter.
     *
     * @apiNote
     * Drives the "Mute admin activity" toggle on the newsletter
     * settings sheet: silences notifications about admin-only
     * activity on this newsletter for the authenticated user.
     *
     * @param newsletter the newsletter JID; never {@code null}
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #unmuteNewsletter(JidProvider)
     */
    void muteNewsletter(JidProvider newsletter);

    /**
     * Unmutes admin-activity notifications for the given newsletter.
     *
     * @apiNote
     * Drives the "Mute admin activity" toggle on the newsletter
     * settings sheet: restores notifications about admin-only
     * activity on this newsletter for the authenticated user.
     *
     * @param newsletter the newsletter JID; never {@code null}
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #muteNewsletter(JidProvider)
     */
    void unmuteNewsletter(JidProvider newsletter);

    /**
     * Admin-revokes a message previously published on a newsletter owned by this account.
     *
     * @apiNote
     * Drives the "Delete for everyone" affordance on a newsletter
     * message from the channel admin context: revokes the published
     * message identified by {@code serverMessageId} so followers no
     * longer see it.
     *
     * @param newsletter      the newsletter JID hosting the message; never {@code null}
     * @param serverMessageId the target server message id; never {@code null}
     * @throws NullPointerException            if {@code newsletter} or {@code serverMessageId} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void revokeNewsletterMessage(JidProvider newsletter, String serverMessageId);

    /**
     * Admin-revokes a status (story) previously posted to a newsletter owned by this account.
     *
     * <p>This is the channel status/story revoke, distinct from
     * {@link #revokeNewsletterMessage(JidProvider, String)}: it removes a
     * status published to the newsletter's status feed rather than a message
     * from its main timeline. The revoke rides on a {@code <status>} root
     * addressed to the newsletter and identifying the status by
     * {@code statusId}.
     *
     * @apiNote
     * Drives the "Delete" affordance on a newsletter status from the channel
     * admin context: revokes the published status identified by
     * {@code statusId} so followers no longer see it.
     *
     * @param newsletter the newsletter JID hosting the status; never {@code null}
     * @param statusId   the target status id; never {@code null}
     * @throws NullPointerException            if {@code newsletter} or {@code statusId} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterRevokeStatusAction", exports = "revokeNewsletterStatusAction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    void revokeNewsletterStatus(JidProvider newsletter, String statusId);

    /**
     * Accepts a pending newsletter admin invitation addressed to this account.
     *
     * @apiNote
     * Drives the "Accept admin invite" affordance on the newsletter
     * admin-invite modal: this account becomes an administrator of
     * the newsletter referenced by the supplied JID.
     *
     * @param newsletter the newsletter JID whose pending admin invite is being accepted; never {@code null}
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void acceptNewsletterAdminInvite(JidProvider newsletter);

    /**
     * Revokes a pending admin invitation previously issued by this account for the given newsletter.
     *
     * @apiNote
     * Drives the "Cancel admin invite" affordance on the newsletter
     * admin-invite management surface: revokes a pending admin
     * invitation previously issued by this account against the
     * supplied invitee. Only the channel owner may issue the call;
     * the server rejects the request from non-owners. Pairs with
     * {@link #createNewsletterAdminInvite(JidProvider, JidProvider)}
     * and {@link #queryNewsletterPendingInvites(JidProvider)}.
     *
     * @param newsletter the newsletter JID whose pending invite is being revoked; never {@code null}
     * @param admin      the JID of the user whose pending invite is being revoked; never {@code null}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #createNewsletterAdminInvite(JidProvider, JidProvider)
     * @see #queryNewsletterPendingInvites(JidProvider)
     */
    void revokeNewsletterAdminInvite(JidProvider newsletter, JidProvider admin);

    /**
     * Queries the capability flags granted to this account on the given newsletter.
     *
     * @apiNote
     * Drives the per-channel admin UI gating in the newsletter admin
     * surfaces. Capabilities are the typed feature gates the relay
     * turns on for a given channel (insights dashboards, polls and
     * quizzes, music attachments, sticker pack sharing, the
     * channel-status producer API, etc.). They determine which
     * affordances embedders can surface for the authenticated admin
     * and which operations the relay will accept for this channel.
     *
     * @param newsletter the newsletter JID whose admin capabilities are being queried; never {@code null}
     * @return the capabilities granted to this account on the channel; never {@code null}
     * @throws NullPointerException           if {@code newsletter} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterCapability> queryNewsletterAdminCapabilities(JidProvider newsletter);

    /**
     * Queries the number of administrators currently configured on the given newsletter.
     *
     * @apiNote
     * Backs the "X admins" badge surfaced on the channel info screen.
     *
     * @param newsletter the newsletter JID; never {@code null}
     * @return an {@link OptionalLong} carrying the admin count, or empty when the relay did not report one
     * @throws NullPointerException           if {@code newsletter} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    OptionalLong queryNewsletterAdminsCount(JidProvider newsletter);

    /**
     * Queries a page of followers for the given newsletter.
     *
     * @apiNote
     * Backs the follower roster surface channel admins use to inspect
     * their subscriber base and identify who currently holds an admin
     * role. Each {@link NewsletterFollower} carries the follower JID,
     * the optional push-name and disclosed phone number, the
     * channel-relative role (subscriber, admin, owner), and the
     * moment at which the follow happened. Admins and owners are
     * sorted ahead of subscribers in the returned page.
     *
     * @param newsletter the newsletter JID; never {@code null}
     * @param count      the requested follower page size; the caller should clamp it against the server maximum
     * @return the followers reported on this page, in server order; never {@code null}
     * @throws NullPointerException           if {@code newsletter} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterFollower> queryNewsletterFollowers(JidProvider newsletter, int count);

    /**
     * Queries the list of pending administrator invitations attached to the given newsletter.
     *
     * @apiNote
     * Backs the "Pending invites" list on the newsletter admin-invite
     * management surface. Newsletter owners issue admin invitations
     * via {@link #createNewsletterAdminInvite(JidProvider, JidProvider)};
     * this query returns those invitations that have been issued but
     * not yet accepted via
     * {@link #acceptNewsletterAdminInvite(JidProvider)} or revoked via
     * {@link #revokeNewsletterAdminInvite(JidProvider, JidProvider)}.
     *
     * @param newsletter the newsletter JID whose pending admin invitations are being listed; never {@code null}
     * @return the pending invitations, in server order; never {@code null}
     * @throws NullPointerException           if {@code newsletter} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterAdminInvite> queryNewsletterPendingInvites(JidProvider newsletter);

    /**
     * Queries a paginated page of the newsletter directory described by the given query.
     *
     * @apiNote
     * Powers the explore tab of the newsletter directory: the query's
     * {@link NewsletterDirectoryListQuery#view() view} selects one of
     * {@code RECOMMENDED}, {@code NEW}, {@code POPULAR},
     * {@code FEATURED}, or {@code TRENDING}, and its optional country
     * and category filters narrow the slice. The returned page bundles
     * the directory entries together with a forward-only cursor that
     * callers feed back through
     * {@link NewsletterDirectoryListQuery#cursorToken()} to fetch the
     * following page; leave the cursor unset on the first call.
     *
     * @param query the directory query carrying the view and the optional
     *              country/category filters, page size and pagination
     *              cursor; never {@code null}
     * @return the directory page bundling the entries and the next-page cursor; never {@code null}
     * @throws NullPointerException           if {@code query} or its view is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    NewsletterDirectoryPage queryNewsletterDirectoryList(NewsletterDirectoryListQuery query);

    /**
     * Searches the newsletter directory for channels matching the given free-text query.
     *
     * @apiNote
     * Convenience overload of
     * {@link #searchNewsletterDirectory(String, List, Long, String, boolean)}
     * that drives the directory search box with no category filter
     * and the relay's default page size.
     *
     * @param searchText the free-text search hint
     * @return the first page of matching results
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    NewsletterDirectoryPage searchNewsletterDirectory(String searchText);

    /**
     * Searches the newsletter directory and narrows the result set to the given editorial categories.
     *
     * @apiNote
     * Convenience overload of
     * {@link #searchNewsletterDirectory(String, List, Long, String, boolean)}
     * that retains the relay's default page size and skips the
     * status-metadata sub-selection.
     *
     * @param searchText the free-text search hint
     * @param categories the upper-case category wire strings to filter by, or {@code null} for no category filter
     * @return the first page of matching results
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    NewsletterDirectoryPage searchNewsletterDirectory(String searchText, List<String> categories);

    /**
     * Searches the newsletter directory with full control over filters, paging and status-metadata projection.
     *
     * @apiNote
     * Drives the directory search box on the newsletter explore tab.
     * Pagination follows the same cursor protocol as
     * {@link #queryNewsletterDirectoryList(NewsletterDirectoryListQuery)};
     * pass {@code null} on the initial call and feed back the cursor
     * returned by {@link NewsletterDirectoryPage#nextCursor()} on
     * subsequent calls.
     *
     * @param searchText          the free-text search query, or {@code null}
     * @param categories          the upper-case category wire strings to filter by, or {@code null}
     * @param limit               the page size, or {@code null} to let the relay apply its default
     * @param cursorToken         the pagination cursor, or {@code null} on the first page
     * @param fetchStatusMetadata {@code true} to request the optional {@code status_metadata} sub-selection
     * @return the directory page bundling the entries and the next-page cursor; never {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    NewsletterDirectoryPage searchNewsletterDirectory(String searchText, List<String> categories, Long limit, String cursorToken, boolean fetchStatusMetadata);

    /**
     * Queries the newsletter directory landing categories together with a preview of featured channels for each category.
     *
     * @apiNote
     * Powers the directory landing surface: each category is shown
     * together with a handful of featured newsletters as a visual
     * preview before the user drills down. The {@code input} argument
     * is threaded opaquely through to the server so callers can pass
     * a serialised filter context (per-category limit, country code,
     * category set) without this method having to know the wire shape.
     *
     * @param input the serialised input variable forwarded to the relay; may be {@code null}
     * @return the categories surfaced on the directory landing screen, each carrying a featured-newsletter preview; never {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterDirectoryCategory> queryNewsletterDirectoryCategoriesPreview(String input);

    /**
     * Queries the recommended-newsletters feed personalised for this account.
     *
     * @apiNote
     * Powers the "Recommended for you" rail on the newsletter explore
     * tab. The recommendation engine runs entirely server-side; the
     * query only supplies the page size and an optional country scope,
     * each of which may be left unset to take the relay's default.
     *
     * @param query the recommendation query carrying the optional page
     *              size, country scope and status-metadata flag; never {@code null}
     * @return the directory page bundling the recommended entries and the next-page cursor; never {@code null}
     * @throws NullPointerException            if {@code query} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    NewsletterDirectoryPage queryRecommendedNewsletters(RecommendedNewslettersQuery query);

    /**
     * Queries newsletters similar to the seed newsletter described by the given query.
     *
     * @apiNote
     * Powers the "you might also like" rail on a newsletter channel
     * page. The relay computes similarity entirely server-side; the
     * query supplies the seed JID, the optional page size, and an
     * optional country scope.
     *
     * @param query the similarity query carrying the seed newsletter JID
     *              and the optional page size and country scope; never {@code null}
     * @return the similar newsletters reported by the relay; never {@code null}
     * @throws NullPointerException            if {@code query} or its seed newsletter is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterDirectoryEntry> querySimilarNewsletters(SimilarNewslettersQuery query);

    /**
     * Queries the server-side link preview for a URL pasted into a newsletter compose surface.
     *
     * @apiNote
     * Drives the inline link-preview chip that appears under a URL
     * pasted into the newsletter compose box. Newsletter messages
     * cannot use the regular client-side link-preview pipeline
     * because the recipient anonymity guarantee forbids the client
     * from fetching the target URL directly; instead, the server
     * acts as a trusted proxy and returns title, description,
     * dimensions, and an encrypted thumbnail handle that can be
     * downloaded through the standard media pipeline.
     *
     * @param url the URL to unfurl; never {@code null}
     * @return an {@link Optional} carrying the unfurled preview, or {@link Optional#empty()} when the relay returned no payload
     * @throws NullPointerException if {@code url} is {@code null}
     */
    Optional<NewsletterLinkPreview> queryNewsletterLinkPreview(String url);

    /**
     * Checks whether the given URL belongs to a domain whose link previews may be rendered inside newsletter messages.
     *
     * @apiNote
     * Drives the compose-time warning shown when a pasted URL points
     * to a domain that newsletter integrity has blocked from preview
     * unfurling. Compose surfaces call this query before publishing
     * so they can warn the user when a disallowed domain would
     * otherwise be silently stripped of its preview by the server.
     *
     * @param url the URL whose domain is being validated; never {@code null}
     * @return {@code true} when the relay reports the domain as previewable, {@code false} otherwise
     * @throws NullPointerException if {@code url} is {@code null}
     */
    boolean isNewsletterDomainPreviewable(String url);

    /**
     * Queries the per-emoji list of senders that reacted to the given newsletter message.
     *
     * @apiNote
     * Backs the "who reacted with X" surface inside a newsletter
     * message details panel. Each reaction code is reported together
     * with its sender roster (JID plus optional profile picture
     * direct path); the query returns reactions for every code
     * present on the message, so callers can filter to a single code
     * client-side.
     *
     * @param message the newsletter message whose reactions to read; never {@code null}
     * @return the per-emoji reactor list, in server order; never {@code null}
     * @throws NullPointerException           if {@code message} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterReactor> queryNewsletterMessageReactionSenders(NewsletterMessageInfo message);

    /**
     * Queries the list of voters on a newsletter poll across every option.
     *
     * @apiNote
     * Convenience overload of
     * {@link #queryNewsletterPollVoters(NewsletterMessageInfo, long, String)}
     * that forwards a {@code null} option hash, so the relay returns
     * voters bucketed by every option of the poll.
     *
     * @param message the newsletter poll message whose voters to read; never {@code null}
     * @param limit   the maximum voter edges per option
     * @return the per-option voter groups
     * @throws NullPointerException           if {@code message} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterPollVoter> queryNewsletterPollVoters(NewsletterMessageInfo message, long limit);

    /**
     * Queries the list of voters on a newsletter poll, optionally narrowed to a single option.
     *
     * @apiNote
     * Powers the per-option voter breakdown on a newsletter poll
     * message: newsletter polls track voters per option through a
     * base64-encoded option hash. Passing a non-{@code null}
     * {@code voteHash} narrows the response to senders of that
     * specific option; passing {@code null} returns voters across
     * every option of the poll.
     *
     * @param message  the newsletter poll message whose voters to read; never {@code null}
     * @param limit    the maximum number of voter edges to return
     * @param voteHash the base64-encoded option hash to filter on, or {@code null} to return voters across every option
     * @return the per-option voter groups, in server order; never {@code null}
     * @throws NullPointerException           if {@code message} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterPollVoter> queryNewsletterPollVoters(NewsletterMessageInfo message, long limit, String voteHash);

    /**
     * Transfers ownership of the given newsletter to the supplied user.
     *
     * @apiNote
     * Drives the "Transfer ownership" affordance on the newsletter
     * owner settings sheet: only the current owner may initiate the
     * transfer, and the target user must already have a registered
     * WhatsApp account. After the transfer succeeds the original
     * owner is demoted to admin and the supplied user becomes the
     * sole owner.
     *
     * @param newsletter the newsletter JID whose ownership is being transferred; never {@code null}
     * @param newOwner   the JID of the user receiving ownership; never {@code null}
     * @throws NullPointerException           if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    void transferNewsletterOwnership(JidProvider newsletter, JidProvider newOwner);

    /**
     * Issues a newsletter administrator invitation to the given user.
     *
     * @apiNote
     * Drives the "Invite as admin" affordance on the newsletter
     * admin-invite management surface: the owner of a newsletter
     * invites another user to become a co-administrator. The
     * invitation is recorded server-side and surfaces to the invitee
     * through {@link #queryNewsletterPendingInvites(JidProvider)};
     * the invitee accepts via
     * {@link #acceptNewsletterAdminInvite(JidProvider)} or the
     * inviter revokes via
     * {@link #revokeNewsletterAdminInvite(JidProvider, JidProvider)}.
     *
     * @param newsletter the newsletter JID whose admin roster is being expanded; never {@code null}
     * @param invitee    the JID of the user being invited; never {@code null}
     * @return the persisted invitation, carrying the invitee JID and the expiration instant; never {@code null}
     * @throws NullPointerException           if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    NewsletterAdminInvite createNewsletterAdminInvite(JidProvider newsletter, JidProvider invitee);

    /**
     * Attaches the legally-required paid-partnership disclosure label to a newsletter message.
     *
     * @apiNote
     * Drives the "Mark as paid partnership" affordance on a newsletter
     * post owned by this account: monetised creators must disclose
     * paid partnerships under EU DSA Article 26, and this call flags
     * an existing message so the server renders the disclosure badge
     * alongside it.
     *
     * @param message the newsletter message to label; never {@code null}
     * @throws NullPointerException           if {@code message} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    void addNewsletterPaidPartnershipLabel(NewsletterMessageInfo message);

    /**
     * Logs a batch of newsletter exposure events for attribution and directory-ranking purposes.
     *
     * @apiNote
     * Feeds the server-side newsletter ranking signal: while the user
     * browses newsletters the client records lightweight exposure
     * entries (newsletter JID plus capability flag) and flushes them
     * to the relay through this call. The backend uses the exposure
     * signal to improve directory ranking; the client treats the
     * response as a fire-and-forget acknowledgement.
     *
     * @param exposures the batch of exposure entries; never {@code null}; an empty list still issues the no-op call
     * @throws NullPointerException            if {@code exposures} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    void logNewsletterExposures(List<NewsletterExposure> exposures);

    /**
     * Files an appeal against an enforcement decision recorded on the given report.
     *
     * @apiNote
     * Drives the "Appeal" affordance surfaced to newsletter admins
     * and channel owners after a server-side enforcement action (ban,
     * content removal, geographic suspension): {@code reason} carries
     * the free-form justification entered by the user and
     * {@code reportId} identifies the underlying decision being
     * contested.
     *
     * @param reason   the free-form appeal justification; never {@code null}
     * @param reportId the identifier of the report whose enforcement decision is being contested; never {@code null}
     * @return the persisted appeal record; never {@code null}
     * @throws NullPointerException           if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload or omitted the appeal record
     */
    NewsletterReportAppeal createNewsletterReportAppeal(String reason, String reportId);

    /**
     * Queries the active enforcement actions recorded against the given newsletter.
     *
     * @apiNote
     * Backs the channel enforcement panel newsletter admins inspect
     * to view pending or resolved actions taken on their channel.
     * Enforcements come in four categories (profile-picture
     * deletions, suspensions, violating messages, geographic
     * suspensions), each carrying the violation metadata, the appeal
     * state, and the localised explanatory copy.
     *
     * @param newsletter the newsletter JID; never {@code null}
     * @param locale     the BCP-47 locale tag used to localise the explanatory copy, or {@code null} to fall back to the relay's default
     * @return the enforcements taken on this channel, flattened into a single list; never {@code null}
     * @throws NullPointerException           if {@code newsletter} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterEnforcement> queryNewsletterEnforcements(JidProvider newsletter, String locale);

    /**
     * Queries the list of moderation reports filed against newsletters owned by this account.
     *
     * @apiNote
     * Backs the creator self-management surface where the owner
     * inspects which reports have been filed, their current status,
     * and any associated appeal records. The relay returns every
     * report filed against newsletters this account has authority
     * over; there is no per-newsletter scoping variable.
     *
     * @return the moderation reports filed against newsletters this account has authority over; never {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterReport> queryNewsletterReports();

    /**
     * Queries the analytics insights for the given newsletter.
     *
     * @apiNote
     * Backs the newsletter admin analytics surface (views, reactions,
     * follower-growth deltas). Passing {@code null} for
     * {@code metrics} requests every metric the relay is willing to
     * expose to this caller. Each returned metric also carries the
     * last-update time and freshness status reported for the insights
     * snapshot.
     *
     * @param newsletter the newsletter JID; never {@code null}
     * @param metrics    the list of metric identifiers to fetch values for, or {@code null} to omit the field and let the relay pick the default metric set
     * @return the metrics reported by the relay, in server order; never {@code null}
     * @throws NullPointerException           if {@code newsletter} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload
     */
    List<NewsletterInsightMetric> queryNewsletterInsights(JidProvider newsletter, List<String> metrics);

    /**
     * Initiates a Data Subject Request for the given entity.
     *
     * @apiNote
     * Drives the GDPR-mandated download/deletion flow surfaced to
     * newsletter admins. Although it reads like a "get info"
     * operation, the relay treats it as a mutation because submitting
     * the request kicks off a backend export or deletion job. The
     * returned reference number lets the caller later check progress
     * through privacy-tooling surfaces.
     *
     * @param entityId the identifier of the entity whose DSR is being initiated; never {@code null}
     * @return the relay-assigned reference number; never {@code null}
     * @throws NullPointerException           if {@code entityId} is {@code null}
     * @throws WhatsAppServerRuntimeException if the relay returned no payload or omitted the reference number
     */
    String queryNewsletterDsbInfo(String entityId);

    /**
     * Queries the "about" status text of the given user.
     *
     * @apiNote
     * Backs the biographical line surfaced on a contact's profile
     * (for example {@code "At the movies"} or {@code "Busy"}); this
     * is the persistent profile line, distinct from the ephemeral
     * text status shown on the status tab. The method picks the
     * canonical transport at runtime so callers do not need to know
     * which path was used:
     * <ul>
     *   <li>If the target is a LID JID, or the
     *       {@code mex_usync_about_status} AB prop is enabled, the
     *       query routes through the USync about-status projection.</li>
     *   <li>Otherwise it routes through the GraphQL about query.</li>
     * </ul>
     *
     * @param jid the user JID whose about text should be fetched
     * @return the about text when the server responds with a non-empty {@code <status>} element; {@link Optional#empty()} otherwise
     * @throws NullPointerException            if {@code jid} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<String> queryAbout(JidProvider jid);

    /**
     * Refreshes the cached "about" status text of the given user from the server.
     *
     * <p>Fetches the value via {@link #queryAbout(JidProvider)}, then stores it and notifies listeners:
     * when {@code jid} is the logged-in account the value updates {@link LinkedWhatsAppAccountStore#selfTextStatus()}
     * and fires {@link LinkedWhatsAppClientListener#onAboutChanged}; otherwise it updates the per-contact
     * text-status cache and fires {@link LinkedWhatsAppClientListener#onContactTextStatus}.
     *
     * @apiNote
     * Use this to keep the local text-status cache current; use
     * {@link #queryAbout(JidProvider)} to fetch a user's about without
     * touching the store.
     *
     * @param jid the user JID whose about text should be refreshed
     * @return the about text when present; {@link Optional#empty()} otherwise
     * @throws NullPointerException            if {@code jid} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<String> refreshAbout(JidProvider jid);

    /**
     * Queries the WhatsApp username currently claimed by the authenticated account.
     *
     * @apiNote
     * Drives the "Your username" row on the account-settings sheet.
     * Usernames complement phone numbers as an alternative identifier.
     * The server also returns the registration state (PENDING or
     * ACTIVE) and the recovery-PIN hash; this convenience helper
     * projects only the username because the auxiliary fields are
     * consumed internally by the sign-up surface.
     *
     * @return the claimed username, or {@link Optional#empty()} when the account has no username set or the relay returned no payload
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<String> queryUsername();

    /**
     * Claims, updates or probes the WhatsApp username for the authenticated account.
     *
     * @apiNote
     * Drives the "Set username" / "Change username" affordance on the
     * account-settings sheet: passing a non-empty value assigns the
     * username to the account. Passing a {@code null} or empty value
     * forwards an empty payload, which the relay treats as a probe
     * that exercises the mutation without committing a username.
     *
     * @param username the candidate username to claim, or {@code null}/empty to forward an empty variables payload
     * @return {@code true} when the relay reports {@code "SUCCESS"}, {@code false} when it answers with any other status token or no payload
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    boolean editUsername(String username);

    /**
     * Sets or rotates the recovery PIN attached to the WhatsApp username claimed by the authenticated account.
     *
     * @apiNote
     * Drives the "Username PIN" rotation surface: the recovery PIN
     * backs the username so the account can be regained if the
     * registered phone number becomes unavailable. Passing a
     * {@code null} value clears the PIN.
     *
     * @param pin the new recovery PIN to register, or {@code null} to clear the PIN
     * @throws WhatsAppServerRuntimeException  if the relay rejects the mutation, returns no payload or replies with any token other than {@code "SUCCESS"}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    void editUsernameRecoveryKey(String pin);

    /**
     * Checks whether the given candidate username is available for registration on the WhatsApp relay.
     *
     * @apiNote
     * Drives the live-validation probe behind the username picker UI.
     * The server returns both an availability result and an array of
     * suggested alternatives generated when the candidate is taken
     * or invalid; this helper projects only the boolean.
     *
     * @param candidate the candidate username to validate; {@code null} forwards an empty payload, treated by the relay as a no-op probe
     * @return {@code true} when the relay reports the candidate as available, {@code false} when it is rejected or the relay returned no payload
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    boolean checkUsernameAvailability(String candidate);

    /**
     * Removes the authenticated user's username, clearing it on the relay and locally.
     *
     * <p>Dispatches the set-username mutation with an empty input, which the relay interprets as a
     * deletion. On success the stored username, registration state, and recovery-PIN flag are cleared.
     *
     * @apiNote There is no separate recovery-PIN removal; clearing the username clears its recovery PIN.
     * @return {@code true} if the relay confirmed the removal, {@code false} otherwise
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    boolean removeUsername();

    /**
     * Publishes or clears the authenticated user's ephemeral text status.
     *
     * @apiNote
     * Drives the "Text status" card on the profile drawer: the new
     * value surfaces next to the user's name on every linked device.
     * Pass both {@code text} and {@code emoji} as {@code null} (or
     * {@code text} empty) to clear the published status.
     *
     * @param text              the status body, or {@code null} / empty to
     *                          clear the existing status
     * @param emoji             the optional emoji decoration, or
     *                          {@code null} to publish without an emoji
     * @param ephemeralDuration the auto-expiry duration; {@code null} or
     *                          {@link Duration#ZERO} disables auto-expiry
     * @throws IllegalArgumentException        if {@code ephemeralDuration}
     *                                         is negative
     * @throws WhatsAppServerRuntimeException  if the relay rejects the
     *                                         mutation, returns no payload,
     *                                         or replies with any token
     *                                         other than {@code "SUCCESS"}
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    void editTextStatus(String text, String emoji, Duration ephemeralDuration);

    /**
     * Publishes or clears the authenticated user's ephemeral text status with a typed emoji decoration.
     *
     * <p>Convenience overload of {@link #editTextStatus(String, String, Duration)} that publishes the
     * {@linkplain WhatsAppEmoji#value() canonical value} of {@code emoji} as the decoration, or no
     * decoration when {@code emoji} is {@code null}.
     *
     * @param text              the status body, or {@code null} / empty to clear the existing status
     * @param emoji             the optional emoji decoration, or {@code null} to publish without an emoji
     * @param ephemeralDuration the auto-expiry duration; {@code null} or {@link Duration#ZERO} disables auto-expiry
     * @throws IllegalArgumentException        if {@code ephemeralDuration} is negative
     * @throws WhatsAppServerRuntimeException  if the relay rejects the mutation
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @see #editTextStatus(String, String, Duration)
     */
    default void editTextStatus(String text, WhatsAppEmoji emoji, Duration ephemeralDuration) {
        editTextStatus(text, emoji == null ? null : emoji.value(), ephemeralDuration);
    }

    /**
     * Refreshes the cached text statuses published by one or more users from the server.
     *
     * <p>Caches each returned status into the per-contact text-status cache and fires
     * {@link LinkedWhatsAppClientListener#onContactTextStatus} for every author that has a status set.
     *
     * @apiNote
     * Backs the read path that powers the contact-card "Text status"
     * badge and the chat-list status preview. Each returned
     * {@link ContactTextStatus} carries the status text, the optional
     * emoji, the author-relative last-update timestamp, and the
     * ephemeral duration so callers can reproduce the expiry
     * countdown. Authors that have not published a text status are
     * omitted from the result map.
     *
     * @param users the user JIDs whose text status should be fetched
     * @return a {@link Map} from queried JID to its parsed
     *         {@link ContactTextStatus}, never {@code null} and empty when
     *         the relay returned no payload or none of the queried users
     *         has a status set
     * @throws NullPointerException            if {@code users} is
     *                                         {@code null} or contains a
     *                                         {@code null} entry
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    Map<Jid, ContactTextStatus> refreshUserTextStatuses(List<? extends JidProvider> users);

    /**
     * Fetches the latest linked-identity (LID) rotation pair for the
     * authenticated account.
     *
     * @apiNote
     * Used by callers reconciling local storage after the server
     * rotates an account's LID during the LID-migration rollout: the
     * returned pair maps the old LID to the new LID and lets the
     * caller rewrite any persisted references.
     *
     * @return an {@link Optional} carrying the parsed {@link LidChange}
     *         rotation pair, or {@link Optional#empty()} when the relay
     *         returned no payload
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    Optional<LidChange> queryLidChangeNotification();

    /**
     * Fetches the current Oblivious HTTP for Initiation (OHAI) key
     * configuration advertised by the WhatsApp relay.
     *
     * @apiNote
     * Drives the HPKE key-bundle refresh used to encapsulate ACS
     * (Account Centre Service) requests. Each entry carries a key id,
     * a last-updated time, and the public key bytes; callers cache
     * the bundle and refetch when the relay rotates the set.
     *
     * @return an unmodifiable list of {@link OhaiKeyConfig} entries
     *         advertised by the relay, empty when the relay returned no
     *         payload
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    List<OhaiKeyConfig> queryOhaiKeyConfig();

    /**
     * Fetches the first page of products from a WhatsApp Business catalog.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #queryBusinessCatalog(JidProvider, int)} with the
     * default page size of {@code 5}.
     *
     * @param businessJid the business JID whose catalog should be fetched
     * @return the list of parsed {@link BusinessCatalogEntry} instances on
     *         the first page; never {@code null} and empty when the
     *         catalog has no products or the response is absent
     * @throws NullPointerException if {@code businessJid} is {@code null}
     */
    List<BusinessCatalogEntry> queryBusinessCatalog(JidProvider businessJid);

    /**
     * Fetches the first page of products from a WhatsApp Business catalog,
     * using a caller-chosen page size.
     *
     * @apiNote
     * Backs the storefront product grid customers see on a merchant's
     * profile. The returned list preserves server-side ordering so
     * that callers paginating manually across successive calls
     * observe a stable traversal.
     *
     * @param businessJid the business JID whose catalog should be fetched
     * @param limit       the maximum number of products per page;
     *                    any positive value is accepted and the relay
     *                    clamps silently
     * @return the list of parsed {@link BusinessCatalogEntry} instances on
     *         the first page; never {@code null} and empty when the
     *         catalog has no products or the response is absent
     * @throws NullPointerException     if {@code businessJid} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code limit} is not positive
     */
    List<BusinessCatalogEntry> queryBusinessCatalog(JidProvider businessJid, int limit);

    /**
     * Fetches the first page of collections inside a WhatsApp Business
     * catalog.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #queryBusinessCollections(JidProvider, int)} with the
     * default collection page size of {@code 5} and the default
     * per-collection product preview of {@code 100}.
     *
     * @param businessJid the business JID whose collections should be
     *                    fetched
     * @return the list of parsed {@link BusinessCatalog} instances on the
     *         first page; never {@code null} and empty when the catalog
     *         has no collections or the response is absent
     * @throws NullPointerException if {@code businessJid} is {@code null}
     */
    List<BusinessCatalog> queryBusinessCollections(JidProvider businessJid);

    /**
     * Fetches the first page of collections inside a WhatsApp Business
     * catalog, using a caller-chosen page size.
     *
     * @apiNote
     * Backs the storefront collection-browser customers see on a
     * merchant's profile. The number of products previewed inside each
     * collection defaults to {@code 100}; use
     * {@link #queryBusinessCollections(JidProvider, int, int)} to choose
     * a different per-collection preview size.
     *
     * @param businessJid the business JID whose collections should be
     *                    fetched
     * @param limit       the maximum number of collections per page
     * @return the list of parsed {@link BusinessCatalog} instances on the
     *         first page; never {@code null} and empty when the catalog
     *         has no collections or the response is absent
     * @throws NullPointerException     if {@code businessJid} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code limit} is not positive
     */
    List<BusinessCatalog> queryBusinessCollections(JidProvider businessJid, int limit);

    /**
     * Fetches the first page of collections inside a WhatsApp Business
     * catalog, choosing both the collection page size and how many
     * products are previewed inside each collection.
     *
     * @apiNote
     * Backs the storefront collection-browser customers see on a
     * merchant's profile. Each collection in the result previews up to
     * {@code itemLimit} of its products; raise it to render larger
     * collection thumbnails or lower it to fetch lighter pages.
     *
     * @param businessJid the business JID whose collections should be
     *                    fetched
     * @param limit       the maximum number of collections per page
     * @param itemLimit   the maximum number of products previewed inside
     *                    each returned collection
     * @return the list of parsed {@link BusinessCatalog} instances on the
     *         first page; never {@code null} and empty when the catalog
     *         has no collections or the response is absent
     * @throws NullPointerException     if {@code businessJid} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code limit} or
     *                                  {@code itemLimit} is not positive
     */
    List<BusinessCatalog> queryBusinessCollections(JidProvider businessJid, int limit, int itemLimit);

    /**
     * Checks whether a WhatsApp Business catalog services a given
     * postcode.
     *
     * @apiNote
     * Backs the "Delivers to {area}" pre-flight badge the cart UI
     * shows before letting the buyer commit. The verdict is one of
     * {@link BusinessPostcodeVerificationResult#SUCCESS},
     * {@link BusinessPostcodeVerificationResult#INVALID_POSTCODE}, or
     * {@link BusinessPostcodeVerificationResult#UNSERVICEABLE_LOCATION};
     * the optional {@code encryptedLocationName} carries the resolved
     * area name (encrypted under the merchant's direct-connection
     * key) that callers decrypt to render the cart-UI hint.
     *
     * @param businessJid                   the JID of the business whose
     *                                      service area is being tested
     * @param directConnectionEncryptedInfo the opaque direct-connection
     *                                      envelope carrying the
     *                                      buyer-side postcode
     * @return the parsed verification verdict and the optional encrypted
     *         location name; never {@code null}
     * @throws NullPointerException            if any argument is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         client- or server-error
     *                                         variant or an unrecognised
     *                                         {@code result_code}
     */
    BusinessPostcodeVerification verifyBusinessPostcode(JidProvider businessJid, String directConnectionEncryptedInfo);

    /**
     * Re-prices a business shopping cart against the merchant's current
     * catalog and returns the authoritative cart.
     *
     * @apiNote
     * Backs the "review order" step the cart UI runs before the
     * place-order tap. The response carries an updated cart-wide
     * total and one per-line entry with the rebuilt name, price,
     * currency, media, remaining stock count, and any active
     * sale-price window; lines whose product was removed surface
     * their server-side status code so the UI can grey them out. The
     * {@link BusinessCartRefresh} bundles the merchant JID, the cart
     * product ids in server-side order, the desired thumbnail
     * dimensions, and an optional direct-connection envelope used by
     * merchants enrolled in the encrypted direct-connection feature.
     *
     * @param refresh the cart-refresh request bundling the business JID,
     *                ordered product ids, thumbnail size, and optional
     *                direct-connection envelope
     * @return the freshly-rebuilt cart with up-to-date prices and
     *         availability for every line; never {@code null}
     * @throws NullPointerException            if {@code refresh} or any
     *                                         non-nullable field is
     *                                         {@code null}
     * @throws IllegalArgumentException        if the product id list is
     *                                         empty
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         client- or server-error
     *                                         variant
     */
    BusinessRefreshedCart refreshBusinessCart(BusinessCartRefresh refresh);

    /**
     * Resolves a click-to-WhatsApp (CTWA) deep-link invite into the
     * originating ad's metadata.
     *
     * @apiNote
     * Used by business clients to populate the system "context" bubble
     * that quotes the Facebook / Instagram ad which opened a brand-new
     * chat (or to stamp the next outgoing message's
     * {@link ContextInfo#externalAdReply()}). The
     * {@code expectedSourceUrl} is replayed back to the server so it
     * can reject replays where the invite code was lifted out of
     * context. The returned record carries every ad-side field
     * WhatsApp surfaces in-app: thumbnail bytes (or CDN URL),
     * headline, body, video URL, source-app identifier, and the
     * WAMO-AGM greeting / payload / image fields used by AGM-enrolled
     * advertisers.
     *
     * @param businessJid       the JID of the business advertised in the
     *                          ad
     * @param inviteCode        the invite code embedded in the
     *                          click-to-WhatsApp deep link
     * @param expectedSourceUrl the deep-link URL the client landed on,
     *                          replayed back to the server for replay
     *                          rejection
     * @return the parsed ad context; never {@code null}
     * @throws NullPointerException            if any argument is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     * @throws NoSuchElementException          if the server returned no
     *                                         context or an incomplete
     *                                         response
     */
    BusinessCtwaContext queryCtwaContext(JidProvider businessJid, String inviteCode, String expectedSourceUrl);

    /**
     * Reconciles the local view of the Blocked Contacts privacy list
     * with the server.
     *
     * @apiNote
     * Use after the user toggles a block from another paired device,
     * or whenever the Blocked Contacts surface should redraw against
     * an authoritative copy. The blocked-contact set on
     * {@link LinkedWhatsAppStore} is replaced with the server's view, and
     * {@link LinkedWhatsAppClientListener#onContactBlocked} fires once per
     * contact whose blocked flag flipped. If WhatsApp is in the
     * middle of migrating phone numbers to LID identifiers on this
     * account, the refresh refuses to commit a list that would be
     * addressed against the wrong identifier kind for the current
     * device, raises the matching
     * {@link WhatsAppLidMigrationException} subtype, and leaves the
     * local list untouched.
     *
     * @throws WhatsAppLidMigrationException.StateDiscrepancy        if
     *         the server's view of the LID migration disagrees with
     *         the local view
     * @throws WhatsAppLidMigrationException.BlocklistChatDbUnmigrated
     *         if the server returns a LID-addressed list before this
     *         device has finished migrating its chats to LID and the
     *         primary has not yet sent the mapping payload
     */
    void refreshBlockList();

    /**
     * Blocks a contact at the server.
     *
     * @apiNote
     * Drives the "Block contact" action: after this returns, the
     * contact can no longer send messages or see this account's
     * presence. On success the contact is added to the
     * {@link LinkedWhatsAppStore} block list eagerly.
     *
     * @param contact the contact to block
     * @throws NullPointerException           if {@code contact} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the relay rejected the
     *                                        request
     */
    void blockContact(JidProvider contact);

    /**
     * Unblocks a contact at the server.
     *
     * @apiNote
     * Drives the "Unblock contact" action: the contact is restored to
     * the normal messaging and presence channels. On success the
     * contact is removed from the {@link LinkedWhatsAppStore} block list
     * eagerly.
     *
     * @param contact the contact to unblock
     * @throws NullPointerException           if {@code contact} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the relay rejected the
     *                                        request
     */
    void unblockContact(JidProvider contact);

    /**
     * Reconciles the local view of one marketing-message opt-out list
     * category with the server.
     *
     * @apiNote
     * Use after the user toggles a marketing opt-out from another
     * paired device, or whenever the marketing-message settings
     * surface for that category should redraw against an
     * authoritative copy. The new entries replace
     * {@link LinkedWhatsAppSettingsStore#optOutListEntries(String)} for the
     * category, and {@link LinkedWhatsAppClientListener#onOptOutList} fires
     * with the new set. Reads the cached digest from
     * {@link LinkedWhatsAppSettingsStore#optOutListHash(String)} so an unchanged
     * server-side view is a no-op.
     *
     * @param category the opt-out category
     * @throws NullPointerException            if {@code category} is
     *                                         {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected
     *                                         the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void refreshOptOutList(String category);

    /**
     * Reconciles the local view of one privacy-axis contact blacklist
     * with the server.
     *
     * @apiNote
     * Use to redraw the privacy settings surface for one axis (for
     * example {@code "last"}, {@code "profile"}, {@code "status"},
     * {@code "online"}) against an authoritative copy. The
     * addressing-mode parameter chooses between the phone-number and
     * the LID variant. The new entries replace
     * {@link LinkedWhatsAppSettingsStore#contactBlacklistEntries(String)} for the
     * category, and {@link LinkedWhatsAppClientListener#onContactBlacklist}
     * fires with the new set. Reads the cached digest from
     * {@link LinkedWhatsAppSettingsStore#contactBlacklistHash(String)} so an
     * unchanged server-side view is a no-op.
     *
     * @param category       the privacy axis category name
     * @param addressingMode the JID addressing mode to request
     * @throws NullPointerException            if any argument is
     *                                         {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay returned an
     *                                         error response
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void refreshContactBlacklist(String category, ContactBlacklistAddressingMode addressingMode);

    /**
     * Adds or removes a contact on the marketing-message opt-out list.
     *
     * @apiNote
     * Drives the per-business marketing opt-out toggle: applies the
     * requested membership change for the supplied JID, category, and
     * action, alongside the analytics metadata ({@code reason},
     * {@code entry_point}, {@code signup_id}) and an optional opt-out
     * duration override. This is the marketing-messaging counterpart
     * of {@link #blockContact(JidProvider)} /
     * {@link #unblockContact(JidProvider)}.
     *
     * @param update the {@link OptOutListUpdate} bundling the target JID,
     *               category, action, optional digest, and optional
     *               analytics metadata
     * @throws NullPointerException            if {@code update} or any
     *                                         required field is
     *                                         {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void updateOptOutList(OptOutListUpdate update);

    /**
     * Fetches the profile picture URL for the given JID.
     *
     * @apiNote
     * Drives the contact-card / chat-list avatar fetch: returns the
     * picture URL when present, or {@link Optional#empty()} when the
     * target has no picture set.
     *
     * @param jid the JID whose picture URL should be fetched
     * @return the picture URL when present; {@link Optional#empty()}
     *         otherwise
     * @throws NullPointerException            if {@code jid} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<URI> queryPicture(JidProvider jid);

    /**
     * Refreshes the logged-in account's own profile picture from the server.
     *
     * <p>Fetches the picture URL for the authenticated account, stores it into
     * {@link LinkedWhatsAppAccountStore#profilePicture()}, and fires
     * {@link LinkedWhatsAppClientListener#onProfilePictureChanged} for the account JID.
     *
     * @apiNote
     * Use this to keep the account's own avatar in the store current;
     * use {@link #queryPicture(JidProvider)} to fetch an arbitrary
     * contact's picture without touching the store.
     *
     * @return the account's picture URL when present;
     *         {@link Optional#empty()} otherwise
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<URI> refreshPicture();

    /**
     * Changes the push name (broadcast display name) for this account.
     *
     * @apiNote
     * Backs the "Your name" edit on the profile drawer: writes the
     * new name into {@link LinkedWhatsAppAccountStore#setName(String)} and
     * notifies listeners.
     *
     * @param newPushName the new broadcast display name
     * @throws NullPointerException            if {@code newPushName} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editName(String newPushName);

    /**
     * Changes this account's "About" status text.
     *
     * @apiNote
     * Backs the "About" edit on the profile drawer: blocks until the
     * server acknowledges the new biographical line.
     *
     * @param aboutText the new about text
     * @throws NullPointerException            if {@code aboutText} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editAbout(String aboutText);

    /**
     * Changes this account's profile picture.
     *
     * @apiNote
     * Backs the "Change profile picture" tap on the profile drawer:
     * the full-size JPEG (drained from the supplied stream) and a
     * 96x96 preview thumbnail (generated locally) are uploaded
     * together. The {@link SizedInputStream} must yield a fresh,
     * readable stream.
     *
     * @param jpeg the full-size JPEG payload as a sized stream
     * @throws NullPointerException            if {@code jpeg} is {@code null}
     *                                         or yields a {@code null} stream
     * @throws IllegalArgumentException        if the supplied bytes are not
     *                                         a valid image
     * @throws java.io.UncheckedIOException    if reading the supplied stream
     *                                         fails
     * @throws IllegalStateException           if the self JID is not
     *                                         known
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editProfilePicture(SizedInputStream jpeg);

    /**
     * Removes this account's profile picture.
     *
     * @apiNote
     * Backs the "Remove profile picture" tap on the profile drawer:
     * the picture is dropped server-side and the contact card falls
     * back to the default avatar on every device that observes this
     * account.
     *
     * @throws IllegalStateException           if the self JID is not
     *                                         known
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void removeProfilePicture();

    /**
     * Broadcasts this client's own presence state, reusing the store's
     * push name.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #editPresence(ContactStatus, String)} with
     * {@link LinkedWhatsAppAccountStore#name()} as the display-name override.
     *
     * @param status {@link ContactStatus#AVAILABLE} or
     *               {@link ContactStatus#UNAVAILABLE}
     * @throws NullPointerException            if {@code status} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code status} is not
     *                                         {@code AVAILABLE} or
     *                                         {@code UNAVAILABLE}
     * @throws WhatsAppSessionException.Closed if the socket has been
     *                                         closed
     */
    void editPresence(ContactStatus status);

    /**
     * Broadcasts this client's own presence state with an explicit
     * display-name override.
     *
     * @apiNote
     * Drives the foreground/background presence broadcast emitted
     * when a session gains or loses focus: announces
     * {@link ContactStatus#AVAILABLE} or
     * {@link ContactStatus#UNAVAILABLE} together with the supplied
     * push name (when non-{@code null}) so the relay can forward it
     * to peers.
     *
     * @param status       {@link ContactStatus#AVAILABLE} or
     *                     {@link ContactStatus#UNAVAILABLE}
     * @param presenceName optional display name override; may be
     *                     {@code null}
     * @throws NullPointerException            if {@code status} is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code status} is not
     *                                         {@code AVAILABLE} or
     *                                         {@code UNAVAILABLE}
     * @throws WhatsAppSessionException.Closed if the socket has been
     *                                         closed
     */
    void editPresence(ContactStatus status, String presenceName);

    /**
     * Sends a chat-state indication (typing, recording, or paused) for a
     * conversation.
     *
     * @apiNote
     * Drives the "typing..." / "recording audio..." footer the
     * remote peer sees while the user composes. The accepted states
     * are:
     * <ul>
     *   <li>{@link ContactStatus#COMPOSING} for a text-in-progress
     *       indicator</li>
     *   <li>{@link ContactStatus#RECORDING} for a voice-message
     *       recording indicator</li>
     *   <li>{@link ContactStatus#UNAVAILABLE} to clear any
     *       in-progress indicator</li>
     * </ul>
     *
     * @param chat  the chat JID the state applies to
     * @param state {@link ContactStatus#COMPOSING},
     *              {@link ContactStatus#RECORDING}, or
     *              {@link ContactStatus#UNAVAILABLE} (mapped to paused)
     * @throws NullPointerException            if any argument is
     *                                         {@code null}
     * @throws IllegalArgumentException        if {@code state} is not one
     *                                         of {@code COMPOSING},
     *                                         {@code RECORDING}, or
     *                                         {@code UNAVAILABLE}
     * @throws WhatsAppSessionException.Closed if the socket has been
     *                                         closed
     */
    void editChatState(JidProvider chat, ContactStatus state);

    /**
     * Subscribes to real-time presence updates for a contact.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #subscribeToPresence(JidProvider, String, JidProvider)}
     * with {@code null} for both the display-name override and the
     * context scope. After the server accepts the subscribe, presence
     * pushes for the target flow until the subscription is cancelled
     * via {@link #unsubscribeFromPresence(JidProvider)} or the socket
     * closes.
     *
     * @param target the contact JID to subscribe to
     * @throws NullPointerException            if {@code target} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been
     *                                         closed
     */
    void subscribeToPresence(JidProvider target);

    /**
     * Subscribes to real-time presence updates for a target, with an
     * optional display name and context scope.
     *
     * @apiNote
     * Drives the presence-push subscription that powers the "online"
     * / "last seen" footer on chat headers and the per-participant
     * indicators on community / group surfaces.
     *
     * @param presenceTo      the JID to subscribe to
     * @param presenceName    optional display name; may be {@code null}
     * @param presenceContext optional context JID for community / group
     *                        surfaces; {@code null} for the default 1:1
     *                        chat scope
     * @throws NullPointerException            if {@code presenceTo} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void subscribeToPresence(JidProvider presenceTo, String presenceName, JidProvider presenceContext);

    /**
     * Cancels a presence subscription previously established with
     * {@link #subscribeToPresence(JidProvider)}.
     *
     * @apiNote
     * Symmetric counterpart to {@link #subscribeToPresence(JidProvider)}:
     * after this returns, no further presence push notifications
     * arrive for the contact until a new subscription is opened.
     *
     * @param target the contact JID to unsubscribe from
     * @throws NullPointerException            if {@code target} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket has been
     *                                         closed
     */
    void unsubscribeFromPresence(JidProvider target);

    /**
     * Sends a fresh message to a chat and returns the key that identifies it.
     *
     * @apiNote
     * Primary outgoing-message entry point: prepares the raw
     * {@link LinkedMessageContainer} into a populated {@link ChatMessageInfo}
     * (or {@link NewsletterMessageInfo} for newsletter JIDs),
     * encrypts per-device, and dispatches through the chat, group,
     * status, or newsletter sender appropriate to the JID server.
     *
     * <p>Poll messages are created and voted on through this same entry
     * point. Send a {@code PollCreationMessage} (name, options,
     * selectable count) to create a poll: the send pipeline adopts the
     * message's per-message secret as the poll's vote-encryption key. To
     * cast a vote, send a {@code PollUpdateMessage} built with its
     * {@code pollCreationMessageKey} and {@code selectedOptions}; the
     * pipeline resolves the referenced poll-creation message and encrypts
     * the vote in place.
     *
     * @param jid       the destination chat JID
     * @param container the message payload to send
     * @return the {@link MessageKey} of the dispatched message
     * @throws NullPointerException                           if any
     *                                                        argument is
     *                                                        {@code null}
     * @throws WhatsAppMessageException.Send.InvalidRecipient if the JID
     *                                                        does not
     *                                                        match a
     *                                                        supported
     *                                                        chat type
     */
    @Override
    MessageKey sendMessage(JidProvider jid, LinkedMessageContainer container);

    /**
     * Sends a pre-built {@link LinkedMessageInfo} without re-running the
     * preparer pipeline.
     *
     * @apiNote
     * Use this overload when the caller has assembled a
     * {@link ChatMessageInfo} or {@link NewsletterMessageInfo} with a
     * message id, timestamp, and any extension metadata already
     * populated; typical scenarios are rehydrating a draft or
     * re-transmitting a message that failed a previous send. Wraps
     * the same dispatch
     * {@link #sendMessage(JidProvider, LinkedMessageContainer)} uses.
     *
     * @param messageInfo the fully-populated outgoing message
     * @throws NullPointerException                           if
     *                                                        {@code messageInfo}
     *                                                        is
     *                                                        {@code null}
     * @throws WhatsAppMessageException.Send.InvalidRecipient if the JID
     *                                                        does not
     *                                                        match a
     *                                                        supported
     *                                                        chat type
     */
    void sendMessage(LinkedMessageInfo messageInfo);

    /**
     * Edits the body of a previously sent message.
     *
     * @apiNote
     * Drives the "Edit message" action: the replacement payload is
     * wrapped in a protocol-message envelope and dispatched through
     * the standard send pipeline so every linked device reconciles
     * the change. The edit window is enforced server-side; late edits
     * are rejected by the server.
     *
     * @param originalKey the key of the message to edit; must carry a
     *                    {@code parentJid} identifying the chat
     * @param newContent  the replacement message container
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code originalKey} has no
     *                                  {@code parentJid}
     */
    void editMessage(MessageKey originalKey, LinkedMessageContainer newContent);

    /**
     * Deletes a message locally or for every participant in the chat.
     *
     * @apiNote
     * Drives the "Delete for me" / "Delete for everyone" actions in
     * the message context menu. When {@code everyone} is {@code false}
     * the message is removed from the local store.
     *
     * @param key      the key of the message to delete
     * @param everyone {@code true} to delete for every participant
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} has no
     *                                  {@code parentJid}, or (when
     *                                  {@code everyone} is {@code false})
     *                                  no {@code id}
     */
    void deleteMessage(MessageKey key, boolean everyone);

    /**
     * Posts a new status update to {@code status@broadcast}.
     *
     * @apiNote
     * Drives the "Add status" surface: prepares the content (message
     * id, message secret, device context info) and routes it through
     * the status-specific sender, which applies sender-key encryption
     * to the current status audience. The returned
     * {@link ChatMessageInfo} is the persisted model row callers
     * reference later (for example to revoke the post via
     * {@link #deleteStatus(String)}).
     *
     * @param content the raw status body (text, image, video, sticker
     *                etc.)
     * @return the persisted message info for the new status
     * @throws NullPointerException  if {@code content} is {@code null}
     * @throws IllegalStateException if the client is not logged in or the
     *                               message could not be stored after
     *                               sending
     */
    ChatMessageInfo sendStatus(LinkedMessageContainer content);

    /**
     * Revokes a previously posted status update.
     *
     * @apiNote
     * Drives the "Delete status" tap on the status-tray surface:
     * issues a revoke protocol message addressed at
     * {@code status@broadcast} carrying the key of the original status
     * post. The status-specific sender handles device-list narrowing
     * and the direct-fanout fallback when recipients have left the
     * audience.
     *
     * @param statusId the id of the status message to revoke
     * @throws NullPointerException  if {@code statusId} is {@code null}
     * @throws IllegalStateException if the client is not logged in
     */
    void deleteStatus(String statusId);

    /**
     * Reshares an existing status or chat message as the user's own status.
     *
     * @apiNote
     * Mirrors the status-reshare composer flow: the source message's text or media is re-posted as a
     * fresh status carrying a {@link com.github.auties00.cobalt.wire.linked.message.status.StatusAttribution.Type#RESHARE}
     * attribution crediting the original. Text is copied directly; image and video media are
     * re-downloaded and re-uploaded so the reshared status owns independent media. Resharing a
     * newsletter status adds a channel-reshare attribution identifying the originating channel and
     * its server message id.
     *
     * @param sourceKey the key of the message to reshare
     * @return the posted status message
     * @throws NullPointerException     if {@code sourceKey} is {@code null}
     * @throws IllegalArgumentException if the source message is absent or its type cannot be reshared
     *                                  as a status
     * @throws IllegalStateException    if the client is not logged in
     */
    ChatMessageInfo reshareStatus(MessageKey sourceKey);

    /**
     * Eagerly establishes the Signal sessions for a chat's devices ahead of the first send.
     *
     * @apiNote
     * Mirrors WhatsApp's chat-open session warm-up: resolves the chat's device fan-out and fetches
     * pre-key bundles for any device that does not yet have a session, so the first message to the
     * chat encrypts without a server round-trip. Best-effort: a device whose pre-keys cannot be
     * fetched is skipped. Commits one {@code PrekeysDepletion} metric (with the
     * {@code USER_INTENT_PREFETCH} fetch reason) per depleted one-time pre-key.
     *
     * @param chat the chat whose sessions to pre-establish
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void eagerlyEstablishSession(JidProvider chat);

    /**
     * Emits a {@code read} receipt for a viewed status update.
     *
     * @apiNote
     * Drives the "viewed" tick the author sees on their status entry:
     * a read receipt is delivered to the status broadcast addressed
     * at the original status author. The author JID is resolved from
     * the cached status message's sender.
     *
     * @param statusId the id of the viewed status message
     * @throws NullPointerException   if {@code statusId} is {@code null}
     * @throws NoSuchElementException if no status with the given id is
     *                                cached or it has no sender JID
     */
    void markStatusViewed(String statusId);

    /**
     * Reconciles the local view of the Status privacy panel with the
     * server.
     *
     * @apiNote
     * Use to redraw the Status privacy panel against an authoritative
     * copy of the selected distribution mode and the JID list paired
     * with the whitelist or except-contacts modes. The new value
     * replaces {@link LinkedWhatsAppSettingsStore#statusPrivacy()} and
     * {@link LinkedWhatsAppClientListener#onStatusPrivacyChanged} fires
     * with the new value.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void refreshStatusPrivacy();

    /**
     * Changes the Status privacy configuration.
     *
     * @apiNote
     * Drives the "Status privacy" panel write: applies the new
     * distribution mode server-side and the local
     * {@link LinkedWhatsAppSettingsStore#statusPrivacy() status-privacy setting} in the store is
     * updated eagerly.
     *
     * @param mode the new distribution mode
     * @param jids the JID list applied by
     *             {@link StatusPrivacyMode#WHITELIST} and
     *             {@link StatusPrivacyMode#CONTACTS_EXCEPT}; may be empty
     *             or {@code null} for {@link StatusPrivacyMode#CONTACTS}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    void editStatusPrivacy(StatusPrivacyMode mode, Collection<? extends JidProvider> jids);

    /**
     * Forwards a single message to a single destination chat.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #forwardMessages(Collection, Collection)} with singleton
     * collections; matches the single-message branch of the forward
     * picker. PSA-message forwards also fire the corresponding
     * chat-PSA telemetry event.
     *
     * @param sourceKey   the key of the message to forward
     * @param destination the destination chat JID
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the source message cannot be
     *                                  resolved in the local store
     */
    void forwardMessage(MessageKey sourceKey, JidProvider destination);

    /**
     * Forwards a set of messages to every destination chat.
     *
     * @apiNote
     * Drives the multi-select "Forward to" picker: resolves each
     * source key against the local store, extracts the
     * {@link LinkedMessageContainer}, and sends it to every destination in
     * turn. Unresolvable source keys and unsendable destinations are
     * skipped silently.
     *
     * @param sourceKeys   the keys of the messages to forward
     * @param destinations the destination chat JIDs
     * @throws NullPointerException if any argument is {@code null}
     */
    void forwardMessages(Collection<MessageKey> sourceKeys, Collection<? extends JidProvider> destinations);

    /**
     * Adds or replaces this account's reaction on a message.
     *
     * @apiNote
     * Drives the emoji-reaction picker shown when long-pressing a
     * message bubble: builds a reaction message keyed to the target
     * and dispatches it through the standard send pipeline; the
     * reaction is automatically wrapped in an encrypted-reaction
     * envelope when the target chat is a CAG community subgroup.
     * When {@code messageKey} identifies a newsletter message (its
     * {@code parentJid} is a newsletter JID) the reaction is instead
     * published through the newsletter reaction stanza, keyed by the
     * cached message's server id, so the same entry point covers both
     * regular chats and channels. Sending an empty emoji is equivalent
     * to {@link #removeReaction(MessageKey)}.
     *
     * @param messageKey the key of the message being reacted to
     * @param emoji      the reaction emoji; empty string removes the
     *                   existing reaction
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code messageKey} has no
     *                                  {@code parentJid}
     * @throws NoSuchElementException   if {@code messageKey} targets a
     *                                  newsletter message that is not in
     *                                  the local message cache
     */
    void addReaction(MessageKey messageKey, String emoji);

    /**
     * Removes this account's reaction from a message.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #addReaction(MessageKey, String)} with the empty string;
     * the empty value is the wire signal to withdraw the sender's
     * previous reaction.
     *
     * @param messageKey the key of the message whose reaction should be
     *                   removed
     * @throws NullPointerException     if {@code messageKey} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code messageKey} has no
     *                                  {@code parentJid}
     */
    void removeReaction(MessageKey messageKey);

    /**
     * Stars (bookmarks) a message so it appears in the account's starred
     * messages list.
     *
     * @apiNote
     * Drives the star tap on a message bubble: the change is
     * propagated to every linked device via the {@code REGULAR_HIGH}
     * app-state sync collection. PSA-message stars also fire the
     * corresponding chat-PSA telemetry event.
     *
     * @param key the key of the message to star
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} has no
     *                                  {@code parentJid} or no
     *                                  {@code id}
     */
    void starMessage(MessageKey key);

    /**
     * Unstars a previously starred message.
     *
     * @apiNote
     * Counterpart to {@link #starMessage(MessageKey)}: emits the
     * {@code REGULAR_HIGH} sync mutation and flips the local star
     * flag back off.
     *
     * @param key the key of the message to unstar
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} has no
     *                                  {@code parentJid} or no
     *                                  {@code id}
     */
    void unstarMessage(MessageKey key);

    /**
     * Archives a chat.
     *
     * @apiNote
     * Drives the "Archive chat" swipe / context-menu action: the
     * change propagates to every linked device via the
     * {@code REGULAR_LOW} app-state sync collection, and archiving
     * also queues an unpin mutation. The local
     * {@link Chat#setArchived(Boolean)} flag is flipped eagerly so
     * callers observe the change without waiting for the sync
     * round-trip.
     *
     * @param chat the JID of the chat to archive
     * @throws NullPointerException if {@code chat} is {@code null}
     * @see #unarchiveChat(JidProvider)
     */
    void archiveChat(JidProvider chat);

    /**
     * Unarchives a chat.
     *
     * @apiNote
     * Drives the "Unarchive chat" swipe / context-menu action: the
     * change propagates to every linked device via the
     * {@code REGULAR_LOW} app-state sync collection. The local
     * {@link Chat#setArchived(Boolean)} flag is flipped eagerly so
     * callers observe the change without waiting for the sync
     * round-trip.
     *
     * @param chat the JID of the chat to unarchive
     * @throws NullPointerException if {@code chat} is {@code null}
     * @see #archiveChat(JidProvider)
     */
    void unarchiveChat(JidProvider chat);

    /**
     * Pins a chat.
     *
     * @apiNote
     * Drives the pin context-menu action: the change propagates to
     * every linked device via the {@code REGULAR_LOW} app-state sync
     * collection, and the local
     * {@link Chat#setPinnedTimestamp(Instant)} is updated eagerly.
     * Pinning forces {@link Chat#setArchived(Boolean)} to
     * {@code false} so a pinned chat cannot stay archived.
     *
     * @param chat the JID of the chat to pin
     * @throws NullPointerException if {@code chat} is {@code null}
     * @see #unpinChat(JidProvider)
     */
    void pinChat(JidProvider chat);

    /**
     * Unpins a chat.
     *
     * @apiNote
     * Drives the unpin context-menu action: the change propagates to
     * every linked device via the {@code REGULAR_LOW} app-state sync
     * collection, and the local
     * {@link Chat#setPinnedTimestamp(Instant)} is cleared eagerly.
     *
     * @param chat the JID of the chat to unpin
     * @throws NullPointerException if {@code chat} is {@code null}
     * @see #pinChat(JidProvider)
     */
    void unpinChat(JidProvider chat);

    /**
     * Mutes a chat until the given instant.
     *
     * @apiNote
     * Drives the "Mute notifications" dialog: the change propagates
     * to every linked device via the {@code REGULAR_HIGH} app-state
     * sync collection, and the mute state is applied to the local
     * {@link Chat} eagerly. Pass {@code null} to unmute; an
     * {@link Instant} with epoch second {@code -1} is the sentinel
     * for "muted indefinitely".
     *
     * @param chat      the JID of the chat to mute
     * @param muteUntil the instant at which the mute should expire, or
     *                  {@code null} to unmute
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void muteChat(JidProvider chat, Instant muteUntil);

    /**
     * Unmutes a chat.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #muteChat(JidProvider, Instant)} with {@code null} for
     * {@code muteUntil}, restoring the chat's default notification
     * behaviour.
     *
     * @param chat the JID of the chat to unmute
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void unmuteChat(JidProvider chat);

    /**
     * Marks a chat as read.
     *
     * @apiNote
     * Drives the implicit "seen" signal fired when a chat receives
     * focus: the change propagates to every linked device via the
     * {@code REGULAR_LOW} app-state sync collection, and the local
     * {@link Chat#setMarkedAsUnread(Boolean)} and
     * {@link Chat#setUnreadCount(Integer)} flags are cleared eagerly.
     *
     * @param chat the JID of the chat to mark as read
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void markChatAsRead(JidProvider chat);

    /**
     * Marks a chat as unread.
     *
     * @apiNote
     * Drives the "Mark as unread" context-menu tap: the change
     * propagates to every linked device via the {@code REGULAR_LOW}
     * app-state sync collection. The local
     * {@link Chat#setMarkedAsUnread(Boolean)} flag is set eagerly,
     * along with the sentinel unread-count value that marks the chat
     * as "manually unread".
     *
     * @param chat the JID of the chat to mark as unread
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void markChatAsUnread(JidProvider chat);

    /**
     * Empties a chat while keeping the chat row itself.
     *
     * @apiNote
     * Drives the "Clear messages" confirmation: the change propagates
     * to every linked device via the {@code REGULAR_HIGH} app-state
     * sync collection, and {@link Chat#removeMessages()} is applied
     * eagerly so callers observe the chat emptied without waiting
     * for the sync round-trip.
     *
     * @param chat        the JID of the chat to clear
     * @param keepStarred {@code true} to preserve starred messages,
     *                    {@code false} to delete them with the rest
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void clearChat(JidProvider chat, boolean keepStarred);

    /**
     * Deletes a chat entirely from the local store and every linked
     * device.
     *
     * @apiNote
     * Drives the "Delete chat" confirmation: the change propagates
     * to every linked device via the {@code REGULAR_HIGH} app-state
     * sync collection, and the chat is removed from the local store
     * eagerly.
     *
     * @param chat the JID of the chat to delete
     * @return the deleted {@link Chat} when it existed locally, or
     *         {@link Optional#empty()} otherwise
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    Optional<Chat> deleteChat(JidProvider chat);

    /**
     * Locks a chat behind the chat-lock PIN.
     *
     * @apiNote
     * Drives the "Lock chat" tap that hides the chat from the main
     * list: queues an unarchive mutation, an unpin mutation, and a
     * lock mutation, then pushes the full set through
     * {@link #pushWebAppState(SyncPatchType, List)}, and mirrors
     * the locked-and-unarchived update on the local {@link Chat}
     * eagerly.
     *
     * @param chat the JID of the chat to lock
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void lockChat(JidProvider chat);

    /**
     * Unlocks a chat, restoring it to the main chat list.
     *
     * @apiNote
     * Counterpart to {@link #lockChat(JidProvider)}: queues only the
     * lock mutation, pushes it through
     * {@link #pushWebAppState(SyncPatchType, List)}, and flips the
     * locked flag back off on the local {@link Chat} eagerly.
     *
     * @param chat the JID of the chat to unlock
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void unlockChat(JidProvider chat);

    /**
     * Creates a new business chat label from the given creation request.
     *
     * @apiNote
     * Drives the "New label" form on the Business labels manager:
     * the {@link LabelCreate#name() name} is mapped to a predefined id
     * when applicable via
     * {@link BusinessLabelConstants#mapLabelNameToPredefinedId(String)}
     * and the label is registered as a regular (non-predefined)
     * custom label.
     *
     * @param create the label creation request carrying the display name
     *               and palette colour index; never {@code null}
     * @return the newly-allocated label id (stringified integer)
     * @throws NullPointerException if {@code create} is {@code null}
     */
    String createLabel(LabelCreate create);

    /**
     * Edits the display name and palette colour of an existing chat label.
     *
     * @apiNote
     * Drives the "Edit label" form on the Business labels manager.
     * Pair this entry point with {@link #createLabel(LabelCreate)}
     * when applications surface a single add-or-edit affordance.
     *
     * @param edit the label edit request carrying the label id, the new
     *             display name and the new palette colour index; never {@code null}
     * @return the updated label, or {@link Optional#empty()} when no label
     *         with the requested id exists in the local store
     * @throws NullPointerException if {@code edit} is {@code null}
     */
    Optional<Label> editLabel(LabelEdit edit);

    /**
     * Deletes an existing chat label along with every chat or contact
     * association registered against it.
     *
     * @apiNote
     * Drives the "Delete label" affordance on the Business labels
     * manager. The accompanying assignments are torn down in the
     * same patch so the relay does not surface stale label badges on
     * the formerly tagged chats.
     *
     * @param labelId the identifier of the label to delete
     * @return the removed label, or {@link Optional#empty()} when no
     *         label with {@code labelId} exists in the local store
     * @throws NullPointerException if {@code labelId} is {@code null}
     */
    Optional<Label> deleteLabel(String labelId);

    /**
     * Deletes an existing chat label along with every chat or contact
     * association registered against it.
     *
     * @apiNote
     * Convenience overload that resolves the label identifier from
     * {@link Label#id()} before delegating to
     * {@link #deleteLabel(String)}.
     *
     * @param label the label to delete
     * @return the removed label, or {@link Optional#empty()} when no
     *         label with the same id exists in the local store
     * @throws NullPointerException if {@code label} is {@code null}
     */
    Optional<Label> deleteLabel(Label label);

    /**
     * Applies a new user-chosen order to the chat labels.
     *
     * @apiNote
     * Drives the drag-to-reorder gesture on the Business labels
     * manager. Submit the full ordered list of label identifiers
     * rather than a delta; entries absent from the list keep whatever
     * {@code orderIndex} they had.
     *
     * @param labelIds the label identifiers in the new display order
     * @throws NullPointerException if {@code labelIds} is {@code null}
     */
    void reorderLabels(List<String> labelIds);

    /**
     * Associates a label with the given chat.
     *
     * @apiNote
     * Drives the "Apply label" affordance on the Business chat list:
     * tags the chat so it surfaces under the matching label in the
     * labels manager.
     *
     * @param labelId the label identifier
     * @param chat    the chat JID to tag with the label
     * @throws NullPointerException if any argument is {@code null}
     */
    void associateLabel(String labelId, JidProvider chat);

    /**
     * Associates a label with the given chat.
     *
     * @apiNote
     * Convenience overload that resolves the label identifier from
     * {@link Label#id()} before delegating to
     * {@link #associateLabel(String, JidProvider)}.
     *
     * @param label the label
     * @param chat  the chat JID to tag with the label
     * @throws NullPointerException if any argument is {@code null}
     */
    void associateLabel(Label label, JidProvider chat);

    /**
     * Dissociates a label from the given chat.
     *
     * @apiNote
     * Removes the tag set by
     * {@link #associateLabel(String, JidProvider)}, driving the
     * "Remove label" affordance on the Business chat list.
     *
     * @param labelId the label identifier
     * @param chat    the chat JID to untag
     * @throws NullPointerException if any argument is {@code null}
     */
    void dissociateLabel(String labelId, JidProvider chat);

    /**
     * Dissociates a label from the given chat.
     *
     * @apiNote
     * Convenience overload that resolves the label identifier from
     * {@link Label#id()} before delegating to
     * {@link #dissociateLabel(String, JidProvider)}.
     *
     * @param label the label
     * @param chat  the chat JID to untag
     * @throws NullPointerException if any argument is {@code null}
     */
    void dissociateLabel(Label label, JidProvider chat);

    /**
     * Associates a label with a specific message.
     *
     * @apiNote
     * Tags an individual message under the named label rather than
     * the enclosing chat, driving the message-level label affordance
     * on the Business chat list. Receivers reconcile the assignment
     * against the message's parent chat record.
     *
     * @param labelId    the label identifier
     * @param messageKey the target message key
     * @throws NullPointerException if any argument is {@code null}
     */
    void associateLabel(String labelId, MessageKey messageKey);

    /**
     * Associates a label with a specific message.
     *
     * @apiNote
     * Convenience overload that resolves the label identifier from
     * {@link Label#id()} before delegating to
     * {@link #associateLabel(String, MessageKey)}.
     *
     * @param label      the label
     * @param messageKey the target message key
     * @throws NullPointerException if any argument is {@code null}
     */
    void associateLabel(Label label, MessageKey messageKey);

    /**
     * Dissociates a label from a specific message.
     *
     * @apiNote
     * Removes the tag set by {@link #associateLabel(String, MessageKey)}.
     *
     * @param labelId    the label identifier
     * @param messageKey the target message key
     * @throws NullPointerException if any argument is {@code null}
     */
    void dissociateLabel(String labelId, MessageKey messageKey);

    /**
     * Dissociates a label from a specific message.
     *
     * @apiNote
     * Convenience overload that resolves the label identifier from
     * {@link Label#id()} before delegating to
     * {@link #dissociateLabel(String, MessageKey)}.
     *
     * @param label      the label
     * @param messageKey the target message key
     * @throws NullPointerException if any argument is {@code null}
     */
    void dissociateLabel(Label label, MessageKey messageKey);

    /**
     * Creates a new business broadcast list with the given display name
     * and recipients.
     *
     * @apiNote
     * Drives the "New broadcast list" creation flow on the Business
     * chat list. Use the returned JID to subsequently send messages
     * with {@link #sendBroadcast(JidProvider, LinkedMessageContainer)}.
     *
     * @param name       the display name of the broadcast list
     * @param recipients the recipient JIDs
     * @return the JID identifying the new broadcast list
     * @throws NullPointerException if any argument is {@code null}
     */
    Jid createBroadcastList(String name, Collection<? extends JidProvider> recipients);

    /**
     * Renames a broadcast list and replaces its recipient set.
     *
     * @apiNote
     * Drives the "Edit broadcast list" affordance on the Business
     * chat list. Submit the full new recipient list rather than a
     * delta: the underlying mutation overwrites the previous
     * participant set on every linked device.
     *
     * @param broadcastListId the JID returned by
     *                        {@link #createBroadcastList(String, Collection)}
     * @param newName         the new display name
     * @param newRecipients   the full new set of recipient JIDs
     * @throws NullPointerException if any argument is {@code null}
     */
    void editBroadcastList(JidProvider broadcastListId, String newName, Collection<? extends JidProvider> newRecipients);

    /**
     * Deletes a broadcast list.
     *
     * @apiNote
     * Drives the "Delete broadcast list" affordance on the Business
     * chat list. Outgoing messages already sent to the list keep
     * their per-recipient delivery state; only the list itself
     * disappears.
     *
     * @param broadcastListId the broadcast list JID
     * @throws NullPointerException if {@code broadcastListId} is {@code null}
     */
    void deleteBroadcastList(JidProvider broadcastListId);

    /**
     * Sends a message to every recipient of the given broadcast list.
     *
     * @apiNote
     * Drives the "Send to broadcast list" action on the Business chat
     * list: the relay fans the payload out to each participant
     * individually so recipients never see each other.
     *
     * @param broadcastListId the broadcast list JID
     * @param message         the message payload
     * @return the resulting chat-message metadata
     * @throws NullPointerException if any argument is {@code null}
     */
    ChatMessageInfo sendBroadcast(JidProvider broadcastListId, LinkedMessageContainer message);

    /**
     * Assigns the given chat to the given agent.
     *
     * @apiNote
     * Drives the "Assign chat" affordance on the Business Premium
     * chat-assignment surface. Pass the empty string for
     * {@code agentId} to clear the current assignment (or use
     * {@link #unassignChatFromAgent(JidProvider)} for a
     * self-documenting call site).
     *
     * @param chat    the chat JID
     * @param agentId the agent identifier
     * @throws NullPointerException if any argument is {@code null}
     */
    void assignChatToAgent(JidProvider chat, String agentId);

    /**
     * Unassigns the given chat from any agent.
     *
     * @apiNote
     * Convenience over
     * {@link #assignChatToAgent(JidProvider, String)} with an empty
     * agent id: drops every existing assignment for the chat in one
     * call.
     *
     * @param chat the chat JID
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void unassignChatFromAgent(JidProvider chat);

    /**
     * Records that the assigned agent has opened the given chat.
     *
     * @apiNote
     * Drives the "agent has read this chat" indicator on the Business
     * Premium chat-assignment surface. Useful so the next time a
     * different agent opens the same chat, the local UI can render
     * whether the originally assigned agent has already seen the
     * latest messages.
     *
     * @param chat the chat JID
     * @throws NullPointerException  if {@code chat} is {@code null}
     * @throws IllegalStateException when the chat is not currently assigned
     * @see #markChatAssignmentUnopened(JidProvider)
     */
    void markChatAssignmentOpened(JidProvider chat);

    /**
     * Records that the assigned agent has not opened the given chat.
     *
     * @apiNote
     * Drives the "agent has read this chat" indicator on the Business
     * Premium chat-assignment surface, clearing the opened marker so the
     * local UI renders the chat as unseen by the originally assigned
     * agent.
     *
     * @param chat the chat JID
     * @throws NullPointerException  if {@code chat} is {@code null}
     * @throws IllegalStateException when the chat is not currently assigned
     * @see #markChatAssignmentOpened(JidProvider)
     */
    void markChatAssignmentUnopened(JidProvider chat);

    /**
     * Reconciles the local view of the account-wide default
     * Disappearing Messages timer with the server.
     *
     * @apiNote
     * Use to redraw the "Default message timer" entry on the Privacy
     * panel against an authoritative copy. The timer is shared
     * across all linked devices. Typically called once after login to
     * seed the "new chat" UI, or after
     * {@link #editDefaultDisappearingMode(ChatEphemeralTimer)} to
     * confirm the change took effect. The new value replaces
     * {@link LinkedWhatsAppSettingsStore#disappearingMode()} and
     * {@link LinkedWhatsAppClientListener#onDisappearingModeChanged}
     * fires.
     *
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws NoSuchElementException          if the server reply contains
     *                                         no disappearing-mode entry
     */
    void refreshDisappearingMode();

    /**
     * Sets the per-chat disappearing-message timer.
     *
     * @apiNote
     * Drives the "Disappearing messages" toggle on the chat info
     * panel; the change is mirrored across both participants of a
     * peer chat or every member of a group, and is recorded as a
     * system message in the conversation. Use
     * {@link #editDefaultDisappearingMode(ChatEphemeralTimer)} when
     * the intent is to change the default for newly-opened chats
     * rather than an existing thread.
     *
     * @param chat  the JID of the chat whose timer is being changed
     * @param timer the new timer; {@link ChatEphemeralTimer#OFF} disables
     *              disappearing messages
     * @throws NullPointerException if any argument is {@code null}
     */
    void editEphemeralTimer(JidProvider chat, ChatEphemeralTimer timer);

    /**
     * Refreshes the server-side acceptance state for the named Terms-of-Service
     * and disclosure notices.
     *
     * <p>Records the identifiers of the returned notices into {@link LinkedWhatsAppSettingsStore#tosNotices()} and fires
     * {@link LinkedWhatsAppClientListener#onTosNoticesChanged}.
     *
     * @apiNote
     * Powers the acceptance prompts WhatsApp shows before unlocking
     * features such as updated ToS, business-bot disclosures and
     * broadcast or newsletter producer disclosures. Applications gating
     * their own feature surface on a notice call this and check the
     * per-notice verdict before allowing the user in. The returned
     * {@link TosNotices#refresh()} value is the server's recommended
     * re-poll cadence; long-running sessions typically schedule the
     * next call after that interval elapses.
     *
     * @param noticeIds the disclosure or ToS identifiers to inspect;
     *                  never {@code null} but possibly empty (an empty
     *                  collection still returns the refresh hint)
     * @return the per-notice acceptance verdicts and the recommended
     *         re-poll interval; never {@code null}
     * @throws NullPointerException            if {@code noticeIds} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     * @throws NoSuchElementException          if the server reply carries
     *                                         no {@link TosNotices} entry
     */
    TosNotices refreshTosNotices(Collection<String> noticeIds);

    /**
     * Cancels a previously-submitted "Request account info" GDPR data
     * export so the server discards it before the report finishes
     * generating.
     *
     * @apiNote
     * Drives the "Cancel data export" affordance under the Settings
     * GDPR panel; useful when an application reverts a request that
     * was issued in error. The server queues the export and emails a
     * download link when ready, so a successful cancellation must
     * race the link-generation deadline.
     *
     * @param reportType which export to cancel:
     *                   {@link GdprReportType#ACCOUNT} for the full
     *                   account report or
     *                   {@link GdprReportType#NEWSLETTERS} for the
     *                   newsletter-only variant; never {@code null}
     * @throws NullPointerException            if {@code reportType} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    void cancelGdprRequest(GdprReportType reportType);

    /**
     * Fetches the WhatsApp Web push-server public key used to encrypt
     * browser push subscriptions before forwarding them to the server.
     *
     * @apiNote
     * Powers the WhatsApp Web PWA push-notification flow: the key
     * encrypts the Service Worker push subscription endpoint and
     * auth secrets before upload, so the server can deliver
     * background pushes without seeing the raw browser-side
     * credentials. Applications wiring up their own browser-style
     * push delivery call this once per session and feed the result
     * into the encryption step before registering the subscription.
     * Most non-browser embedders do not need to call this.
     *
     * @return the base-64 encoded public key when the server published
     *         one; {@link Optional#empty()} when the server returned an
     *         error (an installed
     *         {@link WhatsAppLinkedClientErrorHandler} can recover the
     *         underlying error code for richer diagnostics)
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    Optional<String> queryPushServerKey();

    /**
     * Refreshes the local account's privacy configuration from the server, storing each returned
     * category into {@link LinkedWhatsAppSettingsStore#privacySettings()}.
     *
     * @apiNote
     * Mirrors the Settings privacy panel: refreshes the active audience
     * for every supported {@link PrivacySettingType}. The Status
     * privacy distribution is excluded because it travels on a
     * separate channel and is surfaced by
     * {@link #refreshStatusPrivacy()} instead.
     *
     * @return an immutable map from every recognised
     *         {@link PrivacySettingType} to the
     *         {@link PrivacySettingValue} the server currently enforces;
     *         never {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Map<PrivacySettingType<?>, PrivacySettingValue> refreshPrivacySettings();

    /**
     * Changes a single privacy setting on the local account.
     *
     * @apiNote
     * Drives the per-category selectors on the Settings privacy panel. The supplied
     * {@link PrivacySettingValue} fully describes the change: it names its
     * {@linkplain PrivacySettingValue#type() setting}, carries its audience token, and, for a
     * contacts-except value, carries the inline blocklist through
     * {@link PrivacySettingValue#excluded()}; constructing the value is the only way to pair a
     * setting with an audience it accepts. Observers registered via
     * {@link #addPrivacySettingChangedListener(LinkedPrivacySettingChangedListener)} see the new
     * state after the server acknowledges the change without another round trip.
     *
     * @param value the new privacy value; never {@code null}
     * @throws NullPointerException            if {@code value} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    void editPrivacySetting(PrivacySettingValue value);

    /**
     * Restores a message previously withheld by Defense Mode, re-delivering it to the registered
     * listeners.
     *
     * @apiNote
     * Defense Mode withholds unsolicited messages from senders that are not in the user's address
     * book while the feature is enabled; a withheld message is stored but kept out of the
     * {@code onNewMessage} listeners until restored, either individually through this method or in
     * bulk when Defense Mode is turned off. The message is identified by its {@link MessageKey};
     * restoring a message that does not exist or was not quarantined is a no-op.
     *
     * @param key the key of the quarantined message to restore; never {@code null}
     * @return {@code true} when a quarantined message was restored, {@code false} otherwise
     * @throws NullPointerException if {@code key} is {@code null}
     */
    boolean restoreQuarantinedMessage(MessageKey key);

    /**
     * Exports a chat's message history to a WhatsApp-format ZIP archive.
     *
     * @apiNote
     * Produces a ZIP containing {@code chat.txt} (a plain-text transcript) and {@code chat.md} (a
     * markdown transcript), plus a {@code media/} folder when {@link ChatExportOptions#includeMedia()}
     * is set. Messages are filtered (protocol, reaction, poll-update, keep-in-chat and pin stubs,
     * view-once and ephemeral messages are excluded) and ordered oldest-first. Commits a
     * {@code ChatExport} metric describing the outcome.
     *
     * @param chat    the chat to export
     * @param options the export options
     * @param output  the stream that receives the produced ZIP archive
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the chat is not present in the store
     */
    void exportChat(JidProvider chat, ChatExportOptions options, OutputStream output);

    /**
     * Enables sending and receiving "read" receipts (blue double-ticks).
     *
     * @apiNote
     * Drives the "Read receipts" toggle on the Settings privacy panel;
     * the switch is symmetric. Enabling read receipts lets peers see
     * when this account opens their messages, and in exchange this
     * account sees read receipts from them. Use this convenience when
     * the UI surfaces a simple on/off toggle rather than the broader
     * audience picker driven by
     * {@link #editPrivacySetting(PrivacySettingValue)}.
     *
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     * @see #disableReadReceipts()
     */
    void enableReadReceipts();

    /**
     * Disables sending and receiving "read" receipts (blue double-ticks).
     *
     * @apiNote
     * Drives the "Read receipts" toggle on the Settings privacy panel;
     * the switch is symmetric. When disabled, peers stop seeing when
     * this account opens their messages, but in exchange this account
     * also stops seeing read receipts from them. Use this convenience
     * when the UI surfaces a simple on/off toggle rather than the
     * broader audience picker driven by
     * {@link #editPrivacySetting(PrivacySettingValue)}.
     *
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     * @see #enableReadReceipts()
     */
    void disableReadReceipts();

    /**
     * Issues one or more privacy tokens that bind the local account to a
     * peer's identity.
     *
     * @apiNote
     * Powers the "trusted contact" workflow: when the local user
     * explicitly trusts a peer, this call records a server-side bond
     * that survives either side's device re-pairings, so the peer's
     * identity-key changes do not silently invalidate the relationship
     * and message-receipt privacy decisions can be short-circuited for
     * that contact. Subsequent device-list rebuilds and Signal protocol
     * session resets read the same token back to confirm the peer is
     * still trusted. Issuing a fresh token for a peer who already has
     * one of the same type overwrites the previous record.
     *
     * @param userJid    the peer whose identity the tokens cover; never
     *                   {@code null}
     * @param tokenTypes the token categories to issue; never {@code null},
     *                   must be non-empty
     * @param timestamp  the issue time the server records on each token;
     *                   never {@code null}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalArgumentException        if {@code tokenTypes} is empty
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    void issuePrivacyTokens(JidProvider userJid, Collection<PrivacyTokenType> tokenTypes, Instant timestamp);

    /**
     * Reconciles the local cache of a server-side privacy "disallowed
     * list", returning either a {@code match} verdict or the fresh
     * contact roster paired with the new content digest.
     *
     * @apiNote
     * Privacy disallowed lists are the per-category exclusion rosters
     * backing the {@link PrivacySettingType#LAST_SEEN},
     * {@link PrivacySettingType#PROFILE_PICTURE},
     * {@link PrivacySettingType#ABOUT} and
     * {@link PrivacySettingType#GROUP_ADD} audience selectors.
     * Each category maintains its own sliding digest ({@code dhash}) so
     * reconciliation can hit a server-side fast path when the local
     * cache is up to date. Applications typically call this on first
     * connect for every category they care about, with {@code dhash}
     * set to the digest from the previous run (or {@code ""} on a cold
     * start), and again whenever a server-side 409 conflict on a
     * privacy-set update signals that the digest the client carried in
     * the request was already stale.
     *
     * @param jid      the requesting user's JID; the MEX query wraps the
     *                 single user in its {@code query_input} array; never
     *                 {@code null}
     * @param dhash    the digest of the locally cached roster used for
     *                 delta refreshes; pass {@code ""} (or {@code null}) on
     *                 a cold start so the server returns the full list
     * @param category the privacy domain identifier in MEX form
     *                 ({@code "ABOUT"}, {@code "GROUPADD"}, {@code "LAST"},
     *                 {@code "PROFILE"}); never {@code null}
     * @param type     the allow/deny polarity ({@code "DENYLIST"} for the
     *                 standard exclusion roster); never {@code null}
     * @return the reconciliation verdict; never {@code null}
     * @throws NullPointerException            if {@code jid}, {@code category}
     *                                         or {@code type} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    PrivacyDisallowedList queryPrivacyDisallowedList(JidProvider jid, String dhash, String category, String type);

    /**
     * Reads the current "reachout timelock" enforcement window applied
     * to the authenticated account.
     *
     * @apiNote
     * Surfaces the same compliance verdict WhatsApp shows on the
     * spam-cooldown notice when an account has been flagged for spam
     * reports or suspicious outreach: whether enforcement is
     * currently active, when the window ends, and which enforcement
     * type applies (soft warning, hard block, etc.). The post-login
     * bootstrap invokes this once; applications that want to gate
     * their own send path on the timelock should call it directly
     * and inspect the result.
     *
     * @return the parsed {@link ReachoutTimelock} verdict, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<ReachoutTimelock> queryReachoutTimelock();

    /**
     * Fetches the FMX (first-message-experience) integrity signals the
     * relay attaches to a peer.
     *
     * @apiNote
     * Powers the safety nudges WhatsApp shows when a user starts a
     * conversation with an unfamiliar contact: the
     * {@code is_new_account} flag drives the "new on WhatsApp" badge,
     * while {@code is_suspicious_start_chat} drives the spam-warning
     * sheet. Callers integrating their own start-chat surface can call
     * this once the user composes the first outbound message to drive
     * the same advisories.
     *
     * @param userJid the user JID whose signals are being fetched; never
     *                {@code null}
     * @return the parsed {@link UserIntegritySignals}, or
     *         {@link Optional#empty()} when the relay returned no payload
     *         (for example because the peer is not visible to integrity
     *         scoring)
     * @throws NullPointerException            if {@code userJid} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    Optional<UserIntegritySignals> queryUserIntegritySignals(JidProvider userJid);

    /**
     * Reads the current outbound new-chat messaging quota the server
     * enforces on the authenticated account.
     *
     * @apiNote
     * Surfaces the throttling state for brand-new chat threads opened
     * per billing cycle: total quota, used quota, cycle start and end
     * timestamps, and the status flags ({@code oteStatus},
     * {@code mvStatus}, {@code cappingStatus}) the official clients
     * use to drive the throttling UI and per-message warnings. The
     * post-login bootstrap invokes this once; applications that want
     * to gate their own send path on the capping verdict should call
     * it directly and inspect the result.
     *
     * @return the parsed {@link NewChatMessageCappingInfo}, or
     *         {@link Optional#empty()} when the server returned no envelope
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<NewChatMessageCappingInfo> queryNewChatMessageCappingInfo();

    /**
     * Submits a passkey-signed integrity challenge response.
     *
     * @apiNote
     * Completes the server-initiated integrity handshake: when the
     * relay emits an integrity challenge request, the caller signs
     * the challenge with the user's WebAuthn passkey credential and
     * submits the JSON-serialised assertion through this entry point.
     * There is no automatic post-login invocation; callers wire their
     * own observer on the integrity-challenge notification and invoke
     * this method when the challenge arrives. A clean return signals
     * acceptance; rejection surfaces as
     * {@link WhatsAppServerRuntimeException} carrying the relay-side
     * {@code error_message}. The {@code prfAvailable} argument
     * reflects whether the WebAuthn assertion includes a
     * {@code prf_output} field.
     *
     * @param signedChallenge the JSON-serialised WebAuthn assertion
     *                        bytes; never {@code null}
     * @param prfAvailable    {@code true} when the assertion carries a
     *                        {@code prf_output} field
     * @throws NullPointerException            if {@code signedChallenge}
     *                                         is {@code null}
     * @throws WhatsAppServerRuntimeException  when the relay rejected
     *                                         the challenge; the
     *                                         relay-side
     *                                         {@code error_message} is
     *                                         carried in the exception
     *                                         message
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    void submitPasskeyIntegrityChallenge(byte[] signedChallenge, boolean prfAvailable);

    /**
     * Reads the registered country code for a single user.
     *
     * @apiNote
     * The country code is the two-letter ISO identifier the server
     * derives from the user's phone-number registration.
     *
     * @param userJid the user JID to query; never {@code null}
     * @return the ISO country code, or {@link Optional#empty()} when the
     *         server returned no item or no country code
     * @throws NullPointerException            if {@code userJid} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<String> queryUserCountryCode(JidProvider userJid);

    /**
     * Reads the WhatsApp username record claimed by a single user.
     *
     * @apiNote
     * WhatsApp lets users claim a global username distinct from their
     * phone-number identifier; once claimed, the username is reachable
     * for any user JID. The returned {@link Username} carries the
     * claimed name together with its registration state, the instant it
     * was registered, the recovery PIN bound to it, and the lookup
     * status reported for this user.
     *
     * @param userJid the user JID to query; never {@code null}
     * @return the claimed username record, or {@link Optional#empty()}
     *         when the user has not claimed one or the server returned no
     *         item
     * @throws NullPointerException            if {@code userJid} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<Username> queryUserUsername(JidProvider userJid);

    /**
     * Sets the account-wide default disappearing-message timer for all
     * new chats.
     *
     * @apiNote
     * Drives the "Default message timer" entry on the privacy panel;
     * the timer is applied to every brand-new conversation opened
     * from any linked device. Use
     * {@link #editEphemeralTimer(JidProvider, ChatEphemeralTimer)} to
     * change the timer on an existing chat.
     *
     * @param timer the new default timer; {@link ChatEphemeralTimer#OFF}
     *              disables disappearing messages by default
     * @throws NullPointerException if {@code timer} is {@code null}
     */
    void editDefaultDisappearingMode(ChatEphemeralTimer timer);

    /**
     * Creates a new WhatsApp group with the given subject and initial
     * participants, leaving disappearing messages off.
     *
     * @apiNote
     * Convenience over
     * {@link #createGroup(String, ChatEphemeralTimer, Collection)}
     * preset to {@link ChatEphemeralTimer#OFF}.
     *
     * @param subject      the group display name; never {@code null}
     * @param participants the user JIDs to add on creation; never
     *                     {@code null} and must be non-empty
     * @return the parsed metadata of the freshly created group
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the participant collection is empty
     */
    GroupMetadata createGroup(String subject, Collection<? extends JidProvider> participants);

    /**
     * Creates a new WhatsApp group with the given subject, initial
     * disappearing-message timer and participants.
     *
     * @apiNote
     * Drives the "New group" creation flow on the sidebar. The
     * freshly created group appears in the local store immediately
     * after this call returns.
     *
     * @param subject         the group display name; never {@code null}
     * @param ephemeralTimer  the initial disappearing-message timer;
     *                        {@link ChatEphemeralTimer#OFF} disables it
     * @param participants    the user JIDs to add on creation; never
     *                        {@code null} and must be non-empty
     * @return the parsed metadata of the freshly created group
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the participant collection is empty
     */
    GroupMetadata createGroup(String subject, ChatEphemeralTimer ephemeralTimer, Collection<? extends JidProvider> participants);

    /**
     * Leaves a WhatsApp group, removing the current user from the
     * participant list server-side.
     *
     * @apiNote
     * Drives the "Exit group" affordance on the group info panel.
     * Use {@link #leaveCommunity(JidProvider)} to detach from a
     * community (which exits the parent and every linked subgroup in
     * one round trip) and {@link #leaveGroup(JidProvider...)} to
     * batch multiple exits.
     *
     * @param group the JID of the group to leave; never {@code null}
     * @throws NullPointerException     if {@code group} is {@code null}
     * @throws IllegalArgumentException if the JID is not a group/community
     */
    void leaveGroup(JidProvider group);

    /**
     * Leaves multiple WhatsApp groups in one request.
     *
     * @apiNote
     * Varargs convenience over {@link #leaveGroup(JidProvider)} that
     * batches several JIDs into a single request.
     *
     * @param groups the JIDs of the groups to leave; never {@code null}
     *               or empty
     * @throws NullPointerException     if {@code groups} or any element
     *                                  is {@code null}
     * @throws IllegalArgumentException if {@code groups} is empty or any
     *                                  JID is not a group/community
     */
    void leaveGroup(JidProvider... groups);

    /**
     * Reports whether the given group is flagged as "internal" by the
     * WhatsApp relay.
     *
     * @apiNote
     * Internal groups are the Meta-side staff or testing groups whose
     * lifecycle differs from regular consumer groups. The flag is
     * reported uniformly across regular groups, community parents,
     * default subgroups, and ordinary subgroups.
     *
     * @param groupJid the group JID to query; never {@code null}
     * @return {@code true} if the relay reports the group as internal,
     *         {@code false} otherwise (including when the relay returned
     *         no payload)
     * @throws NullPointerException            if {@code groupJid} is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     */
    boolean isGroupInternal(JidProvider groupJid);

    /**
     * Queries the metadata envelope for a group.
     *
     * @apiNote
     * Convenience over
     * {@link #queryGroupInfo(JidProvider, boolean, String)} that
     * fetches a cold metadata snapshot with no participant-list partial
     * hash.
     *
     * @param group the group JID
     * @return the parsed metadata, or {@link Optional#empty()} when the
     *         relay returned no payload
     * @throws NullPointerException if {@code group} is {@code null}
     */
    Optional<GroupMetadata> queryGroupInfo(JidProvider group);

    /**
     * Queries the full metadata envelope for a group, excluding bot
     * participants from the participant edge list.
     *
     * @apiNote
     * Backs the group info panel for consumer groups. Use
     * {@link #queryGroupInfoIncludingBots(JidProvider, boolean, String)}
     * when the UI renders bots as first-class members.
     *
     * @param group             the group JID to query; never {@code null}
     * @param includeUsername   whether to hydrate the username subtree
     *                          on every participant
     * @param participantsPhash the participant-list partial hash
     *                          carried for incremental refreshes;
     *                          {@code null} on a cold fetch
     * @return the parsed {@link GroupMetadata}, or
     *         {@link Optional#empty()} when the relay returned no payload
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<GroupMetadata> queryGroupInfo(JidProvider group, boolean includeUsername, String participantsPhash);

    /**
     * Queries the full metadata envelope for a group, including bot
     * participants in the participant edge list.
     *
     * @apiNote
     * Used by chat UIs that render bots as first-class members; the
     * non-bot variant is
     * {@link #queryGroupInfo(JidProvider, boolean, String)}.
     *
     * @param group             the group JID to query; never {@code null}
     * @param includeUsername   whether to hydrate the username subtree
     *                          on every participant
     * @param participantsPhash the participant-list partial hash, or
     *                          {@code null} on a cold fetch
     * @return the parsed {@link GroupMetadata}, or
     *         {@link Optional#empty()} when the relay returned no payload
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Optional<GroupMetadata> queryGroupInfoIncludingBots(JidProvider group, boolean includeUsername, String participantsPhash);

    /**
     * Rotates the invite code for the given group, community or contact,
     * invalidating any previously distributed
     * {@code chat.whatsapp.com/<code>} link.
     *
     * @apiNote
     * Drives the "Reset link" affordance on the invite sheet. The
     * {@code entryPoint} telemetry tag is mandatory: every caller
     * must identify the UI surface that triggered the rotation.
     *
     * @param receiver   the receiver JID the code is being minted for
     *                   (group, community or contact); never {@code null}
     * @param entryPoint the UI-surface telemetry tag identifying what
     *                   triggered the rotation, e.g.
     *                   {@code "CHAT_INFO_INVITE_BUTTON"}; never
     *                   {@code null}
     * @return the freshly minted invite-code scalar
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    String createGroupInviteCode(JidProvider receiver, String entryPoint);

    /**
     * Creates a new WhatsApp community from the given creation request.
     *
     * @apiNote
     * Drives the "New community" creation flow on the sidebar; the
     * parent group is created in request-required mode, so additions
     * to subgroups require admin approval. The community appears in
     * the local store immediately after this call returns. An unset
     * {@link CommunityCreate#ephemeralTimer()} leaves disappearing
     * messages off ({@link ChatEphemeralTimer#OFF}).
     *
     * @param create the community creation request carrying the display
     *               name, optional description and disappearing-message
     *               timer; never {@code null}
     * @return the parsed metadata of the freshly created community
     * @throws NullPointerException   if {@code create} is {@code null}
     * @throws NoSuchElementException if the server response does not
     *                                carry a {@code <group>} community
     *                                subtree
     */
    CommunityMetadata createCommunity(CommunityCreate create);

    /**
     * Deactivates a community parent group on the server, turning every
     * linked subgroup into a standalone group.
     *
     * @apiNote
     * Drives the "Deactivate community" affordance on the community
     * admin sheet. The subgroups themselves are not deleted; they
     * survive as regular standalone groups under the same JIDs.
     *
     * @param community the JID of the community to deactivate; never
     *                  {@code null}
     * @throws NullPointerException          if {@code community} is
     *                                       {@code null}
     * @throws IllegalArgumentException      if the JID is not a
     *                                       group/community
     * @throws WhatsAppServerRuntimeException when the relay returns a
     *                                       client- or server-error reply
     */
    void deactivateCommunity(JidProvider community);

    /**
     * Leaves a WhatsApp community, detaching the current user from the
     * parent group and every subgroup transitively.
     *
     * @apiNote
     * Drives the "Exit community" affordance on the community info
     * panel. The exit is applied to the parent and every linked
     * subgroup in one round trip; use
     * {@link #leaveGroup(JidProvider)} to leave a single subgroup
     * without exiting the community.
     *
     * @param community the JID of the community to leave; never
     *                  {@code null}
     * @throws NullPointerException          if {@code community} is
     *                                       {@code null}
     * @throws IllegalArgumentException      if the JID is not a
     *                                       group/community
     * @throws WhatsAppServerRuntimeException when the relay returns a
     *                                       client- or server-error reply
     */
    void leaveCommunity(JidProvider community);

    /**
     * Transfers ownership of a community to one of its existing admins.
     *
     * @apiNote
     * Drives the "Transfer community ownership" affordance on the
     * community admin sheet. The new owner must already be an admin
     * of the community.
     *
     * @param community the community JID; never {@code null}
     * @param newOwner  the JID of the new owner; never {@code null}, must
     *                  be an existing admin of the community
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code community} is not a
     *                                  group/community or {@code newOwner}
     *                                  is a group/community
     */
    void transferCommunityOwnership(JidProvider community, JidProvider newOwner);

    /**
     * Reads the pending subgroup suggestions for a community.
     *
     * @apiNote
     * Surfaces the groups the local user belongs to that the server
     * recommends moving into the community. Drives the "Suggested
     * subgroups" affordance on the community admin sheet, pairing
     * with {@link #approveSubgroupSuggestion} and
     * {@link #rejectSubgroupSuggestion} for the per-candidate verdicts.
     *
     * @param community the community JID to query; never {@code null}
     * @return the list of suggested subgroup JIDs; empty when there are
     *         no pending suggestions
     * @throws NullPointerException     if {@code community} is {@code null}
     * @throws IllegalArgumentException if the JID is not a group/community
     */
    List<Jid> querySubgroupSuggestions(JidProvider community);

    /**
     * Approves a pending subgroup suggestion, accepting the group into
     * the community as an official subgroup.
     *
     * @apiNote
     * The {@link SubgroupSuggestion#suggestionCreator() suggestionCreator}
     * is the user that originally proposed the candidate; it travels
     * through {@link #querySubgroupSuggestions(JidProvider)} on the
     * {@code creator.id} field of each suggestion edge.
     *
     * @param suggestion the suggestion verdict subject carrying the parent
     *                   community, the suggested subgroup and the
     *                   suggestion creator; never {@code null}
     * @throws NullPointerException     if {@code suggestion} is {@code null}
     * @throws IllegalArgumentException if either group JID is not a
     *                                  group/community
     */
    void approveSubgroupSuggestion(SubgroupSuggestion suggestion);

    /**
     * Rejects a pending subgroup suggestion, declining the recommendation
     * to move the group into the community.
     *
     * @apiNote
     * The {@link SubgroupSuggestion#suggestionCreator() suggestionCreator}
     * is the user that originally proposed the candidate; it travels
     * through {@link #querySubgroupSuggestions(JidProvider)} on the
     * {@code creator.id} field of each suggestion edge.
     *
     * @param suggestion the suggestion verdict subject carrying the parent
     *                   community, the suggested subgroup and the
     *                   suggestion creator; never {@code null}
     * @throws NullPointerException     if {@code suggestion} is {@code null}
     * @throws IllegalArgumentException if either group JID is not a
     *                                  group/community
     */
    void rejectSubgroupSuggestion(SubgroupSuggestion suggestion);

    /**
     * Reads the participant count of a single subgroup by piggy-backing
     * on the community-wide participant-count MEX query.
     *
     * @apiNote
     * Cheap counter for the community admin sheet when only the
     * headline number for one subgroup is needed; the projection is
     * the cheapest available because the MEX query returns counts for
     * every subgroup in the same envelope.
     *
     * @param subgroup the subgroup JID to query; never {@code null}
     * @return the total participant count, or {@code -1} when the server
     *         response does not carry a count for the requested subgroup
     * @throws NullPointerException     if {@code subgroup} is {@code null}
     * @throws IllegalArgumentException if the JID is not a group/community
     */
    long querySubgroupParticipantCount(JidProvider subgroup);

    /**
     * Reads the full list of subgroups that belong to a community,
     * projecting each entry into a {@link CommunityLinkedGroup}.
     *
     * @apiNote
     * Drives the "Subgroups" tab on the community info panel. The
     * first entry of the result is always the community's
     * default announcement subgroup
     * ({@link CommunityLinkedGroup#isDefaultSubgroup()} {@code == true});
     * regular subgroups follow in the order the relay surfaced them.
     *
     * @param community the non-{@code null} community JID
     * @return the list of {@link CommunityLinkedGroup} entries belonging
     *         to the community, default subgroup first
     * @throws NullPointerException          if {@code community} is
     *                                       {@code null}
     * @throws IllegalArgumentException      if the JID is not a
     *                                       group/community
     * @throws WhatsAppServerRuntimeException when the relay omits the
     *                                       default-subgroup record or
     *                                       the regular-subgroup edges
     */
    List<CommunityLinkedGroup> querySubgroups(JidProvider community);

    /**
     * Adds one or more participants to a WhatsApp group, returning the
     * per-participant server status.
     *
     * @apiNote
     * Drives the "Add participants" affordance on the group info
     * panel. Inspect the returned status map to surface per-target
     * failures (the addition can succeed for some JIDs and fail for
     * others, for instance when a peer's privacy settings forbid being
     * added to groups).
     *
     * @param group the target group JID; never {@code null}
     * @param toAdd the user JIDs to add; never {@code null} and must be
     *              non-empty
     * @return a map from each target JID to its server-assigned
     *         {@link GroupParticipantStatus};
     *         {@link GroupParticipantStatus#OK} signals success
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the JID is not a group/community
     *                                  or the collection is empty
     */
    Map<Jid, GroupParticipantStatus> addGroupParticipants(JidProvider group, Collection<? extends JidProvider> toAdd);

    /**
     * Registers a server-side pending group "store invite" for invitees who
     * cannot be reached on WhatsApp, returning the per-invitee relay outcome.
     *
     * <p>Backs the SMS fallback for group invitations: when a target phone
     * number is not registered on WhatsApp, the relay stores a pending
     * invitation keyed by that number so the invitee is auto-joined to the
     * group once they register and come online. The returned map pairs each
     * requested invitee, in the supplied order, with an opaque relay
     * {@link OptionalLong} outcome code; an empty {@code OptionalLong} means the
     * invite was stored successfully, while a present value carries the
     * relay-defined failure code for that invitee. The codes are telemetry
     * grade: this method never branches on them or throws on a per-invitee
     * failure, it merely surfaces them. When the relay answers without a
     * result envelope the returned map is empty.
     *
     * @param group    the target group JID; never {@code null}
     * @param invitees the invitee JIDs to store invites for; never
     *                 {@code null} and must be non-empty
     * @return a map from each requested invitee JID to its relay outcome code;
     *         an empty {@link OptionalLong} value signals success and an empty
     *         map signals a missing result envelope
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the JID is not a group/community or
     *                                  the collection is empty
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGroupStoreInviteSmsJob", exports = "mexGroupStoreInviteSms",
            adaptation = WhatsAppAdaptation.ADAPTED)
    Map<Jid, OptionalLong> storeGroupInviteSms(JidProvider group, Collection<? extends JidProvider> invitees);

    /**
     * Removes one or more participants from a WhatsApp group without
     * cascading the eviction to linked subgroups.
     *
     * @apiNote
     * Convenience over
     * {@link #removeGroupParticipants(JidProvider, Collection, boolean)}
     * preset to {@code removeLinkedGroups=false}. Use the three-argument
     * overload to evict the participants from every subgroup of the
     * parent community in one round trip.
     *
     * @param group    the target group JID; never {@code null}
     * @param toRemove the user JIDs to remove; never {@code null} and
     *                 must be non-empty
     * @return a map from each target JID to its server-assigned
     *         {@link GroupParticipantStatus}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code toRemove} is empty
     */
    Map<Jid, GroupParticipantStatus> removeGroupParticipants(JidProvider group, Collection<? extends JidProvider> toRemove);

    /**
     * Removes one or more participants from a group and optionally
     * cascades the eviction across every subgroup of the same community.
     *
     * @apiNote
     * Drives the "Remove participant" affordance on the group info
     * panel; the cascading variant matches the "Remove from
     * community" affordance on the community admin sheet.
     *
     * @param group              the target group JID; never {@code null}
     * @param toRemove           the participants to remove; never
     *                           {@code null} and must be non-empty
     * @param removeLinkedGroups when {@code true} the participants are
     *                           also evicted from every subgroup of the
     *                           parent community in one round trip
     * @return a map from each participant JID to the per-target outcome
     *         {@link GroupParticipantStatus};
     *         {@link GroupParticipantStatus#OK} signals success
     * @throws NullPointerException            if any reference argument
     *                                         is {@code null}
     * @throws IllegalArgumentException        if {@code toRemove} is empty
     * @throws WhatsAppSessionException.Closed if the socket is no longer
     *                                         open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Map<Jid, GroupParticipantStatus> removeGroupParticipants(JidProvider group, Collection<? extends JidProvider> toRemove, boolean removeLinkedGroups);

    /**
     * Promotes one or more participants of a WhatsApp group to
     * administrator.
     *
     * @apiNote
     * Drives the "Make group admin" affordance on the group info
     * panel.
     *
     * @param group      the target group JID; never {@code null}
     * @param toPromote  the user JIDs to promote; never {@code null} and
     *                   must be non-empty
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the JID is not a group/community
     *                                  or the collection is empty
     */
    void promoteGroupParticipants(JidProvider group, Collection<? extends JidProvider> toPromote);

    /**
     * Demotes one or more administrators of a WhatsApp group, community or
     * newsletter to regular members.
     *
     * @apiNote
     * Drives the "Dismiss as admin" affordance on the group info
     * panel. The inverse of
     * {@link #promoteGroupParticipants(JidProvider, Collection)}. When
     * {@code group} is a newsletter JID this demotes each supplied
     * newsletter administrator back to a regular follower, including
     * the admin's own "Step down" action.
     *
     * @param group     the target group, community or newsletter JID; never {@code null}
     * @param toDemote  the user JIDs to demote; never {@code null} and
     *                  must be non-empty
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the JID is not a group, community
     *                                  or newsletter, or the collection is
     *                                  empty
     */
    void demoteGroupParticipants(JidProvider group, Collection<? extends JidProvider> toDemote);

    /**
     * Marks a sticker as a favourite on the user's account.
     *
     * @apiNote
     * Drives the "star" affordance on the sticker picker; the change
     * is mirrored to every linked device, so a sticker
     * starred on one device shows up in the favourites row on every
     * other.
     *
     * @param stickerHash the sticker file hash that uniquely identifies
     *                    the sticker across devices
     * @throws NullPointerException if {@code stickerHash} is {@code null}
     */
    void favoriteSticker(String stickerHash);

    /**
     * Unmarks a sticker as a favourite on the user's account.
     *
     * @apiNote
     * Inverse of {@link #favoriteSticker(String)}; same affordance
     * on the sticker picker when the user taps a starred sticker.
     *
     * @param stickerHash the sticker file hash that uniquely identifies
     *                    the sticker across devices
     * @throws NullPointerException if {@code stickerHash} is {@code null}
     */
    void unfavoriteSticker(String stickerHash);

    /**
     * Removes a sticker from the recent-stickers collection.
     *
     * @apiNote
     * Drives the "Remove from recents" affordance on the sticker
     * picker; the removal is mirrored to every linked device so the
     * sticker disappears from the recents row globally.
     *
     * @param stickerHash the sticker file hash that uniquely identifies
     *                    the sticker across devices
     * @throws NullPointerException if {@code stickerHash} is {@code null}
     */
    void removeRecentSticker(String stickerHash);

    /**
     * Closes the given poll so that no further votes can be cast.
     *
     * @apiNote
     * Drives the "Stop poll" affordance on the poll bubble.
     * Receivers mark the poll as closed and refuse to accept additional
     * {@code vote} stanzas for it.
     *
     * @param pollKey the {@link MessageKey} of the
     *                {@link PollCreationMessage} to close
     * @throws NullPointerException     if {@code pollKey} is {@code null}
     * @throws IllegalArgumentException if {@code pollKey} has no parent JID
     */
    void closePoll(MessageKey pollKey);

    /**
     * Sends a welcome-message request to the given WhatsApp bot and
     * records that the welcome has been triggered.
     *
     * @apiNote
     * Drives the first-time open flow for a bot chat: the welcome message
     * is shown once and the record is mirrored to every linked device so
     * subsequent opens skip the prompt.
     *
     * @param botJid the JID of the bot to send the welcome request to
     * @throws NullPointerException if {@code botJid} is {@code null}
     */
    void sendBotWelcomeRequest(JidProvider botJid);

    /**
     * Renames an AI conversation thread owned by the given bot.
     *
     * @apiNote
     * Drives the "Rename thread" affordance on the Meta AI
     * conversation list; the new title is mirrored to every linked
     * device so the renamed thread shows the same name across them.
     *
     * @param chatJid  the bot JID owning the thread, encoded as a plain
     *                 string JID (e.g. {@code 12345@bot})
     * @param threadId the AI thread identifier
     * @param newName  the new thread title
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code newName} is blank
     */
    void renameAiThread(String chatJid, String threadId, String newName);

    /**
     * Deletes an AI conversation thread owned by the given bot.
     *
     * @apiNote
     * Drives the "Delete thread" affordance on the Meta AI
     * conversation list; the deletion is mirrored to every linked
     * device so the thread disappears across them.
     *
     * @param chatJid  the bot JID owning the thread, encoded as a plain
     *                 string JID
     * @param threadId the AI thread identifier
     * @throws NullPointerException if any argument is {@code null}
     */
    void deleteAiThread(String chatJid, String threadId);

    /**
     * Adds the given chat to the favourites list.
     *
     * @apiNote
     * Drives the "Pin to favourites" affordance on the sidebar;
     * the favourites list is mirrored to every linked device so the
     * same set of chats surfaces under the favourites filter across
     * them.
     *
     * @param chat the JID of the chat to favourite
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void favoriteChat(JidProvider chat);

    /**
     * Removes the given chat from the favourites list.
     *
     * @apiNote
     * Inverse of {@link #favoriteChat(JidProvider)}; same surface on
     * the sidebar.
     *
     * @param chat the JID of the chat to unfavourite
     * @throws NullPointerException if {@code chat} is {@code null}
     */
    void unfavoriteChat(JidProvider chat);

    /**
     * Adds a note to the given chat.
     *
     * @apiNote
     * Drives the "Add note" affordance on the Business chat info
     * panel; the note is private to the local account and mirrored
     * to every linked device. The returned id is the same identifier
     * receivers observe in the synced mutation; pair it with
     * {@link #editNoteOnChat(JidProvider, String, String)} and
     * {@link #deleteNoteFromChat(JidProvider, String)} for the edit
     * and delete paths.
     *
     * @param chat     the chat the note is attached to
     * @param noteText the free-text body of the note
     * @return the generated note id
     * @throws NullPointerException if any argument is {@code null}
     */
    String addNoteToChat(JidProvider chat, String noteText);

    /**
     * Updates the text of an existing note attached to the given chat.
     *
     * @apiNote
     * Drives the "Edit note" affordance on the Business chat info
     * panel.
     *
     * @param chat     the chat the note is attached to
     * @param noteId   the identifier of the note to update
     * @param newText  the new note text
     * @throws NullPointerException if any argument is {@code null}
     */
    void editNoteOnChat(JidProvider chat, String noteId, String newText);

    /**
     * Deletes an existing note from the given chat.
     *
     * @apiNote
     * Drives the "Delete note" affordance on the Business chat info
     * panel; the deletion is mirrored to every linked device.
     *
     * @param chat   the chat the note is attached to
     * @param noteId the identifier of the note to delete
     * @throws NullPointerException if any argument is {@code null}
     */
    void deleteNoteFromChat(JidProvider chat, String noteId);

    /**
     * Pins the referenced message inside its chat for every participant.
     *
     * @apiNote
     * Drives the "Pin message" affordance on the message bubble.
     * The pin is visible to every participant of the chat; use
     * {@link #unpinMessage(MessageKey)} to remove it.
     *
     * @param msgKey the key of the message to pin
     * @throws NullPointerException     if {@code msgKey} is {@code null}
     * @throws IllegalArgumentException if {@code msgKey} has no parent JID
     */
    void pinMessage(MessageKey msgKey);

    /**
     * Removes the pin from the referenced message inside its chat for
     * every participant.
     *
     * @apiNote
     * Inverse of {@link #pinMessage(MessageKey)}; same surface on
     * the message bubble.
     *
     * @param msgKey the key of the message to unpin
     * @throws NullPointerException     if {@code msgKey} is {@code null}
     * @throws IllegalArgumentException if {@code msgKey} has no parent JID
     */
    void unpinMessage(MessageKey msgKey);

    /**
     * Changes the user's preferred UI locale.
     *
     * @apiNote
     * Drives the Settings language picker; the new locale is
     * mirrored to every linked device on the {@code CRITICAL_BLOCK}
     * app-state collection so paired devices switch their UI in
     * lockstep.
     *
     * @param locale the new BCP-47 locale tag (e.g. {@code "en_US"});
     *               never {@code null}
     * @throws NullPointerException            if {@code locale} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editLocale(String locale);

    /**
     * Enables URL link previews (unfurling) for this account.
     *
     * @apiNote
     * Drives the "Disable link previews" toggle on the Settings
     * privacy panel; the new value is mirrored to every linked
     * device so the URL-unfurl behaviour stays consistent across
     * them.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #disableLinkPreviews()
     */
    void enableLinkPreviews();

    /**
     * Disables URL link previews (unfurling) for this account.
     *
     * @apiNote
     * Drives the "Disable link previews" toggle on the Settings
     * privacy panel; the new value is mirrored to every linked
     * device so the URL-unfurl behaviour stays consistent across
     * them.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #enableLinkPreviews()
     */
    void disableLinkPreviews();

    /**
     * Switches the clock display preference to 24-hour time.
     *
     * @apiNote
     * Drives the "Use 24-hour time" toggle in Settings; eagerly
     * writes through to
     * {@link LinkedWhatsAppSettingsStore#setTwentyFourHourFormat(boolean)} so
     * local reads see the new value before the linked-device fanout
     * returns.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #disableTwentyFourHourFormat()
     */
    void enableTwentyFourHourFormat();

    /**
     * Switches the clock display preference to 12-hour time.
     *
     * @apiNote
     * Drives the "Use 24-hour time" toggle in Settings; eagerly
     * writes through to
     * {@link LinkedWhatsAppSettingsStore#setTwentyFourHourFormat(boolean)} so
     * local reads see the new value before the linked-device fanout
     * returns.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #enableTwentyFourHourFormat()
     */
    void disableTwentyFourHourFormat();

    /**
     * Enables the Meta AI features preference.
     *
     * @apiNote
     * Convenience entry point for the "Enable Meta AI" toggle.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #disableAIFeatures()
     */
    void enableAIFeatures();

    /**
     * Disables the Meta AI features preference.
     *
     * @apiNote
     * Convenience entry point for the "Enable Meta AI" toggle.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #enableAIFeatures()
     */
    void disableAIFeatures();

    /**
     * Enables auto-unarchiving a chat when it receives a new message.
     *
     * @apiNote
     * Drives the "Keep chats archived" toggle in Settings; eagerly
     * writes through to
     * {@link LinkedWhatsAppSettingsStore#setUnarchiveChats(boolean)} so subsequent
     * local reads observe the new preference immediately.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #disableUnarchiveChatsOnNewMessage()
     */
    void enableUnarchiveChatsOnNewMessage();

    /**
     * Disables auto-unarchiving a chat when it receives a new message,
     * keeping archived chats archived.
     *
     * @apiNote
     * Drives the "Keep chats archived" toggle in Settings; eagerly
     * writes through to
     * {@link LinkedWhatsAppSettingsStore#setUnarchiveChats(boolean)} so subsequent
     * local reads observe the new preference immediately.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #enableUnarchiveChatsOnNewMessage()
     */
    void disableUnarchiveChatsOnNewMessage();

    /**
     * Updates the per-account notification-activity preference.
     *
     * @apiNote
     * Surfaces the forward-looking notification-activity setting
     * (action index 60, collection {@code REGULAR}); eagerly writes
     * through to
     * {@link LinkedWhatsAppSettingsStore#setNotificationActivitySetting(NotificationActivitySettingAction.NotificationActivitySetting)}
     * so subsequent local reads see the new value.
     *
     * @param setting the non-{@code null}
     *                {@link NotificationActivitySettingAction.NotificationActivitySetting}
     *                to persist
     * @throws NullPointerException            if {@code setting} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editNotificationActivity(NotificationActivitySettingAction.NotificationActivitySetting setting);

    /**
     * Exchanges a CTWA email-recovery verification code for an
     * Ads-Manager session bundle.
     *
     * @apiNote
     * Backs the second step of the click-to-WhatsApp ads recovery
     * flow: the user has typed the code received by email, this call
     * trades the code for the Facebook access token and Ads-Manager
     * session cookies needed to drive the linked Ads surface. The
     * {@code 431 TOO_MANY_ATTEMPTS} and {@code 432 INCORRECT_NONCE}
     * relay arms surface as {@link WhatsAppServerRuntimeException}
     * with their original numeric codes.
     *
     * @param code        the non-{@code null} user-supplied
     *                    verification code
     * @param fromUserJid the optional {@code from} echo carried on the
     *                    outbound IQ; may be {@code null}
     * @return an {@link Optional} carrying the parsed
     *         {@link CtwaAccessTokenSession}, or empty when no
     *         documented variant parsed
     * @throws NullPointerException            if {@code code} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request with one of the
     *                                         documented client/server
     *                                         error variants
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<CtwaAccessTokenSession> queryAccessTokenAndSessionCookies(String code, JidProvider fromUserJid);

    /**
     * Issues an account-binding nonce for the WhatsApp-to-Facebook
     * business linking flow.
     *
     * @apiNote
     * Called by the linking flow that ties the local WhatsApp
     * Business account to a Facebook business identity; the returned
     * nonce is then echoed back to the Meta-side surface as proof of
     * possession.
     *
     * @param identifierScope the optional scope qualifying the issued
     *                        identifier; {@code null} leaves it
     *                        unscoped
     * @return an {@link Optional} carrying the issued nonce, or empty
     *         when the relay did not return a documented
     *         {@code Success} variant
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<String> queryAccountNonce(String identifierScope);

    /**
     * Queries the marketing-messages, Meta-Verified, and GenAI
     * feature-eligibility status of the local business account.
     *
     * @apiNote
     * Drives the SMB feature-discovery surfaces (marketing campaigns,
     * Meta Verified badge, GenAI compose). Each boolean argument
     * selects whether the corresponding projection is requested; the
     * relay only echoes back the feature blocks the client opted into.
     *
     * @param featuresMetaVerified      whether to request the
     *                                  Meta-Verified projection
     * @param featuresMarketingMessages whether to request the
     *                                  marketing-messages projection
     * @param featuresGenai             whether to request the GenAI
     *                                  projection
     * @return an {@link Optional} carrying the parsed
     *         {@link BusinessEligibility}, or empty on no-parse
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<BusinessEligibility> queryBusinessEligibility(boolean featuresMetaVerified, boolean featuresMarketingMessages, boolean featuresGenai);

    /**
     * Lists the Facebook, Instagram, and WhatsApp-ad-identity accounts
     * currently linked to this business account.
     *
     * @apiNote
     * Backs the "Linked accounts" panel in the SMB settings: surfaces
     * the optional Facebook page, Facebook business, Instagram
     * professional, and WhatsApp-ad-identity sub-tuples relevant to
     * cross-surface attribution.
     *
     * @return an {@link Optional} carrying the parsed
     *         {@link BusinessLinkedAccounts}, or empty on no-parse
     * @throws WhatsAppServerRuntimeException  if the relay returned the
     *                                         {@code Forbidden} arm or
     *                                         any other documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<BusinessLinkedAccounts> queryLinkedAccounts();

    /**
     * Refreshes the current SMB-data-sharing-with-Meta consent value
     * from the server.
     *
     * <p>Stores the fetched consent into {@link LinkedWhatsAppBusinessStore#businessPrivacySetting()} and fires
     * {@link LinkedWhatsAppClientListener#onBusinessPrivacySettingChanged}.
     *
     * @apiNote
     * Drives the "Share business data with Meta" toggle position in
     * SMB privacy settings; surfaces the
     * {@link BusinessDataSharingConsent} variant currently recorded
     * for this account.
     *
     * @return an {@link Optional} carrying the parsed
     *         {@link BusinessDataSharingConsent}, or empty on no-parse
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<BusinessDataSharingConsent> refreshBusinessPrivacySetting();

    /**
     * Persists the SMB-data-sharing-with-Meta consent value
     * server-side.
     *
     * @apiNote
     * Drives the SMB "Share business data with Meta" choice change;
     * passing {@code null} clears any stored choice so the relay
     * reverts to the unset state.
     *
     * @param dataSharingConsent the {@link BusinessDataSharingConsent}
     *                           to persist; {@code null} to clear the
     *                           stored value
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         change with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editBusinessPrivacySetting(BusinessDataSharingConsent dataSharingConsent);

    /**
     * Fetches the global Small-Business CTWA data-sharing setting from the relay and stores it.
     *
     * @apiNote
     * This is the account-wide switch consulted by the CTWA conversion-signal pipeline before it
     * emits; it is distinct from {@link #refreshBusinessPrivacySetting()}, which surfaces the same
     * underlying consent as the privacy-settings toggle position.
     *
     * @return the resolved {@link CtwaDataSharingSetting}; {@link CtwaDataSharingSetting#NOT_SET}
     *         when the relay reply did not parse
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    CtwaDataSharingSetting refreshBusinessDataSharingSetting();

    /**
     * Records a per-contact message-feedback preference.
     *
     * @apiNote
     * Backs the per-conversation feedback actions exposed by SMB
     * (block, unblock, report, and similar opaque verbs) by tagging
     * a specific contact with a {@link MessageFeedbackAction} and an
     * optional free-form annotation.
     *
     * @param action   the non-{@code null} feedback action verb
     * @param jid      the non-{@code null} target contact JID
     * @param feedback the optional free-form annotation; may be
     *                 {@code null}
     * @throws NullPointerException            if {@code action} or
     *                                         {@code jid} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         change with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void updateMessageFeedbackPreference(MessageFeedbackAction action, JidProvider jid, String feedback);

    /**
     * Convenience overload of
     * {@link #queryMeteredMessagingCheckout(BusinessMeteredMessagingCheckoutRequest)}
     * for the basic quote-only case.
     *
     * @apiNote
     * Forwards {@code false} for both ad-account and dedupe-skip
     * markers, omits the offer id, and supplies no pending campaigns.
     *
     * @param participants the non-{@code null} prospective message
     *                     recipients
     * @return an {@link Optional} carrying the checkout quote, or
     *         empty when the relay returned no payload
     * @throws NullPointerException if {@code participants} is {@code null}
     */
    Optional<BusinessMeteredMessagingCheckout> queryMeteredMessagingCheckout(List<? extends JidProvider> participants);

    /**
     * Queries the metered-messaging checkout for the SMB
     * marketing-messages feature.
     *
     * @apiNote
     * Drives the marketing-campaign creation surface that needs to
     * surface the per-recipient cost, the remaining campaign quota,
     * and the per-recipient eligibility tuple before letting the user
     * confirm a paid broadcast. The optional markers carried on
     * {@code request} select extra projections returned by the relay.
     *
     * @param request the non-{@code null} checkout request carrying the
     *                recipients (in the 1..2000 range) and the optional
     *                billing, deduplication, offer and pending-campaign
     *                (max 200) knobs
     * @return an {@link Optional} carrying the parsed
     *         {@link BusinessMeteredMessagingCheckout}, or empty on
     *         no-parse
     * @throws NullPointerException            if {@code request} is {@code null}
     * @throws IllegalArgumentException        if the recipients are
     *                                         outside the supported range
     *                                         or the pending-campaign list
     *                                         exceeds 200 entries
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<BusinessMeteredMessagingCheckout> queryMeteredMessagingCheckout(BusinessMeteredMessagingCheckoutRequest request);

    /**
     * Issues a silent CTWA access-token nonce probe.
     *
     * @apiNote
     * Backs the silent-recovery flow that decides whether to bother
     * the user with an email-recovery prompt: the relay either
     * accepts the silent path and returns a
     * {@link CtwaSilentNonceResult.Issued}, or refuses and reports
     * the email address that must confirm account ownership via
     * {@link CtwaSilentNonceResult.RecoveryRequired}. Wire errors are
     * surfaced as typed {@link WhatsAppFacebookGraphQlException} subtypes so the
     * caller can react to client and server faults distinctly.
     *
     * @param fromUserJid the optional {@code from} echo on the
     *                    outbound IQ; may be {@code null}
     * @return an {@link Optional} carrying the parsed
     *         {@link CtwaSilentNonceResult}
     * @throws WhatsAppFacebookGraphQlException.SilentNonceClientError if the relay
     *                                                       rejected the
     *                                                       request with a
     *                                                       {@code 4xx}
     *                                                       error envelope
     * @throws WhatsAppFacebookGraphQlException.SilentNonceServerError if the relay
     *                                                       returned a
     *                                                       transient
     *                                                       {@code 5xx}
     *                                                       failure or the
     *                                                       reply was
     *                                                       unparseable
     * @throws WhatsAppSessionException.Closed               if the socket is
     *                                                       closed
     */
    Optional<CtwaSilentNonceResult> querySilentNonce(JidProvider fromUserJid);

    /**
     * Triggers dispatch of a CTWA account-recovery nonce.
     *
     * @apiNote
     * Used by the CTWA recovery flow when the silent nonce probe
     * came back as {@link CtwaSilentNonceResult.RecoveryRequired};
     * the relay then ships the recovery nonce out-of-band, typically
     * by email, so the user can paste it back into
     * {@link #queryAccessTokenAndSessionCookies(String, JidProvider)}.
     *
     * @param fromUserJid the optional {@code from} echo on the
     *                    outbound IQ; may be {@code null}
     * @return {@code true} when the relay confirms dispatch,
     *         {@code false} on failure or non-parse
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    boolean sendAccountRecoveryNonce(JidProvider fromUserJid);

    /**
     * Uploads CTWA-ad-media descriptors to the native-ad payload
     * service.
     *
     * @apiNote
     * Backs the CTWA ad authoring flow: after the bytes are uploaded
     * to the media servers via the regular upload pipeline, this
     * call binds the resulting (id, type) descriptors to the
     * native-ad payload so the ad creative can reference them.
     *
     * @param media     the optional primary media descriptor; may be
     *                  {@code null}
     * @param mediaList the non-{@code null} additional descriptors
     *                  (0..10)
     * @throws NullPointerException            if {@code mediaList} is {@code null}
     * @throws IllegalArgumentException        if {@code mediaList}
     *                                         exceeds 10 entries
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request with a documented
     *                                         client/server error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void uploadAdMedia(CtwaAdMediaEntry media, List<CtwaAdMediaEntry> mediaList);

    /**
     * Records the user's acceptance of a versioned payments
     * Terms-of-Service document.
     *
     * @apiNote
     * Backs the BR-PIX and UPI consumer payment onboarding screens
     * that present a versioned ToS modal; this method records the
     * acceptance so the relay considers the user enrolled at the
     * specified version. Select the per-market consumer variant by
     * passing the corresponding {@link PaymentsTosV3ConsumerVariant}.
     *
     * @param acceptPayTosVersion the integer ToS version being accepted
     * @param variant             the non-{@code null} consumer-variant
     *                            payload selecting BR/UPI
     * @throws NullPointerException            if {@code variant} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the consent
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editPaymentsTosV3Acceptance(int acceptPayTosVersion, PaymentsTosV3ConsumerVariant variant);

    /**
     * Creates a Brazil-specific custom payment method on the local
     * business account.
     *
     * @apiNote
     * Backs the BR SMB "Add payment method" flow that lets a
     * merchant declare a {@code "pay_on_delivery"} or
     * {@code "pix_key"} channel alongside 1..5 metadata key-value
     * pairs. The supplied {@link BrazilCustomPaymentMethodCreate}
     * carries the device id, method type, optional update marker,
     * optional p2p/p2m flow, and the metadata tuples.
     *
     * @param create the non-{@code null} create request payload
     * @return the freshly created {@link BrazilCustomPaymentMethod}
     * @throws NullPointerException            if {@code create} or any
     *                                         non-nullable nested
     *                                         argument is {@code null}
     * @throws IllegalArgumentException        if the metadata list is
     *                                         empty or has more than 5
     *                                         entries
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         create with an IQ-level
     *                                         error
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    BrazilCustomPaymentMethod createBrazilCustomPaymentMethod(BrazilCustomPaymentMethodCreate create);

    /**
     * Publishes a newsletter post or question response.
     *
     * @apiNote
     * Drives the newsletter compose flow: the helper picks between
     * the brand-new-message wire shape and the question-response wire
     * shape based on whether the
     * {@link NewsletterPublishMessageRequest} carries a target
     * message server id, and waits for the application-level ack.
     *
     * @param newsletterJid the non-{@code null} target newsletter JID
     * @param request       the non-{@code null} publish request
     * @return the non-{@code null} relay {@link NewsletterPublishAck}
     * @throws NullPointerException            if either argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         negative ack or the
     *                                         envelope did not parse
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    NewsletterPublishAck publishNewsletterMessage(JidProvider newsletterJid, NewsletterPublishMessageRequest request);

    /**
     * Publishes a newsletter status post.
     *
     * @apiNote
     * Sister of
     * {@link #publishNewsletterMessage(JidProvider, NewsletterPublishMessageRequest)}
     * scoped to the status namespace; picks between brand-new-status
     * and status-reaction-on-existing-status wire shapes based on
     * whether the {@link NewsletterPublishStatusRequest} carries a
     * target status server id.
     *
     * @param newsletterJid the non-{@code null} target newsletter JID
     * @param request       the non-{@code null} publish request
     * @return the non-{@code null} relay {@link NewsletterPublishAck}
     * @throws NullPointerException            if either argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         negative ack or the
     *                                         envelope did not parse
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    NewsletterPublishAck publishNewsletterStatus(JidProvider newsletterJid, NewsletterPublishStatusRequest request);

    /**
     * Dispatches a typed legacy-IQ request and returns the raw
     * reply.
     *
     * @apiNote
     * Escape hatch for callers that hand-craft an
     * {@link IqStanza.Request} and want the raw {@link Stanza}
     * back instead of a model projection; the higher-level helpers
     * delegate through this when no typed model is wired up yet.
     *
     * @param request the non-{@code null} typed legacy-IQ request
     * @return the non-{@code null} raw inbound reply stanza
     * @throws NullPointerException            if {@code request} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    Stanza sendNode(IqStanza.Request request);

    /**
     * Convenience overload of
     * {@link #queryBusinessCatalogProducts(JidProvider, List, int, int, String)}
     * for ordinary (non-direct-connection) merchants.
     *
     * @apiNote
     * Forwards a {@code null} direct-connection encrypted-info blob,
     * which is the regular-merchant case; use the full overload when
     * fetching catalog entries for a direct-connection merchant.
     *
     * @param catalogJid the merchant's catalog JID
     * @param productIds the product ids to fetch
     * @param width      the requested image width
     * @param height     the requested image height
     * @return the products
     * @throws NullPointerException if {@code catalogJid} is {@code null}
     */
    List<BusinessProduct> queryBusinessCatalogProducts(JidProvider catalogJid, List<String> productIds, int width, int height);

    /**
     * Convenience overload of
     * {@link #queryBusinessCatalogProducts(JidProvider, List, int, int)} that requests WhatsApp's
     * default catalog image dimensions.
     *
     * @apiNote
     * Forwards WhatsApp's default 100x100-pixel catalog image
     * dimensions, matching the storefront's default product-thumbnail
     * request when no specific size is needed.
     *
     * @param catalogJid the merchant's catalog JID; never {@code null}
     * @param productIds the product ids to fetch; never {@code null}
     * @return the products
     * @throws NullPointerException if {@code catalogJid} or {@code productIds} is {@code null}
     */
    List<BusinessProduct> queryBusinessCatalogProducts(JidProvider catalogJid, List<String> productIds);

    /**
     * Fetches the WhatsApp Business catalog product list for a
     * merchant by id.
     *
     * @apiNote
     * Drives the storefront product-detail surface that needs a
     * specific set of catalog entries (typically the ones referenced
     * by a chat-attached order or by a cart). The returned list is
     * in the relay's reply order, normally matching
     * {@code productIds}, so callers can zip the two lists; entries
     * the relay could not resolve surface as {@link BusinessProduct}
     * instances with {@link BusinessProduct#invalid()} set to
     * {@code true}.
     *
     * @param catalogJid                    the non-{@code null} merchant
     *                                      catalog JID
     * @param productIds                    the non-{@code null} and
     *                                      non-empty list of catalog
     *                                      product ids to fetch
     * @param width                         the desired image width in
     *                                      pixels
     * @param height                        the desired image height in
     *                                      pixels
     * @param directConnectionEncryptedInfo the optional direct-connection
     *                                      blob carried for
     *                                      direct-connection merchants;
     *                                      may be {@code null}
     * @return the non-{@code null} parsed product list, possibly empty
     * @throws NullPointerException            if {@code catalogJid} or
     *                                         {@code productIds} is {@code null}
     * @throws IllegalArgumentException        if {@code productIds} is empty
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         client- or server-error variant
     */
    List<BusinessProduct> queryBusinessCatalogProducts(JidProvider catalogJid, List<String> productIds, int width, int height, String directConnectionEncryptedInfo);

    /**
     * Attaches an uploaded cover photo to the authenticated user's
     * Business profile.
     *
     * @apiNote
     * Backs the SMB "Change cover photo" surface; the caller is
     * expected to have already uploaded the JPEG via the media
     * upload pipeline, so this method only ships the resulting
     * {@code (id, ts, token)} triple that wires the upload to the
     * profile.
     *
     * @param id    the upload id returned by the media upload pipeline
     * @param ts    the non-{@code null} upload timestamp
     * @param token the non-{@code null} opaque upload token issued by
     *              the media server
     * @throws NullPointerException            if {@code ts} or
     *                                         {@code token} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    void editBusinessCoverPhoto(long id, Instant ts, byte[] token);

    /**
     * Clears one or more server-tracked dirty-bit entries.
     *
     * @apiNote
     * Called after the client has fully ingested the resources the
     * server marked dirty (typical examples: {@code account_sync},
     * {@code groups}, {@code blocklist}). Without the acknowledgement
     * the server re-announces the same dirty resources on every
     * reconnect.
     *
     * @param dirtyBits the non-{@code null} and non-empty
     *                  {@code (type, timestamp)} entries to clear
     * @throws NullPointerException            if {@code dirtyBits} is {@code null}
     * @throws IllegalArgumentException        if {@code dirtyBits} is empty
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    void clearDirtyBits(Map<String, Long> dirtyBits);

    /**
     * Transcodes the given source bytes to the wire format expected for
     * {@code provider}'s slot, uploads the encoded payload to WhatsApp's
     * CDN, and populates both the codec-derived and CDN metadata fields
     * on {@code provider}.
     *
     * @apiNote
     * The bytes are first piped through an internal transcoder pipeline
     * keyed by {@link MediaProvider#mediaPath()}: image sources are
     * re-encoded as MJPEG with a derived micro-thumbnail, videos as
     * H.264 / AAC MP4 with a faststart moov, audio as 48 kHz stereo AAC
     * (M4A) or, for push-to-talk
     * {@link com.github.auties00.cobalt.wire.linked.message.media.AudioMessage
     * audio messages}, as 16 kHz mono Opus (OGG) plus the wire-format
     * waveform, stickers as a 512x512 WebP, documents pass through with
     * a PDF page-zero thumbnail and page count when applicable, and slots
     * without a dedicated pipeline (application-state and history-sync
     * blobs, profile pictures, newsletter media) pass through verbatim.
     * The transcoder applies its derived fields (width, height, duration,
     * mimetype, jpegThumbnail, waveform, pageCount, mediaSize) directly
     * to {@code provider}; the CDN upload then writes {@code mediaUrl},
     * {@code mediaDirectPath}, {@code mediaKey},
     * {@code mediaKeyTimestamp}, {@code mediaSha256}, and
     * {@code mediaEncryptedSha256} so the same {@link MediaProvider} can
     * immediately be embedded in an outgoing {@link LinkedMessageContainer}
     * and dispatched through {@link #sendMessage(JidProvider, LinkedMessageContainer)}.
     * Uses the cached media connection held by the store and blocks
     * until the first {@code media_conn} refresh has landed.
     *
     * @param provider    the media provider to populate; codec-derived
     *                    and CDN metadata fields are written in place
     * @param inputStream the raw plaintext bytes to transcode and upload
     * @return {@code true} if the upload succeeded; {@code false} if
     *         the {@link MediaProvider#mediaPath()} carries no CDN
     *         path and the operation was skipped
     * @throws NullPointerException              if any argument is
     *                                           {@code null}
     * @throws WhatsAppMediaException.Processing if the transcoder
     *                                           pipeline fails to
     *                                           decode, encode, or
     *                                           buffer the source
     * @throws WhatsAppMediaException.Upload     if every candidate
     *                                           CDN host failed, the
     *                                           response was
     *                                           malformed, or an I/O
     *                                           error occurred
     *                                           during the POST
     * @throws WhatsAppMediaException.Connection if the calling thread
     *                                           is interrupted while
     *                                           waiting for the
     *                                           cached media
     *                                           connection
     * @throws WhatsAppSessionException.Closed   if the socket is no
     *                                           longer open
     */
    boolean uploadMedia(MediaProvider provider, InputStream inputStream);

    /**
     * Transcodes the source file and uploads the result to WhatsApp's
     * CDN, mutating the provider with the resulting metadata.
     *
     * @apiNote
     * Preferred entry point when the caller already has a file on disk:
     * skips the source-side temp-file spill that the
     * {@link #uploadMedia(MediaProvider, InputStream)} adapter performs
     * for non-file-backed streams. The file is not modified or deleted
     * by this method.
     *
     * @param provider the media provider to populate
     * @param source   the raw plaintext file
     * @return {@code true} if the upload succeeded; {@code false} if
     *         the {@link MediaProvider#mediaPath()} carries no CDN path
     * @throws NullPointerException              if any argument is
     *                                           {@code null}
     * @throws WhatsAppMediaException.Processing if the transcoder
     *                                           pipeline fails to
     *                                           decode, encode, or
     *                                           buffer the source
     * @throws WhatsAppMediaException.Upload     if every candidate CDN
     *                                           host failed, the
     *                                           response was malformed,
     *                                           or an I/O error
     *                                           occurred during the
     *                                           POST
     * @throws WhatsAppMediaException.Connection if the calling thread
     *                                           is interrupted while
     *                                           waiting for the cached
     *                                           media connection
     * @throws WhatsAppSessionException.Closed   if the socket is no
     *                                           longer open
     */
    boolean uploadMedia(MediaProvider provider, Path source);

    /**
     * Downloads the encrypted media described by the given provider
     * from WhatsApp's CDN and returns a stream over the decrypted
     * bytes.
     *
     * @apiNote
     * Retrieves the actual content of a piece of media received in a
     * chat: the photo, video, voice message, document, sticker, or
     * status the user wants to view, save, or forward. WhatsApp keeps
     * this content scrambled on its servers, so this fetches it and
     * hands back the original, ready-to-use bytes. Read the stream to
     * obtain the media, and close it once done.
     *
     * @param provider the received media to download
     * @return a decrypting {@link InputStream} positioned at the
     *         start of the plaintext payload, never {@code null}
     * @throws NullPointerException              if {@code provider}
     *                                           is {@code null}
     * @throws WhatsAppMediaException.Download   if every candidate
     *                                           CDN host failed, the
     *                                           encrypted-hash check
     *                                           failed, or an I/O
     *                                           error occurred
     *                                           during the GET
     * @throws WhatsAppMediaException.Connection if the calling thread
     *                                           is interrupted while
     *                                           waiting for the
     *                                           cached media
     *                                           connection
     * @throws WhatsAppSessionException.Closed   if the socket is no
     *                                           longer open
     */
    InputStream downloadMedia(MediaProvider provider);

    /**
     * Records dismissal of a single Terms-of-Service notice.
     *
     * @apiNote
     * Backs the "Got it" tap on legal-update prompts that do not
     * require explicit acceptance; tells the relay it can stop
     * re-publishing the notice. Use
     * {@link #refreshTosNotices(Collection)} first to learn which
     * notice ids are currently outstanding.
     *
     * @param noticeId the non-{@code null} notice id to delete
     * @throws NullPointerException            if {@code noticeId} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    void deleteTosNotice(String noticeId);

    /**
     * Records acceptance of one or more Terms-of-Service notices.
     *
     * @apiNote
     * Marks the user as having accepted each requested legal-update
     * or disclosure prompt, unlocking any feature gated on it. Use
     * {@link #refreshTosNotices(Collection)} first to enumerate the
     * outstanding notice ids and to verify the acceptance later.
     *
     * @param noticeIds the non-{@code null} notice ids being
     *                  accepted; may be empty
     * @throws NullPointerException            if {@code noticeIds} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    void acknowledgeTosNotices(List<String> noticeIds);

    /**
     * Fetches the relay's digest of the local Signal pre-key bundle.
     *
     * @apiNote
     * Drives the periodic key-resync check: the local client
     * recomputes the same digest from its own bundle and compares,
     * triggering a fresh upload via
     * {@link #uploadSignalPreKeys(SignalPreKeyBundle)} when the two
     * diverge. The returned {@link IdentityKeyDigest} carries the
     * registration id, key-bundle type marker, identity public key,
     * signed pre-key, one-time pre-key identifiers, and the SHA-1
     * digest over the concatenated material.
     *
     * @return an {@link Optional} carrying the parsed
     *         {@link IdentityKeyDigest}, or empty when the relay had
     *         no record on file
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    Optional<IdentityKeyDigest> queryKeyDigest();

    /**
     * Fetches the long-term identity public keys for one or more
     * device JIDs.
     *
     * @apiNote
     * Used when the local client is about to start a Signal session
     * with a peer device and needs that device's identity public key
     * to verify the fingerprint. Devices the relay could not resolve
     * are omitted from the result; each returned {@link IdentityKey}
     * carries the device it belongs to.
     *
     * @param deviceJids the non-{@code null} and non-empty list of
     *                   devices whose identity keys to fetch
     * @return the resolved {@link IdentityKey} entries, one per device
     *         the relay returned a key for
     * @throws NullPointerException            if {@code deviceJids} is {@code null}
     * @throws IllegalArgumentException        if {@code deviceJids} is empty
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    List<IdentityKey> queryIdentityKeys(List<? extends JidProvider> deviceJids);

    /**
     * Rotates the local signed pre-key on the relay.
     *
     * @apiNote
     * Called periodically (typically every few days) so that
     * subsequent inbound Signal sessions derive their shared secret
     * from a fresh signed pre-key; this limits the window over
     * which a previously leaked key can be used to start new
     * sessions with the local user.
     *
     * @param signedPreKey the non-{@code null} freshly-minted
     *                     {@link SignalSignedPreKey}
     * @throws NullPointerException            if {@code signedPreKey} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    void rotateSignedPreKey(SignalSignedPreKey signedPreKey);

    /**
     * Uploads a fresh batch of one-time pre-keys to the relay.
     *
     * @apiNote
     * Replenishes the relay-side one-time pre-key pool that drains
     * as remote devices fetch keys to start new Signal sessions
     * with the local user. Call when
     * {@link #queryKeyDigest()} indicates the pool is below the
     * client's refill watermark.
     *
     * @param bundle the non-{@code null} {@link SignalPreKeyBundle} to upload
     * @throws NullPointerException            if {@code bundle} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    void uploadSignalPreKeys(SignalPreKeyBundle bundle);

    /**
     * Uploads the registration-time pre-key bundle to the relay.
     *
     * @apiNote
     * Runs once during device registration handshake; distinct from
     * {@link #uploadSignalPreKeys(SignalPreKeyBundle)} which runs
     * after registration to replenish the one-time pre-key pool.
     * Same payload shape, separate wire IQ pair.
     *
     * @param bundle the non-{@code null} {@link SignalPreKeyBundle} to upload
     * @throws NullPointerException            if {@code bundle} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    void uploadRegistrationPreKeys(SignalPreKeyBundle bundle);

    /**
     * Performs the blind-sign round-trip that issues a private-stats
     * anonymous credential.
     *
     * @apiNote
     * Private-stats is WhatsApp's anonymous-attribution telemetry
     * channel: the caller passes a blinded elliptic-curve point
     * (32 bytes) and a project-name tag (UTF-8 bytes) scoping the
     * credential to a particular collector. The relay co-signs the
     * blinded point with a project-specific key and returns the
     * signed point plus the ACS public key, DLEQ proof coordinates,
     * and mint timestamp. The caller then unblinds the signed point
     * to obtain the redeemable token.
     *
     * @param blindedCredential the non-{@code null} blinded
     *                          elliptic-curve point bytes
     * @param projectName       the non-{@code null} UTF-8 project-name
     *                          bytes
     * @return an {@link Optional} carrying the parsed
     *         {@link PrivateStatsToken}, or empty on no-parse
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  if the relay returned a
     *                                         documented client- or
     *                                         server-error variant
     */
    Optional<PrivateStatsToken> issuePrivateStatsToken(byte[] blindedCredential, byte[] projectName);

    /**
     * Acknowledges full ingestion of a group's metadata and history
     * snapshot.
     *
     * @apiNote
     * Called after the local store has consumed the dirty notification
     * carrying the group's pending updates. Without the acknowledgement
     * the server redelivers the same notification on every reconnect.
     *
     * @param group the non-{@code null} group JID being acknowledged
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    void acknowledgeGroup(JidProvider group);

    /**
     * Accepts a pending group-add invite.
     *
     * @apiNote
     * Used when the relay invited the caller to a group whose
     * membership-approval mode is {@code request_required}: the
     * accept either commits the caller as a participant immediately
     * or enqueues a request for an admin to review. The returned
     * boolean reports which branch occurred.
     *
     * @param accept the non-{@code null} {@link GroupAddAccept}
     *               carrying the target group JID, invite code,
     *               expiration timestamp, and inviting admin JID
     * @return {@code true} when the relay routed the accept into the
     *         pending-approval queue, {@code false} when the caller
     *         was admitted immediately
     * @throws NullPointerException            if {@code accept} or any
     *                                         required nested field is
     *                                         {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    boolean acceptGroupAdd(GroupAddAccept accept);

    /**
     * Queries metadata for many groups in one round trip.
     *
     * @apiNote
     * Used by sync paths that need to refresh metadata for several
     * groups at once, for example after a long disconnect when many
     * per-group dirty bits have accumulated. The relay returns one
     * entry per requested JID across four possible shapes (full
     * {@code group_info}, truncated {@code group_info}, the
     * {@code group_forbidden} marker, or the {@code group_not_exist}
     * marker); the two markers are silently dropped from the
     * returned list because they carry no metadata.
     *
     * @param groups the non-{@code null} group JIDs being queried
     * @return an unmodifiable list of {@link GroupMetadata} entries in
     *         the relay's reply order, possibly empty
     * @throws NullPointerException            if {@code groups} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     * @see #queryGroupMetadata(JidProvider...)
     */
    List<GroupMetadata> queryGroupMetadata(Collection<? extends JidProvider> groups);

    /**
     * Queries metadata for the given groups in one round trip.
     *
     * @apiNote
     * Varargs convenience for {@link #queryGroupMetadata(Collection)}.
     *
     * @param groups the non-{@code null} group JIDs being queried
     * @return an unmodifiable list of {@link GroupMetadata} entries in
     *         the relay's reply order, possibly empty
     * @throws NullPointerException            if {@code groups} is {@code null} or contains {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     * @see #queryGroupMetadata(Collection)
     */
    List<GroupMetadata> queryGroupMetadata(JidProvider... groups);

    /**
     * Cancels one or more pending self-issued membership-approval
     * requests.
     *
     * @apiNote
     * Used when the local user, having previously asked to join a
     * closed group or community sub-group, withdraws the request
     * before an admin acts on it. The returned map's values surface
     * the per-applicant outcome:
     * {@link GroupParticipantStatus#OK} for successful cancellations,
     * {@link GroupParticipantStatus#NOT_AUTHORIZED} when the caller
     * does not own the targeted request and lacks admin rights, and
     * {@link GroupParticipantStatus#NOT_WHATSAPP_USER} when the relay
     * could not find a matching pending request.
     *
     * @param group      the non-{@code null} target group JID
     * @param applicants the non-{@code null} participant JIDs whose
     *                   pending requests are being cancelled
     * @return an unmodifiable map keyed by applicant JID in the
     *         relay's reply order
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Map<Jid, GroupParticipantStatus> cancelGroupMembershipRequests(JidProvider group, Collection<? extends JidProvider> applicants);

    /**
     * Suggests a brand-new sub-group be created under a parent
     * community.
     *
     * @apiNote
     * Backs the community-admin tooling: the admin describes a
     * sub-group that does not yet exist, the relay reserves a JID,
     * and the returned {@link SubgroupSuggestionResult} carries the
     * provisional metadata (jid, creator, creation timestamp,
     * optional creator phone number, and an optional
     * description-error string when the relay could not accept the
     * supplied description verbatim).
     *
     * @param suggestion the non-{@code null} {@link SubgroupSuggestionNew}
     *                   carrying the parent community JID, subject,
     *                   optional description, and the locked/announcement/
     *                   hiddenGroup toggles
     * @return the suggestion result, always carrying
     *         {@link SubgroupSuggestionResult.Kind#NEW_GROUP}
     * @throws NullPointerException            if {@code suggestion} or
     *                                         any non-nullable nested
     *                                         field is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    SubgroupSuggestionResult suggestNewSubgroup(SubgroupSuggestionNew suggestion);

    /**
     * Suggests pre-existing groups be linked into a parent community
     * as sub-groups.
     *
     * @apiNote
     * Sister of {@link #suggestNewSubgroup(SubgroupSuggestionNew)} for
     * the case where the admin wants to recommend already-existing
     * groups be folded into the community rather than spinning up a
     * fresh one. The relay validates each candidate against the
     * community's policies; rejected candidates carry a
     * {@link SubgroupSuggestionResult.Candidate.Reason} discriminator
     * pinpointing why.
     *
     * @param community       the non-{@code null} parent community JID
     * @param candidateGroups the non-{@code null} and non-empty list
     *                        of candidate sub-group JIDs
     * @return the suggestion result, always carrying
     *         {@link SubgroupSuggestionResult.Kind#EXISTING_GROUPS}
     * @throws NullPointerException            if any argument is {@code null}
     * @throws IllegalArgumentException        if {@code candidateGroups} is empty
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    SubgroupSuggestionResult suggestExistingSubgroups(JidProvider community, Collection<? extends JidProvider> candidateGroups);

    /**
     * Deactivates a community parent group.
     *
     * @apiNote
     * Equivalent in effect to deactivating the entire community:
     * every linked sub-group is unlinked and converted back into a
     * standalone group. The returned {@link Optional} carries the
     * post-deletion stored {@link GroupMetadata} when the community
     * was known locally; otherwise empty.
     *
     * @param community the non-{@code null} parent community JID
     * @return an {@link Optional} carrying the post-deletion
     *         {@link GroupMetadata}, or empty when the community is
     *         not in the local store
     * @throws NullPointerException            if {@code community} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Optional<GroupMetadata> deleteParentGroup(JidProvider community);

    /**
     * Queries the profile pictures for many groups in one batch.
     *
     * @apiNote
     * Backs the community-roster and chat-list flows that need to
     * surface fresh profile pictures for several groups at once.
     * The {@link GroupProfilePicture} model unifies the three
     * possible relay shapes: a URL projection populates
     * {@link GroupProfilePicture#url()} and
     * {@link GroupProfilePicture#directPath()}, an inline blob
     * populates {@link GroupProfilePicture#blob()}, and an
     * unchanged/absent marker leaves all of them unset.
     *
     * @param groups the non-{@code null} group JIDs being queried
     * @return an unmodifiable list of {@link GroupProfilePicture}
     *         entries in the relay's reply order
     * @throws NullPointerException            if {@code groups} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    List<GroupProfilePicture> queryGroupProfilePictures(Collection<? extends JidProvider> groups);

    /**
     * Resolves a group's metadata snapshot from a deep-link invite
     * code without joining it.
     *
     * @apiNote
     * Backs the "Preview group before joining" UI flow: the relay
     * validates the supplied invite code and projects subject,
     * picture, owner, admins, ephemeral state, etc. The reply also
     * carries the relay-reported {@code size} attribute, folded into
     * {@link GroupMetadata#size()}.
     *
     * @param inviteCode the non-{@code null} invite code
     * @return an {@link Optional} carrying the parsed
     *         {@link GroupMetadata}, or empty when the relay could
     *         not produce a {@code <group>} subtree
     * @throws NullPointerException            if {@code inviteCode} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Optional<GroupMetadata> queryInviteGroupInfo(String inviteCode);

    /**
     * Resolves a single linked-group entry within a parent community.
     *
     * @apiNote
     * Backs the "Go to parent community" and "Go to general
     * sub-group" navigation actions in the chat UI. The
     * {@code queryLinkedType} discriminator selects which projection
     * the relay returns: {@link LinkedGroupType#PARENT_GROUP} for the
     * parent community itself, or {@link LinkedGroupType#SUB_GROUP}
     * for the specific sub-group identified by {@code queryLinkedJid}.
     *
     * @param community       the non-{@code null} parent community JID
     * @param queryLinkedType the non-{@code null} linkage-direction
     *                        discriminator
     * @param queryLinkedJid  the specific linked-group JID; required
     *                        when {@code queryLinkedType} is
     *                        {@link LinkedGroupType#SUB_GROUP}
     * @return an {@link Optional} carrying the parsed metadata, either
     *         {@link GroupMetadata} for a sub-group projection or
     *         {@link CommunityMetadata} for the parent-community
     *         projection, or empty when the relay returned no
     *         resolvable subtree
     * @throws NullPointerException            if any required argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Optional<ChatMetadata> queryLinkedGroup(JidProvider community, LinkedGroupType queryLinkedType, JidProvider queryLinkedJid);

    /**
     * Returns the union of participants across every group linked to a
     * parent community.
     *
     * @apiNote
     * Backs the "@all" mention flow inside community sub-groups,
     * where the mentioning UI must display the union of every
     * sub-group participant. The returned map keys are the primary
     * JIDs; the values are the resolved phone-number JIDs when the
     * relay supplied LID-to-PN resolution, or {@code null} when it
     * did not.
     *
     * @param community the non-{@code null} parent community JID
     * @return an unmodifiable map keyed by the participant's primary
     *         JID in the relay's reply order
     * @throws NullPointerException            if {@code community} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Map<Jid, Jid> queryLinkedGroupsParticipants(JidProvider community);

    /**
     * Returns the pending membership-approval requests awaiting an
     * admin's review.
     *
     * @apiNote
     * Backs the "Pending requests" tab in the group-info UI. Each
     * {@link GroupMembershipApprovalRequest} entry captures the
     * requesting user's identity, the optional resolved phone-number
     * JID and username, the parent-community JID for community-link
     * join requests, the request timestamp, and the
     * {@link GroupMembershipApprovalRequest.Method} indicating the
     * pathway through which the request was filed.
     *
     * @param group the non-{@code null} target group JID
     * @return an unmodifiable list of pending requests in the relay's
     *         reply order
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    List<GroupMembershipApprovalRequest> queryGroupMembershipApprovalRequests(JidProvider group);

    /**
     * Returns every group the caller currently participates in.
     *
     * @apiNote
     * Used by initial-sync paths that bootstrap the local chat list
     * when the persistent store is empty. The two boolean flags
     * select which optional projections the relay attaches to the
     * full or truncated {@code group_info} entries it returns.
     *
     * @param includeParticipants whether to include per-group
     *                            participant edges
     * @param includeDescription  whether to include per-group
     *                            description text
     * @return an unmodifiable list of {@link GroupMetadata} entries in
     *         the relay's reply order, one per participating group
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    List<GroupMetadata> queryParticipatingGroups(boolean includeParticipants, boolean includeDescription);

    /**
     * Returns the in-group messages members previously reported to
     * the group's administrators.
     *
     * @apiNote
     * Backs the "View previously reported messages" admin view. The
     * relay deduplicates reports by offending stanza id, so a single
     * message reported by several members surfaces as one
     * {@link GroupMessageReport} carrying multiple
     * {@link GroupMessageReport.Reporter} rows.
     *
     * @param group the non-{@code null} target group JID
     * @return an unmodifiable list of {@link GroupMessageReport}
     *         entries in the relay's reply order
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    List<GroupMessageReport> queryReportedMessages(JidProvider group);

    /**
     * Joins a community sub-group.
     *
     * @apiNote
     * Backs the "Join sub-group" flow inside community navigation;
     * the relay decides whether to add the caller directly or
     * enqueue a request for an admin based on the sub-group's
     * membership-approval mode. The returned boolean reports which
     * branch occurred.
     *
     * @param community           the non-{@code null} parent community JID
     * @param subgroup            the non-{@code null} sub-group JID
     *                            being joined
     * @param linkedGroupType     the non-{@code null} sub-group-kind
     *                            discriminator carried in the request
     *                            ({@link LinkedGroupType#SUB_GROUP} for
     *                            an ordinary sub-group,
     *                            {@link LinkedGroupType#DEFAULT_SUB_GROUP}
     *                            for a linked announcement group)
     * @return {@code true} when the relay routed the join into the
     *         pending-approval queue, {@code false} when the caller
     *         was admitted immediately
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    boolean joinLinkedGroup(JidProvider community, JidProvider subgroup, LinkedGroupType linkedGroupType);

    /**
     * Links existing groups under a community parent.
     *
     * @apiNote
     * Backs the community-admin "Add existing group" flow that
     * promotes a standalone group into a community sub-group.
     * Linking implicitly transfers each sub-group's members into the
     * parent community; members whose privacy settings forbid the
     * implicit add are reported back through
     * {@link LinkedSubgroupResult#participantErrors()} keyed by JID
     * with the wire-level {@code "403"} code.
     *
     * @param community the non-{@code null} parent community JID
     * @param subgroups the non-{@code null} sub-group JIDs being linked
     * @return an unmodifiable list of {@link LinkedSubgroupResult}
     *         entries in the relay's reply order
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    List<LinkedSubgroupResult> linkSubgroups(JidProvider community, List<? extends JidProvider> subgroups);

    /**
     * Reports an in-group message to WhatsApp moderation.
     *
     * @apiNote
     * Backs the "Report message" UI action inside groups; the report
     * is forwarded to the upstream moderation pipeline. Success is
     * silent; only failures observably surface.
     *
     * @param group     the non-{@code null} target group JID
     * @param messageId the non-{@code null} server id of the
     *                  offending message
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    void reportGroupMessages(JidProvider group, String messageId);

    /**
     * Revokes the per-participant invite codes used by closed groups.
     *
     * @apiNote
     * Backs the admin "Reset invite link" action when running on a
     * group whose invite mode is the per-participant
     * {@code request_required} variant. The returned map's values
     * surface the per-participant outcome:
     * {@link GroupParticipantStatus#OK} for successful revocations,
     * {@link GroupParticipantStatus#NOT_WHATSAPP_USER} when the
     * relay could not find a matching request code.
     *
     * @param group        the non-{@code null} target group JID
     * @param participants the non-{@code null} per-participant JIDs
     *                     whose request codes are being revoked
     * @return an unmodifiable map keyed by the participant JID in the
     *         relay's reply order
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Map<Jid, GroupParticipantStatus> revokeGroupRequestCode(JidProvider group, List<? extends JidProvider> participants);

    /**
     * Applies a batch of edits to a group's metadata.
     *
     * @apiNote
     * Single entry point for subject and description rewrites,
     * profile-picture updates and removals, binary property toggles
     * (locked, announcement, ephemeral, membership-approval, and so
     * on), and the local-only {@code statusMuted} sync flag. Every
     * optional field on {@link GroupMetadataEdit} that carries a
     * present value drives one mutation:
     * <ul>
     *   <li>{@link GroupMetadataEdit#subject() subject} present
     *       rewrites the group title.</li>
     *   <li>{@link GroupMetadataEdit#description() description}
     *       present rewrites or clears the description; a
     *       {@link GroupDescription.Set Set} sets the body, a
     *       {@link GroupDescription.Clear Clear} removes it.</li>
     *   <li>{@link GroupMetadataEdit#picture() picture} present
     *       updates or removes the group icon;
     *       {@link GroupPicture.Set Set} uploads the picture bytes,
     *       {@link GroupPicture.Clear Clear} removes it.</li>
     *   <li>Each batched toggle (the
     *       {@link GroupMetadataEdit#editInfoPolicy() editInfoPolicy},
     *       {@link GroupMetadataEdit#sendMessagePolicy() sendMessagePolicy},
     *       and siblings) is batched into a single property-update
     *       request.</li>
     *   <li>Each
     *       {@link GroupMetadataEdit#limitSharing() limitSharing},
     *       {@link GroupMetadataEdit#memberAddPolicy() memberAddPolicy},
     *       {@link GroupMetadataEdit#memberLinkPolicy() memberLinkPolicy},
     *       {@link GroupMetadataEdit#memberShareGroupHistoryPolicy() memberShareGroupHistoryPolicy},
     *       and
     *       {@link GroupMetadataEdit#subGroupCreationPolicy() subGroupCreationPolicy}
     *       setting is applied through its corresponding property
     *       mutation.</li>
     *   <li>{@link GroupMetadataEdit#ephemeralTimer() ephemeralTimer}
     *       present is routed through
     *       {@link #editEphemeralTimer(JidProvider, ChatEphemeralTimer)},
     *       which applies the disappearing-message timer change and
     *       the in-memory chat-ephemerality state update.</li>
     *   <li>{@link GroupMetadataEdit#statusMuted() statusMuted}
     *       present merges the value into the stored
     *       {@link GroupMetadata#statusMuted() statusMuted} field
     *       without producing a network packet.</li>
     * </ul>
     *
     * @param edit the non-{@code null} edit packet
     * @return an {@link Optional} carrying the post-edit
     *         {@link GroupMetadata}, or empty when the group is not in
     *         the store
     * @throws NullPointerException            if {@code edit} is {@code null}
     * @throws IllegalArgumentException        if the JID is not a
     *                                         group/community
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    Optional<GroupMetadata> editGroupMetadata(GroupMetadataEdit edit);

    /**
     * Detaches sub-groups from their parent community.
     *
     * @apiNote
     * Inverse of
     * {@link #linkSubgroups(JidProvider, List)}; detached sub-groups
     * become standalone groups again. Each
     * {@link UnlinkedSubgroupResult} carries the sub-group's JID,
     * whether the relay echoed the
     * {@code remove_orphaned_members="true"} attribute, and an
     * optional {@link UnlinkedSubgroupResult.Reason} discriminator
     * pinpointing why the candidate failed to detach.
     *
     * @param community the non-{@code null} parent community JID
     * @param subgroups the non-{@code null} sub-group JIDs being detached
     * @return an unmodifiable list of {@link UnlinkedSubgroupResult}
     *         entries in the relay's reply order
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned
     *                                         a documented
     *                                         {@code ClientError} or
     *                                         {@code ServerError}
     */
    List<UnlinkedSubgroupResult> unlinkSubgroups(JidProvider community, List<? extends JidProvider> subgroups);

    /**
     * Convenience overload of
     * {@link #queryNewsletterMessageUpdates(JidProvider, int, Instant, NewsletterHistoryDirection)}
     * for a cold fetch.
     *
     * @apiNote
     * Forwards a {@code null} {@code since} watermark so the relay
     * starts from the newest cursor regardless of any local delta
     * state.
     *
     * @param newsletter the newsletter
     * @param count      the per-call cap
     * @param direction  the cursor direction (Before/After pivot)
     * @return the message-history page
     * @throws NullPointerException if any argument is {@code null}
     */
    NewsletterMessageHistory queryNewsletterMessageUpdates(JidProvider newsletter, int count, NewsletterHistoryDirection direction);

    /**
     * Returns the windowed message-update deltas applied to a
     * newsletter since a reference timestamp.
     *
     * @apiNote
     * Used by the newsletter sync path that backfills the local
     * message store after a reconnect; the stream carries only
     * per-message deltas (edits, reactions, deletes) rather than
     * full message bodies.
     *
     * @param newsletter the non-{@code null} newsletter JID being queried
     * @param count      the per-call cap; must be non-negative
     * @param since      the reference {@link Instant} delta-cursor (a unix-second floor); may be {@code null}
     * @param direction  the optional pagination cursor; may be {@code null}
     * @return the non-{@code null} message-update slice
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    NewsletterMessageHistory queryNewsletterMessageUpdates(JidProvider newsletter, int count, Instant since, NewsletterHistoryDirection direction);

    /**
     * Returns a windowed page of newsletter message envelopes,
     * addressing the newsletter by its JID.
     *
     * @apiNote
     * Convenience for
     * {@link #queryNewsletterMessages(JidProvider, int, NewsletterViewerRole, NewsletterHistoryDirection)}
     * that omits the optional view-role and pagination cursor.
     *
     * @param newsletter the non-{@code null} newsletter JID
     * @param count      the per-call cap; must be non-negative
     * @return the non-{@code null} message-history page
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    NewsletterMessageHistory queryNewsletterMessages(JidProvider newsletter, int count);

    /**
     * Returns a windowed page of newsletter message envelopes,
     * addressing the newsletter by its JID, with optional view-role
     * projection and pagination cursor.
     *
     * @apiNote
     * Drives the newsletter detail view's message-history pagination.
     * The {@code direction} argument controls which side of the
     * anchor is fetched; the {@code viewRole} echoes the role-scoped
     * ACL projection requested by the UI.
     *
     * @param newsletter the non-{@code null} newsletter JID
     * @param count      the per-call cap; must be non-negative
     * @param viewRole   optional ACL projection role echoed in the
     *                   {@code <view_role>} attribute; may be {@code null}
     * @param direction  the pagination cursor; may be {@code null}
     * @return the non-{@code null} message-history page
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    NewsletterMessageHistory queryNewsletterMessages(JidProvider newsletter, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction);

    /**
     * Returns a windowed page of newsletter message envelopes,
     * addressing the newsletter by its public invite key.
     *
     * @apiNote
     * Convenience for
     * {@link #queryNewsletterMessages(String, int, NewsletterViewerRole, NewsletterHistoryDirection)}
     * that omits the optional view-role and pagination cursor.
     *
     * @param inviteKey the non-{@code null} newsletter invite key
     * @param count     the per-call cap; must be non-negative
     * @return the non-{@code null} message-history page
     * @throws NullPointerException            if {@code inviteKey} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    NewsletterMessageHistory queryNewsletterMessages(String inviteKey, int count);

    /**
     * Returns a windowed page of newsletter message envelopes,
     * addressing the newsletter by its public invite key, with
     * optional view-role projection and pagination cursor.
     *
     * @apiNote
     * Companion to
     * {@link #queryNewsletterMessages(JidProvider, int, NewsletterViewerRole, NewsletterHistoryDirection)}
     * for clients holding only the public invite link instead of the
     * JID (for example an external referrer).
     *
     * @param inviteKey the non-{@code null} newsletter invite key
     * @param count     the per-call cap; must be non-negative
     * @param viewRole  optional ACL projection role; may be {@code null}
     * @param direction the pagination cursor; may be {@code null}
     * @return the non-{@code null} message-history page
     * @throws NullPointerException            if {@code inviteKey} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    NewsletterMessageHistory queryNewsletterMessages(String inviteKey, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction);

    /**
     * Convenience overload of
     * {@link #queryNewsletterResponses(JidProvider, long, int, String, NewsletterResponsesFilter, String)}
     * for the basic page-from-top case.
     *
     * @apiNote
     * Forwards {@code null} for the before-cursor, filter, and
     * search-text so the relay returns the newest responder page.
     *
     * @param newsletter                the non-{@code null} newsletter JID
     * @param questionResponsesServerId the server id of the question/poll message
     * @param questionResponsesCount    the per-call cap
     * @return the responder entries
     * @throws NullPointerException if {@code newsletter} is {@code null}
     */
    List<NewsletterQuestionResponse> queryNewsletterResponses(JidProvider newsletter, long questionResponsesServerId, int questionResponsesCount);

    /**
     * Fetches the responder slice for an interactive newsletter
     * question post.
     *
     * @apiNote
     * Backs the newsletter-author UI that visualises the
     * per-subscriber response stream attached to a question post.
     * Each {@link NewsletterQuestionResponse} carries the responder
     * identity, response timestamp, and a flag marking responses the
     * question owner has already replied to.
     *
     * @param newsletter                the non-{@code null} newsletter JID
     * @param questionResponsesServerId the server id of the question/poll message
     * @param questionResponsesCount    the per-call cap; must be non-negative
     * @param questionResponsesBefore   optional anchor for backwards
     *                                  pagination; may be {@code null}
     * @param filter                    optional filter discriminator; may be {@code null}
     * @param searchText                optional search string; may be {@code null}
     * @return the non-{@code null} responder entries in server order
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    List<NewsletterQuestionResponse> queryNewsletterResponses(JidProvider newsletter, long questionResponsesServerId, int questionResponsesCount, String questionResponsesBefore, NewsletterResponsesFilter filter, String searchText);

    /**
     * Convenience overload of
     * {@link #queryNewsletterStatusUpdates(JidProvider, int, Instant, NewsletterHistoryDirection)}
     * for a cold fetch.
     *
     * @apiNote
     * Forwards a {@code null} {@code since} watermark so the relay
     * starts from the newest cursor regardless of any local delta
     * state.
     *
     * @param newsletter the newsletter
     * @param count      the per-call cap
     * @param direction  the cursor direction (Before/After pivot)
     * @return the status-update slice
     * @throws NullPointerException if any argument is {@code null}
     */
    NewsletterStatusHistory queryNewsletterStatusUpdates(JidProvider newsletter, int count, NewsletterHistoryDirection direction);

    /**
     * Returns the windowed status-update deltas applied to a
     * newsletter since a reference timestamp.
     *
     * @apiNote
     * Status-scoped sister of
     * {@link #queryNewsletterMessageUpdates(JidProvider, int, Instant, NewsletterHistoryDirection)};
     * drives the newsletter sync path that backfills the local
     * status store after a reconnect.
     *
     * @param newsletter the non-{@code null} newsletter JID
     * @param count      the per-call cap; must be non-negative
     * @param since      the reference {@link Instant} delta-cursor (a unix-second floor); may be {@code null}
     * @param direction  the optional pagination cursor; may be {@code null}
     * @return the non-{@code null} status-update slice
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    NewsletterStatusHistory queryNewsletterStatusUpdates(JidProvider newsletter, int count, Instant since, NewsletterHistoryDirection direction);

    /**
     * Fetches a windowed page of newsletter status envelopes,
     * addressing the newsletter by its JID.
     *
     * @apiNote
     * Convenience for
     * {@link #queryNewsletterStatuses(JidProvider, int, NewsletterViewerRole, NewsletterHistoryDirection)}
     * that omits the optional view-role and pagination cursor.
     *
     * @param newsletter the non-{@code null} newsletter JID
     * @param count      the per-call cap; must be non-negative
     * @return the non-{@code null} status-history page
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    NewsletterStatusHistory queryNewsletterStatuses(JidProvider newsletter, int count);

    /**
     * Fetches a windowed page of newsletter status envelopes,
     * addressing the newsletter by its JID, with optional view-role
     * projection and pagination cursor.
     *
     * @apiNote
     * Status-scoped sister of
     * {@link #queryNewsletterMessages(JidProvider, int, NewsletterViewerRole, NewsletterHistoryDirection)};
     * drives the status-history pagination in the newsletter detail
     * view.
     *
     * @param newsletter the non-{@code null} newsletter JID
     * @param count      the per-call cap; must be non-negative
     * @param viewRole   optional ACL projection role; may be {@code null}
     * @param direction  the pagination cursor; may be {@code null}
     * @return the non-{@code null} status-history page
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    NewsletterStatusHistory queryNewsletterStatuses(JidProvider newsletter, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction);

    /**
     * Fetches a windowed page of newsletter status envelopes,
     * addressing the newsletter by its public invite key.
     *
     * @apiNote
     * Convenience for
     * {@link #queryNewsletterStatuses(String, int, NewsletterViewerRole, NewsletterHistoryDirection)}
     * that omits the optional view-role and pagination cursor.
     *
     * @param inviteKey the non-{@code null} newsletter invite key
     * @param count     the per-call cap; must be non-negative
     * @return the non-{@code null} status-history page
     * @throws NullPointerException            if {@code inviteKey} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     */
    NewsletterStatusHistory queryNewsletterStatuses(String inviteKey, int count);

    /**
     * Fetches a windowed page of newsletter status envelopes,
     * addressing the newsletter by its public invite key, with
     * optional view-role projection and pagination cursor.
     *
     * @apiNote
     * Companion to
     * {@link #queryNewsletterStatuses(JidProvider, int, NewsletterViewerRole, NewsletterHistoryDirection)}
     * for clients holding only the public invite link instead of the
     * JID.
     *
     * @param inviteKey the non-{@code null} newsletter invite key
     * @param count     the per-call cap; must be non-negative
     * @param viewRole  optional ACL projection role; may be {@code null}
     * @param direction the pagination cursor; may be {@code null}
     * @return the non-{@code null} status-history page
     * @throws NullPointerException            if {@code inviteKey} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    NewsletterStatusHistory queryNewsletterStatuses(String inviteKey, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction);

    /**
     * Returns the caller's own reactions and poll votes on messages
     * of a specific newsletter.
     *
     * @apiNote
     * Used by the newsletter detail view to populate the "My recent
     * reactions" panel with the caller's most-used reactions for the
     * channel and to highlight options the caller already voted for
     * in newsletter polls.
     *
     * @param limit      the per-call cap; must be non-negative
     * @param newsletter the non-{@code null} newsletter JID
     * @return a non-{@code null} map keyed by newsletter JID whose
     *         values are the per-message add-on entries
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    Map<Jid, List<NewsletterMyAddOn>> queryNewsletterMyAddOns(int limit, JidProvider newsletter);

    /**
     * Subscribes the connection to real-time updates for a specific
     * newsletter.
     *
     * @apiNote
     * Once acknowledged the server starts pushing the newsletter's live
     * message and status delta stream to this connection. The
     * subscription is bounded by the returned duration (clamped by the
     * server to {@code [30s, 600s]}); the caller is expected to refresh
     * before it expires to keep the stream alive.
     *
     * @param newsletter the non-{@code null} newsletter JID being
     *                   subscribed to
     * @return the non-{@code null} relay-chosen subscription duration
     * @throws NullPointerException            if {@code newsletter} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is no longer open
     * @throws WhatsAppServerRuntimeException  when the relay returned a
     *                                         documented
     *                                         {@code ClientError},
     *                                         {@code ServerError}, or
     *                                         the envelope did not parse
     */
    Duration subscribeToNewsletterLiveUpdates(JidProvider newsletter);

    /**
     * Fetches the latest A/B-experiment configuration bundle.
     *
     * @apiNote
     * Refreshes the global props cache; the relay either echoes back
     * the materialised props subtree (when the supplied hash is
     * stale or absent) or short-circuits to a delta when the cached
     * snapshot is already current.
     *
     * @param propsHash      the client's currently-cached props hash;
     *                       may be {@code null} on the first fetch
     * @param propsRefreshId the client's currently-cached refresh id;
     *                       may be {@code null} on the first fetch
     * @return an {@link Optional} carrying the parsed
     *         {@link AbPropsBundle}, or empty on no-parse
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<AbPropsBundle> queryExperimentConfig(String propsHash, Integer propsRefreshId);

    /**
     * Fetches the per-group A/B-experiment configuration.
     *
     * @apiNote
     * Refreshes group-scoped feature gates after the server signals an
     * A/B-props change for a specific group.
     *
     * @param group     the non-{@code null} target group JID
     * @param propsHash the cached group-props hash, or {@code null}
     * @return an {@link Optional} carrying the parsed
     *         {@link AbPropsBundle}, or empty on no-parse
     * @throws NullPointerException            if {@code group} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<AbPropsBundle> queryGroupExperimentConfig(JidProvider group, String propsHash);

    /**
     * Fetches the bot directory listing.
     *
     * @apiNote
     * Refreshes the curated set of WhatsApp AI bots the user can
     * interact with. The request is digest-gated: an unchanged
     * directory echoes the cached {@code bhash} back without any
     * payload.
     *
     * @param botV     the supported protocol revision (typically
     *                 {@code "2"} or {@code "3"}); may be {@code null}
     * @param botBhash the cached directory digest; may be {@code null}
     * @param botArgs  the non-{@code null} optional bot JIDs to scope
     *                 the query to
     * @return an {@link Optional} carrying the parsed
     *         {@link BotDirectory}, or empty on no-parse
     * @throws NullPointerException            if {@code botArgs} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<BotDirectory> queryBotList(String botV, String botBhash, List<? extends JidProvider> botArgs);

    /**
     * Submits a bug report to the WhatsApp support backend.
     *
     * @apiNote
     * Backs the in-app "Contact us" and "Report a problem" flows;
     * the supplied {@link BugReport} carries the reporter JID,
     * description, debug-info JSON, optional device-log handle,
     * optional pre-uploaded attachments, and the optional title,
     * category, ASL join key, and reproducibility tag.
     *
     * @param report the non-{@code null} bug report payload
     * @return an {@link Optional} carrying the backend-side task id,
     *         or empty when the relay omitted it
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<String> reportBug(BugReport report);

    /**
     * Reports an in-app marketing event to the relay.
     *
     * @apiNote
     * Drives attribution of in-app banner and promotion impressions,
     * clicks, and dismissals. The supplied {@link InAppCommsEvent}
     * carries the promotion id, event type code, event timestamp,
     * and an opaque JSON {@code logdata} payload whose schema is
     * owned by the promotion authoring tool.
     *
     * @param event the non-{@code null} in-app comms event
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void reportInAppCommsEvent(InAppCommsEvent event);

    /**
     * Acknowledges the boundary of an offline-batch backfill.
     *
     * @apiNote
     * Called after the client has fully ingested every stanza in
     * the post-reconnect offline backfill so the relay advances its
     * offline cursor. Without the ack the same batch is redelivered
     * on the next reconnect.
     *
     * @param offlineBatchCount the number of stanzas the client has
     *                          just fully consumed; must match the
     *                          size the relay announced
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void acknowledgeOfflineBatch(int offlineBatchCount);

    /**
     * Switches this client's session into active mode.
     *
     * @apiNote
     * Tells the relay this device is the user's primary foregrounded
     * surface and should receive immediate push of new stanzas,
     * presence updates, typing indicators, and the like. A
     * multi-device session typically has one device in active mode
     * at a time; backgrounded companions stay in passive mode (see
     * {@link #enablePassiveMode()}) so the relay can suppress
     * non-essential pushes.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void enableActiveMode();

    /**
     * Switches this client's session into passive mode.
     *
     * @apiNote
     * Tells the relay this device is backgrounded so non-essential
     * pushes (typing indicators, presence updates) can be suspended
     * until the next call to {@link #enableActiveMode()}. See
     * {@link #enableActiveMode()} for the active/passive model.
     *
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void enablePassiveMode();

    /**
     * Fetches Signal pre-key bundles for the supplied users.
     *
     * @apiNote
     * Required before the local client can send messages to a peer
     * it has never communicated with; the returned bundle carries
     * the recipient's identity key, signed pre-key, and one-time
     * pre-key needed to seed a Signal session.
     *
     * @param users the non-{@code null} per-user requests
     * @return an {@link Optional} carrying the parsed
     *         {@link PreKeyBundleResult}, or empty on no-parse
     * @throws NullPointerException            if {@code users} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<PreKeyBundleResult> queryPreKeyBundles(List<PreKeyBundleRequest> users);

    /**
     * Fetches additional one-time pre-keys for users whose locally
     * cached bundles have been exhausted.
     *
     * @apiNote
     * Called when an outbound message would otherwise reuse a stale
     * pre-key; the relay returns just the missing one-time keys
     * without re-sending the long-lived identity material.
     *
     * @param users the non-{@code null} per-user requests
     * @return an {@link Optional} carrying the parsed
     *         {@link PreKeyBundleResult}, or empty on no-parse
     * @throws NullPointerException            if {@code users} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<PreKeyBundleResult> queryMissingPreKeys(List<MissingPreKeyUserRequest> users);

    /**
     * Co-signs a blinded credential token via the anonymous-
     * attribution signing service.
     *
     * @apiNote
     * Signing half of the private-stats handshake: the client
     * blinds a token, the relay co-signs it without learning the
     * contents, and the client later unblinds the result to obtain
     * an unlinkable credential it can spend to attest to a stats
     * event without revealing its identity. The
     * {@code projectNameElementValue} selects which signer key the
     * relay uses.
     *
     * @param blindedCredentialElementValue the non-{@code null}
     *                                      blinded credential token
     * @param projectNameElementValue       the non-{@code null} signing
     *                                      project identifier
     * @return an {@link Optional} carrying the
     *         {@link SignedAttributionCredential}, or empty when the
     *         relay returned no parseable variant
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<SignedAttributionCredential> signAnonymousAttributionCredential(byte[] blindedCredentialElementValue, String projectNameElementValue);

    /**
     * Queries whether the user has blocked Public Service
     * Announcement chats.
     *
     * @apiNote
     * PSAs are official chat blasts the WhatsApp team itself
     * publishes (launch announcements, safety notices, feature
     * tutorials). This method surfaces the current opt-out state so
     * the UI can render the correct toggle position.
     *
     * @return {@code true} when the relay reports the PSA channel
     *         currently blocked, {@code false} when active
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    boolean queryPublicAnnouncementBlocked();

    /**
     * Updates whether the user is blocking Public Service
     * Announcement chats.
     *
     * @apiNote
     * Drives the SMB and consumer PSA-block toggle. See
     * {@link #queryPublicAnnouncementBlocked()} for a definition of
     * PSAs.
     *
     * @param blockingAction the non-{@code null} mutation verb to apply
     *                       to the PSA block list
     * @throws NullPointerException            if {@code blockingAction} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editPublicAnnouncementBlocked(PsaChatBlockAction blockingAction);

    /**
     * Updates the device's push-notification configuration on the
     * relay.
     *
     * @apiNote
     * Called at session bootstrap or whenever the OS-issued push
     * token rotates; submits a fresh platform-specific
     * {@link PushConfig} (FCM, APNs, Web push, WNS, Enterprise,
     * Facebook) or a {@link PushConfig.Clear} that removes the
     * registration so the relay stops sending wakeup pings.
     *
     * @param config the non-{@code null} push-config payload
     * @throws NullPointerException            if {@code config} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void editPushConfig(PushConfig config);

    /**
     * Sends a free-form support feedback report.
     *
     * @apiNote
     * Backs the in-app "Send feedback" surface; supplies the
     * reporter JID, the quoted message id, and a list of
     * feedback-kind tags so the support team can triage.
     *
     * @param from          the non-{@code null} reporter JID
     * @param messageId     the non-{@code null} quoted message id
     * @param feedbackKinds the non-{@code null} feedback-kind tags
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void sendSupportFeedback(JidProvider from, String messageId, List<String> feedbackKinds);

    /**
     * Submits a structured "Contact us" form to support.
     *
     * @apiNote
     * Backs the help-center contact flow; the supplied
     * {@link SupportContactForm} carries the reporter JID,
     * description, topic, optional topic id, debug-info JSON,
     * optional pre-uploaded log handle, and the additional
     * context-flow string used by the support tooling for routing.
     *
     * @param form the non-{@code null} contact form
     * @return an {@link Optional} carrying the
     *         {@link SupportTicketAcknowledgement}, or empty on
     *         no-parse
     * @throws NullPointerException            if {@code form} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the
     *                                         request (retryable or
     *                                         non-retryable)
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<SupportTicketAcknowledgement> sendSupportContactForm(SupportContactForm form);

    /**
     * Reports an individual contact for spam.
     *
     * @apiNote
     * Backs the in-app "Block and report" flow; forwards the report
     * to WhatsApp's trust-and-safety pipeline alongside the
     * caller-curated list of offending stanza ids so the moderator
     * has the conversation context. The supplied
     * {@link IndividualSpamReport} carries the reportee JID, the
     * spam-flow code, an optional {@code is_known_chat} marker, and
     * the evidence stanza ids.
     *
     * @param report the non-{@code null} individual spam report
     * @throws NullPointerException            if {@code report} or any
     *                                         non-nullable nested
     *                                         field is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void reportIndividualForSpam(IndividualSpamReport report);

    /**
     * Reports a group chat for spam.
     *
     * @apiNote
     * Group-scoped sister of
     * {@link #reportIndividualForSpam(IndividualSpamReport)}. The
     * supplied {@link GroupSpamReport} additionally carries an
     * optional {@code adder} JID identifying the user who originally
     * added the reporter to the group, useful for surfacing
     * add-spam patterns.
     *
     * @param report the non-{@code null} group spam report
     * @throws NullPointerException            if {@code report} or any
     *                                         non-nullable nested
     *                                         field is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void reportGroupForSpam(GroupSpamReport report);

    /**
     * Reports a newsletter for spam.
     *
     * @apiNote
     * Backs newsletter abuse complaints; the supplied
     * {@link NewsletterSpamReport} carries the newsletter JID,
     * spam-flow code, optional subject, and a curated set of
     * reported message receipts so the trust-and-safety pipeline can
     * attribute the complaint.
     *
     * @param report the non-{@code null} newsletter spam report
     * @throws NullPointerException            if {@code report} or any
     *                                         non-nullable nested
     *                                         field is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void reportNewsletterForSpam(NewsletterSpamReport report);

    /**
     * Reports a status post for trust-and-safety review.
     *
     * @apiNote
     * Accepts both regular chat statuses broadcast on
     * {@code status@broadcast} and newsletter statuses; the right
     * RPC is dispatched based on the runtime type of
     * {@code status}. Callers supply only the user-typed fields
     * ({@code reason} code and optional {@code subject}); the wire
     * fields (target JID, message id or server id, timestamp) are
     * derived from the supplied {@link LinkedMessageInfo}.
     *
     * @param status  the non-{@code null} offending status post
     * @param reason  the non-{@code null} spam-flow code identifying
     *                the report flow
     * @param subject optional free-text comment; may be {@code null}
     * @throws NullPointerException            if {@code status} or
     *                                         {@code reason} is {@code null}
     * @throws IllegalArgumentException        if {@code status} carries
     *                                         no timestamp, sender, or
     *                                         message identifier
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void reportStatus(LinkedMessageInfo status, String reason, String subject);

    /**
     * Registers this device's user-journey analytics session id with
     * the server.
     *
     * @apiNote
     * Most embedders do not need to call this. It is exposed for
     * clients that mirror the user-journey telemetry surface
     * (Channels, Forward-message, CTWA-ad-creation, signup-funnel).
     * Typical usage:
     * {@snippet :
     *     var id = String.valueOf((Instant.now().toEpochMilli() + Duration.ofDays(3).toMillis())
     *         % Duration.ofDays(7).toMillis());
     *     client.joinUnifiedSession(id);
     * }
     *
     * @param unifiedSessionId the non-{@code null} session id to
     *                         announce; the canonical derivation is a
     *                         rolling 7-day clock
     *                         ({@code (now + 3d) mod 7d}), but any
     *                         non-{@code null} string is accepted
     * @throws NullPointerException            if {@code unifiedSessionId} is {@code null}
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    void joinUnifiedSession(String unifiedSessionId);

    /**
     * Fetches the Terms-of-Service and privacy-policy notices the
     * user has not yet acknowledged.
     *
     * @apiNote
     * User-notice disclosures are the modal pop-ups WhatsApp shows
     * when Terms of Service, privacy policy, or a regional
     * compliance notice changes; each entry carries the disclosure
     * id the client passes back to record the dismissal once the
     * user closes the modal.
     *
     * @param getUserDisclosuresT the non-{@code null} request timestamp
     * @return an {@link Optional} carrying the parsed
     *         {@link UserNoticeBundle}, or empty on no-parse
     * @throws NullPointerException            if {@code getUserDisclosuresT} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<UserNoticeBundle> queryPendingUserNotices(Instant getUserDisclosuresT);

    /**
     * Fetches the per-stage acknowledgement state for an explicit
     * list of user-notice disclosure ids.
     *
     * @apiNote
     * Used after a refresh to verify that the local cache matches
     * the server's view of which Terms-of-Service and privacy
     * notices the user has acknowledged or dismissed. See
     * {@link #queryPendingUserNotices(Instant)} for what
     * disclosures are.
     *
     * @param queries the non-{@code null} per-disclosure stage queries
     * @return an unmodifiable list of {@link UserNoticeStage} entries,
     *         empty when the relay returned no parseable variant
     * @throws NullPointerException            if {@code queries} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    List<UserNoticeStage> queryUserNoticeStages(List<UserNoticeStageQuery> queries);

    /**
     * Mints a fresh call link that can be shared to invite
     * participants into a multi-party call.
     *
     * @apiNote
     * The relay returns a 22-character opaque token embedded into
     * the canonical {@code https://call.whatsapp.com/{voice|video}/{token}}
     * URL exposed by {@link CallLink#url()}. The token's media kind
     * is fixed at creation time and cannot be changed afterwards.
     * The supplied {@link CallLinkCreate} carries the media kind
     * plus four optional fields:
     * <ul>
     *   <li>creator device JID, pinning the link to a known device.</li>
     *   <li>in-flight call id, bundling the link with an existing call.</li>
     *   <li>creator display username, surfaced in the join-prompt UI.</li>
     *   <li>scheduled-call start instant when the link backs an event.</li>
     * </ul>
     *
     * @param create the non-{@code null} call-link create payload
     * @return the freshly minted {@link CallLink}
     * @throws NullPointerException            if {@code create} or its
     *                                         media kind is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    CallLink createCallLink(CallLinkCreate create);

    /**
     * Resolves the metadata behind an existing call link.
     *
     * @apiNote
     * Backs the "Join via link" preview step: the relay resolves
     * the supplied token and replies with the link's creator
     * device, optional creator phone-number JID, optional creator
     * username, and current waiting-room state. The reply also
     * indicates whether the link is bound to a scheduled-call event.
     * The {@code action} discriminator gates the kind of resolve:
     * {@code "preview"} surfaces metadata to a prospective joiner,
     * {@code "edit"} refreshes metadata for the creator.
     *
     * @param token  the non-{@code null} link token
     * @param media  the non-{@code null} expected media kind; must
     *               match the link's configured media or the relay
     *               rejects the query
     * @param action the non-{@code null} action discriminator
     *               (typically {@code "preview"} or {@code "edit"})
     * @return an {@link Optional} carrying the resolved {@link CallLink},
     *         or empty when the reply could not be parsed
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<CallLink> queryCallLink(String token, CallLinkMedia media, String action);

    /**
     * Enables the waiting-room gate on an existing call link.
     *
     * @apiNote
     * Only mutates the link's waiting-room state; in-flight call
     * sessions are unaffected. Subsequent
     * {@link #queryCallLink(String, CallLinkMedia, String)} replies
     * surface the new state via {@link CallLink#waitingRoom()}.
     *
     * @param link the {@code https://call.whatsapp.com/{voice|video}/<token>} call-link URL; never {@code null}
     * @throws NullPointerException            if {@code link} is {@code null}
     * @throws IllegalArgumentException        if {@code link} is not a well-formed
     *                                         {@code call.whatsapp.com/{voice|video}/<token>} call-link URL
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #disableCallLinkWaitingRoom(URI)
     */
    void enableCallLinkWaitingRoom(URI link);

    /**
     * Disables the waiting-room gate on an existing call link.
     *
     * @apiNote
     * Only mutates the link's waiting-room state; in-flight call
     * sessions are unaffected. Subsequent
     * {@link #queryCallLink(String, CallLinkMedia, String)} replies
     * surface the new state via {@link CallLink#waitingRoom()}.
     *
     * @param link the {@code https://call.whatsapp.com/{voice|video}/<token>} call-link URL; never {@code null}
     * @throws NullPointerException            if {@code link} is {@code null}
     * @throws IllegalArgumentException        if {@code link} is not a well-formed
     *                                         {@code call.whatsapp.com/{voice|video}/<token>} call-link URL
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     * @see #enableCallLinkWaitingRoom(URI)
     */
    void disableCallLinkWaitingRoom(URI link);

    /**
     * Checks whether the user already has an active federated-identity
     * link to a Meta account.
     *
     * @apiNote
     * First step of the Meta-SSO linking flow: a non-zero
     * {@link FederatedIdentityState} lets the client skip directly
     * to certificate fetch via
     * {@link #queryFederatedIdentityCertificate(Instant, boolean, boolean)};
     * a suspension marker tells the client to show the appropriate
     * recovery surface; absent state means the linking handshake
     * must run from scratch.
     *
     * @param timestamp the non-{@code null} request timestamp
     * @return an {@link Optional} carrying the relay-reported
     *         {@link FederatedIdentityState}, or empty when the
     *         response could not be parsed
     * @throws NullPointerException            if {@code timestamp} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<FederatedIdentityState> checkFederatedIdentityExists(Instant timestamp);

    /**
     * Pings the federated-identity bridge to keep an in-flight
     * enrolment alive.
     *
     * @apiNote
     * Called on the cadence chosen by the previous ping reply; the
     * returned {@link FederatedIdentityPing} surfaces the next
     * recommended interval. The {@link FederatedRsaEncryption} type
     * stays typed because its four-blob payload is mandatory and
     * meaningful as a unit.
     *
     * @param encryption the non-{@code null} RSA-2048 encryption envelope
     * @param timestamp  the non-{@code null} request timestamp
     * @param fbid       the non-{@code null} encrypted Facebook id payload
     * @return an {@link Optional} carrying the
     *         {@link FederatedIdentityPing} result, or empty when the
     *         response could not be parsed
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<FederatedIdentityPing> sendFederatedIdentityPing(FederatedRsaEncryption encryption, Instant timestamp, byte[] fbid);

    /**
     * Fetches the federated-identity certificate bundle.
     *
     * @apiNote
     * Returns up to three PEM bundles the client uses to encrypt
     * payloads to the Meta-side bridge and to verify bridge-signed
     * responses. The {@code hasPayloadEncCertificates} flag selects
     * whether the encryption and signature PEMs are returned;
     * {@code hasPasswordPem} adds the password PEM bundle.
     *
     * @param timestamp                 the non-{@code null} request timestamp
     * @param hasPayloadEncCertificates {@code true} to request the
     *                                  payload-encryption certs
     *                                  (encryption + signature PEMs)
     * @param hasPasswordPem            {@code true} to request the
     *                                  password PEM bundle
     * @return an {@link Optional} carrying the
     *         {@link FederatedIdentityCertificate}, or empty when the
     *         response could not be parsed
     * @throws NullPointerException            if {@code timestamp} is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<FederatedIdentityCertificate> queryFederatedIdentityCertificate(Instant timestamp, boolean hasPayloadEncCertificates, boolean hasPasswordPem);

    /**
     * Refreshes the access tokens held inside the federated-identity
     * bridge.
     *
     * @apiNote
     * Called when the local copies of the bridge-held access tokens
     * are about to expire; the relay rotates them and returns the
     * new set wrapped inside a fresh
     * {@link FederatedRsaEncryption} envelope.
     *
     * @param encryption the non-{@code null} RSA-2048 encryption envelope
     * @param timestamp  the non-{@code null} request timestamp
     * @param fbid       the non-{@code null} encrypted Facebook id payload
     * @return an {@link Optional} carrying the
     *         {@link FederatedAccessTokenRefresh} reply, or empty when
     *         the response could not be parsed
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<FederatedAccessTokenRefresh> refreshFederatedIdentityAccessTokens(FederatedRsaEncryption encryption, Instant timestamp, byte[] fbid);

    /**
     * Submits an arbitrary encrypted action payload to the
     * federated-identity bridge.
     *
     * @apiNote
     * Generic catch-all RPC for actions the bridge can perform on
     * the linked Meta account once the client holds a valid Waffle
     * session. The reply may carry a {@code wf_deleted} marker
     * surfaced via {@link FederatedEncryptedAction#deleted()};
     * when set, the client must purge its local link state because
     * the bridge has dropped the federated-identity link entirely.
     *
     * @param encryption the non-{@code null} RSA-2048 encryption envelope
     * @param timestamp  the non-{@code null} request timestamp
     * @param fbid       the non-{@code null} encrypted Facebook id payload
     * @param action     the non-{@code null} encrypted action payload
     * @return an {@link Optional} carrying the
     *         {@link FederatedEncryptedAction} reply, or empty when
     *         the response could not be parsed
     * @throws NullPointerException            if any argument is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    Optional<FederatedEncryptedAction> sendFederatedIdentityEncryptedPayload(FederatedRsaEncryption encryption, Instant timestamp, byte[] fbid, byte[] action);

    /**
     * Mints a WhatsApp Enterprise Authenticated Customer record.
     *
     * @apiNote
     * Final step of the federated-identity to enterprise enrolment
     * flow: once the user has accepted the disclosure in their
     * preferred locale, the Meta bridge mints the WAEntAC record so
     * subsequent business surfaces (catalog, hosted business
     * account, and so on) can address the linked enterprise account.
     * The supplied {@link EnterpriseAuthenticatedCustomerCreate}
     * bundles the RSA encryption envelope, request timestamp,
     * accepted disclosure id and version, and the disclosure
     * language and locale tags.
     *
     * @param create the non-{@code null} create request payload
     * @return the minted {@link FederatedEnterpriseCustomer}
     * @throws NullPointerException            if {@code create} or any
     *                                         non-nullable nested
     *                                         field is {@code null}
     * @throws WhatsAppServerRuntimeException  if the relay rejected the request
     * @throws WhatsAppSessionException.Closed if the socket is closed
     */
    FederatedEnterpriseCustomer createEnterpriseAuthenticatedCustomer(EnterpriseAuthenticatedCustomerCreate create);

    /**
     * Returns the synced raw string value of the given AB prop.
     *
     * @apiNote
     * Blocks until the first AB-props sync round completes; once a
     * value is cached subsequent calls return immediately. Falls
     * back to {@link ABProp#defaultValue()} when the server has not
     * provided one.
     *
     * @param prop the non-{@code null} {@link ABProp} definition
     * @return the synced string value, or the default when the server
     *         has not provided one
     * @throws NullPointerException if {@code prop} is {@code null}
     */
    String queryAbPropString(ABProp prop);

    /**
     * Returns the synced boolean value of the given AB prop.
     *
     * @apiNote
     * Boolean projection of {@link #queryAbPropString(ABProp)};
     * blocks until the first AB-props sync round completes and
     * falls back to {@link ABProp#defaultValue()} when the server
     * has not provided one.
     *
     * @param prop the non-{@code null} {@link ABProp} definition
     * @return the parsed boolean, or the default when the server has
     *         not provided one
     * @throws NullPointerException if {@code prop} is {@code null}
     */
    boolean queryAbPropBool(ABProp prop);

    /**
     * Returns the synced integer value of the given AB prop.
     *
     * @apiNote
     * Integer projection of {@link #queryAbPropString(ABProp)};
     * blocks until the first AB-props sync round completes.
     *
     * @param prop the non-{@code null} {@link ABProp} definition
     * @return the parsed integer, the default, or {@code 0}
     * @throws NullPointerException if {@code prop} is {@code null}
     */
    int queryAbPropInt(ABProp prop);

    /**
     * Returns the synced long value of the given AB prop.
     *
     * @apiNote
     * Long projection of {@link #queryAbPropString(ABProp)};
     * blocks until the first AB-props sync round completes.
     *
     * @param prop the non-{@code null} {@link ABProp} definition
     * @return the parsed long, the default, or {@code 0L}
     * @throws NullPointerException if {@code prop} is {@code null}
     */
    long queryAbPropLong(ABProp prop);

    /**
     * Returns the synced double value of the given AB prop.
     *
     * @apiNote
     * Double projection of {@link #queryAbPropString(ABProp)};
     * blocks until the first AB-props sync round completes.
     *
     * @param prop the non-{@code null} {@link ABProp} definition
     * @return the parsed double, the default, or {@code 0.0}
     * @throws NullPointerException if {@code prop} is {@code null}
     */
    double queryAbPropDouble(ABProp prop);

    /**
     * Enrols the account in the WhatsApp Web and Desktop beta program.
     *
     * @apiNote
     * The Web and Desktop clients share the same JS bundle, so this
     * single toggle controls the beta channel for both surfaces.
     * Enrolling emits a singleton {@code external_web_beta} mutation in
     * the {@code REGULAR} sync collection so every linked Web/Desktop
     * installation converges on the same value.
     *
     * @see #disableWebBetaEnrollment()
     */
    void enableWebBetaEnrollment();

    /**
     * Withdraws the account from the WhatsApp Web and Desktop beta
     * program.
     *
     * @apiNote
     * The Web and Desktop clients share the same JS bundle, so this
     * single toggle controls the beta channel for both surfaces.
     * Withdrawing emits a singleton {@code external_web_beta} mutation in
     * the {@code REGULAR} sync collection so every linked Web/Desktop
     * installation converges on the same value.
     *
     * @see #enableWebBetaEnrollment()
     */
    void disableWebBetaEnrollment();

    /**
     * Forces every outgoing VoIP call to route through WhatsApp relays
     * instead of peer-to-peer.
     *
     * @apiNote
     * Drives the "Always relay calls" privacy switch on the Settings
     * privacy-calls surface; when enabled, relayed calls mask the
     * caller's network identifiers from the callee. The change
     * propagates to every linked device via the singleton
     * {@code privacySettingRelayAllCalls} mutation in the
     * {@code REGULAR} sync collection.
     *
     * @see #disableAlwaysRelayCalls()
     */
    void enableAlwaysRelayCalls();

    /**
     * Allows outgoing VoIP calls to route peer-to-peer instead of always
     * through WhatsApp relays.
     *
     * @apiNote
     * Drives the "Always relay calls" privacy switch on the Settings
     * privacy-calls surface; when disabled, calls may connect directly,
     * exposing the caller's network identifiers to the callee. The
     * change propagates to every linked device via the singleton
     * {@code privacySettingRelayAllCalls} mutation in the
     * {@code REGULAR} sync collection.
     *
     * @see #enableAlwaysRelayCalls()
     */
    void disableAlwaysRelayCalls();

    /**
     * Enables Meta AI's on-device Private Processing for sensitive
     * computations.
     *
     * @apiNote
     * Drives the Private Processing toggle on the chat settings surface.
     * The change propagates to every linked device via the singleton
     * {@code private_processing_setting} mutation in the
     * {@code REGULAR_HIGH} sync collection. The protobuf schema models
     * three states ({@code UNDEFINED}, {@code ENABLED},
     * {@code DISABLED}); this method emits the explicit {@code ENABLED}
     * value.
     *
     * @see #disableAiPrivateProcessing()
     */
    void enableAiPrivateProcessing();

    /**
     * Disables Meta AI's on-device Private Processing for sensitive
     * computations.
     *
     * @apiNote
     * Drives the Private Processing toggle on the chat settings surface.
     * The change propagates to every linked device via the singleton
     * {@code private_processing_setting} mutation in the
     * {@code REGULAR_HIGH} sync collection. The protobuf schema models
     * three states ({@code UNDEFINED}, {@code ENABLED},
     * {@code DISABLED}); this method emits the explicit {@code DISABLED}
     * value.
     *
     * @see #enableAiPrivateProcessing()
     */
    void disableAiPrivateProcessing();

    /**
     * Enables cross-device sync of automated message and contact
     * detections (spam, flagged-account warnings).
     *
     * @apiNote
     * Drives the Click-To-WhatsApp detected-outcomes onboarding switch
     * exposed on the Web settings surface. The change propagates to
     * every linked device via the singleton
     * {@code detected_outcomes_status_action} mutation in the
     * {@code REGULAR} sync collection.
     *
     * @see #disableAutomatedDetections()
     */
    void enableAutomatedDetections();

    /**
     * Disables cross-device sync of automated message and contact
     * detections (spam, flagged-account warnings).
     *
     * @apiNote
     * Drives the Click-To-WhatsApp detected-outcomes onboarding switch
     * exposed on the Web settings surface. The change propagates to
     * every linked device via the singleton
     * {@code detected_outcomes_status_action} mutation in the
     * {@code REGULAR} sync collection.
     *
     * @see #enableAutomatedDetections()
     */
    void disableAutomatedDetections();

    /**
     * Marks a one-time onboarding hint as dismissed on every linked
     * device so it does not re-appear.
     *
     * @apiNote
     * Drives the acknowledgement path for tooltips, banners, and
     * one-time tips WhatsApp surfaces on the Web and Desktop client.
     * Each hint is identified by an opaque key; the mutation is keyed
     * on that identifier so each dismissal gets its own row in the
     * sync stream.
     *
     * @param hintId the opaque identifier of the onboarding hint to
     *               dismiss
     * @throws NullPointerException if {@code hintId} is {@code null}
     */
    void dismissOnboardingHint(String hintId);

    /**
     * Restores a previously dismissed onboarding hint so it can appear
     * again on every linked device.
     *
     * @apiNote
     * Counterpart to {@link #dismissOnboardingHint(String)}; emits the
     * same {@code nux_action} mutation with the acknowledged flag set
     * back to {@code false}.
     *
     * @param hintId the opaque identifier of the onboarding hint to
     *               restore
     * @throws NullPointerException if {@code hintId} is {@code null}
     */
    void restoreOnboardingHint(String hintId);

    /**
     * Marks a tappable button on an interactive message as used so it
     * appears disabled on every linked device.
     *
     * @apiNote
     * Drives the post-tap state of call-to-action, flow-trigger, and
     * list-entry buttons on interactive and template messages: tapping
     * a button on one device should disable it on every linked device
     * so a user cannot retrigger the same flow twice. The mutation is
     * keyed on the button identifier so each tappable element gets its
     * own row.
     *
     * @param buttonId the identifier of the interactive button being
     *                 disabled
     * @throws NullPointerException if {@code buttonId} is {@code null}
     */
    void disableInteractiveMessageButton(String buttonId);

    /**
     * Publishes the local recent-emoji usage ranking so every linked
     * device shows the same suggestion order in the emoji picker.
     *
     * @apiNote
     * Each entry pairs an emoji glyph with a usage weight; receivers
     * replace their snapshot wholesale. An empty list is valid and
     * represents "no recent usage".
     *
     * @param usage the per-emoji weight snapshot to publish
     * @throws NullPointerException if {@code usage} is {@code null}
     */
    void editRecentEmojiUsage(List<RecentEmojiWeight> usage);

    /**
     * Edits the addressbook entry for the given contact and propagates
     * the change to every linked device.
     *
     * @apiNote
     * Drives the contact-editor surface: adding or renaming a contact
     * emits a {@code contactAction} mutation that the primary device
     * replays against its native addressbook. Use
     * {@link #deleteContact(JidProvider)} to remove an entry instead.
     *
     * @param edit the contact-edit packet describing the target and the
     *             fields to update; only the target JID is required
     * @throws NullPointerException if {@code edit} is {@code null}
     */
    void editContact(ContactEdit edit);

    /**
     * Removes the addressbook entry for the given contact from every
     * linked device.
     *
     * @apiNote
     * Drives the contact-deletion path: emits a {@code contactAction}
     * mutation with operation {@code REMOVE} so receiving devices drop
     * the matching addressbook row.
     *
     * @param contact the JID of the contact to remove
     * @throws NullPointerException if {@code contact} is {@code null}
     */
    void deleteContact(JidProvider contact);

    /**
     * Records a finished VoIP call in the cross-device call history.
     *
     * @apiNote
     * Emit one mutation per terminated VoIP call. The {@link CallLog}
     * argument already carries the call identifier, the creator JID,
     * and the incoming/outgoing direction; the mutation index is
     * derived from those fields so every linked device sees the same
     * entry in its call tab. The {@link CallLog#callId() callId} and
     * {@link CallLog#callCreatorJid() callCreatorJid} fields must be
     * present.
     *
     * @param entry the call record to ship; must carry a non-empty
     *              {@code callId} and a {@code callCreatorJid}
     * @throws NullPointerException     if {@code entry} is {@code null}
     * @throws IllegalArgumentException if {@code entry} is missing
     *                                  {@code callId} or
     *                                  {@code callCreatorJid}
     */
    void addCallLog(CallLog entry);

    /**
     * Opts a specific customer into sharing their data with the
     * advertiser that drove the conversation.
     *
     * @apiNote
     * Drives the per-customer data-sharing switch surfaced on the
     * Click-To-WhatsApp advertising flow. The mutation is keyed on the
     * customer's LID so each customer has its own row.
     *
     * @param customer the customer's LID-form JID
     * @throws NullPointerException if {@code customer} is {@code null}
     * @see #disableAdvertiserDataSharing(Jid)
     */
    void enableAdvertiserDataSharing(Jid customer);

    /**
     * Opts a specific customer out of sharing their data with the
     * advertiser that drove the conversation.
     *
     * @apiNote
     * Drives the per-customer data-sharing switch surfaced on the
     * Click-To-WhatsApp advertising flow. The mutation is keyed on the
     * customer's LID so each customer has its own row.
     *
     * @param customer the customer's LID-form JID
     * @throws NullPointerException if {@code customer} is {@code null}
     * @see #enableAdvertiserDataSharing(Jid)
     */
    void disableAdvertiserDataSharing(Jid customer);

    /**
     * Replaces the merchant's full set of custom payment methods.
     *
     * @apiNote
     * Drives the Brazil PIX phase-1 seller-sync feature: merchants curate
     * the payment methods that they want to offer their customers, and
     * the mutation overwrites the entire list on every linked device.
     *
     * @param methods the new snapshot of custom payment methods to
     *                broadcast
     * @throws NullPointerException if {@code methods} is {@code null}
     */
    void editCustomPaymentMethods(List<CustomPaymentMethod> methods);

    /**
     * Records the user's acceptance of a region-specific payments
     * terms-of-service notice.
     *
     * @apiNote
     * Drives the merchant terms-of-service flow that prefaces payments
     * features (for example the Brazilian PIX privacy policy). The
     * acceptance state propagates to every linked device so the user is
     * not prompted again.
     *
     * @param notice the payment notice the acceptance applies to
     * @throws NullPointerException if {@code notice} is {@code null}
     * @see #revokePaymentTos(PaymentTosAction.PaymentNotice)
     */
    void acceptPaymentTos(PaymentTosAction.PaymentNotice notice);

    /**
     * Records the user's revocation of a previously accepted
     * region-specific payments terms-of-service notice.
     *
     * @apiNote
     * Drives the merchant terms-of-service flow that prefaces payments
     * features (for example the Brazilian PIX privacy policy). The
     * revocation propagates to every linked device.
     *
     * @param notice the payment notice the revocation applies to
     * @throws NullPointerException if {@code notice} is {@code null}
     * @see #acceptPaymentTos(PaymentTosAction.PaymentNotice)
     */
    void revokePaymentTos(PaymentTosAction.PaymentNotice notice);

    /**
     * Registers an event listener.
     *
     * <p>The argument may be any {@link WhatsAppListener} subtype: a per-event
     * functional interface (for example {@link NewMessageListener},
     * {@link LinkedChatsListener}, {@link LinkedContactPresenceListener}) or the
     * aggregator {@link LinkedWhatsAppClientListener}. The dispatch layer
     * recovers the concrete event interface through {@code instanceof}
     * pattern matching, so a single-event lambda only ever receives the
     * one event it implements.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addListener(LinkedListener listener);

    /**
     * Unregisters an event listener previously passed to
     * {@link #addListener(LinkedListener)} or to one of the typed
     * {@code addXxxListener} methods.
     *
     * @param listener the listener
     * @return this client
     */
    @Override
    LinkedWhatsAppClient removeListener(WhatsAppListener listener);

    LinkedWhatsAppClient addChatsListener(LinkedChatsListener listener);

    LinkedWhatsAppClient addContactsListener(LinkedContactsListener listener);

    LinkedWhatsAppClient addStatusListener(LinkedStatusListener listener);

    LinkedWhatsAppClient addNodeSentListener(LinkedNodeSentListener listener);

    LinkedWhatsAppClient addCallListener(LinkedCallListener listener);

    LinkedWhatsAppClient addWebHistorySyncPastParticipantsListener(LinkedWebHistorySyncPastParticipantsListener listener);

    LinkedWhatsAppClient addWebAppPrimaryFeaturesListener(LinkedWebAppPrimaryFeaturesListener listener);

    LinkedWhatsAppClient addContactPresenceListener(LinkedContactPresenceListener listener);

    LinkedWhatsAppClient addNewslettersListener(LinkedNewslettersListener listener);

    LinkedWhatsAppClient addNodeReceivedListener(LinkedNodeReceivedListener listener);

    LinkedWhatsAppClient addWebAppStateActionListener(LinkedWebAppStateActionListener listener);

    LinkedWhatsAppClient addWebHistorySyncMessagesListener(LinkedWebHistorySyncMessagesListener listener);

    LinkedWhatsAppClient addNewStatusListener(LinkedNewStatusListener listener);

    LinkedWhatsAppClient addAboutChangedListener(LinkedAboutChangedListener listener);

    LinkedWhatsAppClient addIntegrityChallengeListener(LinkedIntegrityChallengeListener listener);

    LinkedWhatsAppClient addPrivacySettingChangedListener(LinkedPrivacySettingChangedListener listener);

    LinkedWhatsAppClient addMessageQuarantinedListener(LinkedMessageQuarantinedListener listener);

    LinkedWhatsAppClient addWebHistorySyncProgressListener(LinkedWebHistorySyncProgressListener listener);

    LinkedWhatsAppClient addProfilePictureChangedListener(LinkedProfilePictureChangedListener listener);

    LinkedWhatsAppClient addNameChangedListener(LinkedNameChangedListener listener);

    LinkedWhatsAppClient addMessageReplyListener(LinkedMessageReplyListener listener);

    LinkedWhatsAppClient addDeviceIdentityChangedListener(LinkedDeviceIdentityChangedListener listener);

    LinkedWhatsAppClient addNewContactListener(LinkedNewContactListener listener);

    LinkedWhatsAppClient addContactBlockedListener(LinkedContactBlockedListener listener);

    LinkedWhatsAppClient addContactTextStatusListener(LinkedContactTextStatusListener listener);

    LinkedWhatsAppClient addLocaleChangedListener(LinkedLocaleChangedListener listener);

    LinkedWhatsAppClient addRegistrationCodeListener(LinkedRegistrationCodeListener listener);

    /**
     * Registers a per-event listener that is notified when the group list
     * has been received from WhatsApp.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addGroupsListener(LinkedGroupsListener listener);

    /**
     * Registers a per-event listener that is notified when the global block
     * list has been received from WhatsApp.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addBlockedContactsListener(LinkedBlockedContactsListener listener);

    /**
     * Registers a per-event listener that is notified when a category
     * blacklist (for example status recipients) is updated.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addContactBlacklistListener(LinkedContactBlacklistListener listener);

    /**
     * Registers a per-event listener that is notified when the list of
     * companion devices linked to this account changes.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addLinkedDevicesListener(LinkedDevicesListener listener);

    /**
     * Registers a per-event listener that is notified when the status
     * privacy setting (audience for the next status post) changes.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addStatusPrivacyChangedListener(LinkedStatusPrivacyChangedListener listener);

    /**
     * Registers a per-event listener that is notified when the account's
     * disappearing-messages default changes.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addDisappearingModeChangedListener(LinkedDisappearingModeChangedListener listener);

    /**
     * Registers a per-event listener that is notified when the recipient
     * opt-out list for a privacy category changes.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addOptOutListListener(LinkedOptOutListListener listener);

    /**
     * Registers a per-event listener that is notified when a call ends
     * (locally or by the peer) and reports the termination reason.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallEndedListener(LinkedCallEndedListener listener);

    /**
     * Registers a per-event listener that is notified when a peer pre-accepts
     * an incoming call before fully answering it.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallPreacceptListener(LinkedCallPreacceptListener listener);

    /**
     * Registers a per-event listener that is notified when a participant's
     * microphone is muted or unmuted during a call.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallMuteChangedListener(LinkedCallMuteChangedListener listener);

    /**
     * Registers a per-event listener that is notified when a participant's
     * camera is enabled or disabled during a call.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallVideoStateChangedListener(LinkedCallVideoStateChangedListener listener);

    /**
     * Registers a per-event listener that is notified when a peer requests
     * to upgrade an audio call to a video call.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallVideoUpgradeRequestListener(LinkedCallVideoUpgradeRequestListener listener);

    /**
     * Registers a per-event listener that is notified when a peer requests
     * to join the lobby of a call link.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallLinkLobbyJoinRequestListener(LinkedCallLinkLobbyJoinRequestListener listener);

    /**
     * Registers a per-event listener that is notified when the host admits
     * a peer waiting in the call-link lobby.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallLinkAdmittedListener(LinkedCallLinkAdmittedListener listener);

    /**
     * Registers a per-event listener that is notified when the host denies
     * a peer waiting in the call-link lobby.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallLinkDeniedListener(LinkedCallLinkDeniedListener listener);

    /**
     * Registers a per-event listener that is notified when a call
     * interaction (reaction, raise-hand, request-keyframe, request-peer-mute,
     * request-video-upgrade) is received from a participant.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallInteractionListener(LinkedCallInteractionListener listener);

    /**
     * Registers a per-event listener that is notified when participants join
     * or leave an ongoing group call.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallParticipantsChangedListener(LinkedCallParticipantsChangedListener listener);

    /**
     * Registers a per-event listener that is notified when a peer transitions
     * through a connection-state value during call signaling.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallPeerStateChangedListener(LinkedCallPeerStateChangedListener listener);

    /**
     * Registers a per-event listener that is notified of the high-priority
     * push-notice variant of an incoming call.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addCallOfferNoticeListener(LinkedCallOfferNoticeListener listener);

    /**
     * Registers a per-event listener that is notified when the relay GraphQL
     * session credential is rotated or refreshed.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addWhatsAppWebGraphQlSessionChangedListener(LinkedGraphQlSessionChangedListener listener);

    /**
     * Registers a per-event listener that is notified when the comet
     * (CTWA Graph API) access-token session changes.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addFacebookGraphQlSessionChangedListener(LinkedFacebookGraphQlSessionChangedListener listener);

    /**
     * Registers a per-event listener that is notified when the business
     * data-sharing consent setting changes.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addBusinessPrivacySettingChangedListener(LinkedBusinessPrivacySettingChangedListener listener);

    /**
     * Registers a per-event listener that is notified when the set of
     * acknowledged in-app terms-of-service notices changes.
     *
     * @param listener the listener
     * @return this client
     */
    LinkedWhatsAppClient addTosNoticesChangedListener(LinkedTosNoticesChangedListener listener);

    /**
     * Adds a product to a business catalog owned by the given account.
     *
     * <p>Files a new product under the account's catalog and returns the created product as the server
     * recorded it, including the server-assigned identifier and the moderation outcome.
     *
     * @apiNote
     * Convenience overload that calls {@link #addCatalogProduct(BusinessCatalogProductCreate)} with
     * the supplied account and product fields and no thumbnail dimensions.
     *
     * @param bizJid      the account that owns the catalog
     * @param productInfo the product fields, or {@code null} to omit them
     * @return the created product as recorded by the server, or {@link Optional#empty()} when the
     *         server returned no product
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessProduct> addCatalogProduct(JidProvider bizJid, CatalogProductInfo productInfo);

    /**
     * Adds a product to a business catalog owned by the given account.
     *
     * <p>Files a new product under the account's catalog and returns the created product as the server
     * recorded it, including the server-assigned identifier and the moderation outcome.
     *
     * @apiNote
     * Backs the "Add product" affordance in the business catalog editor, where the merchant supplies
     * the product name, media, price, and compliance details.
     *
     * @param input the catalog and product fields to file; see {@link BusinessCatalogProductCreate}
     * @return the created product as recorded by the server, or {@link Optional#empty()} when the
     *         server returned no product
     * @throws NullPointerException           if {@code input} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessProduct> addCatalogProduct(BusinessCatalogProductCreate input);

    /**
     * Edits an existing product in a business catalog owned by the given account.
     *
     * <p>Returns the product as the server recorded it after the edit, including the moderation outcome
     * the change triggered.
     *
     * @apiNote
     * Convenience overload that calls {@link #editCatalogProduct(BusinessCatalogProductEdit)} with the
     * supplied account, product id, and changed fields and no thumbnail dimensions.
     *
     * @param bizJid      the account that owns the catalog
     * @param productId   the identifier of the product to edit
     * @param productInfo the changed product fields, or {@code null} to omit them
     * @return the edited product as recorded by the server, or {@link Optional#empty()} when the
     *         server returned no product
     * @throws NullPointerException           if {@code bizJid} or {@code productId} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessProduct> editCatalogProduct(JidProvider bizJid, String productId, CatalogProductInfo productInfo);

    /**
     * Edits an existing product in a business catalog owned by the given account.
     *
     * <p>Returns the product as the server recorded it after the edit, including the moderation outcome
     * the change triggered. The changed fields are supplied as a pre-encoded JSON object literal because
     * the field set varies per product and is not modelled as fixed typed fields.
     *
     * @apiNote
     * Backs the "Edit product" affordance in the business catalog editor, where the merchant changes
     * the product name, media, price, or compliance details.
     *
     * @param input the catalog, target product, and changed fields; see {@link BusinessCatalogProductEdit}
     * @return the edited product as recorded by the server, or {@link Optional#empty()} when the
     *         server returned no product
     * @throws NullPointerException           if {@code input} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessProduct> editCatalogProduct(BusinessCatalogProductEdit input);

    /**
     * Deletes one or more products from a business catalog owned by the given account.
     *
     * <p>Removes every product whose identifier appears in {@code productIds} and returns whether the
     * removal took effect; {@link BusinessCatalogMutationResult#success()} is {@code true} when the
     * server removed at least one product.
     *
     * @apiNote
     * Backs the "Delete product" affordance in the business catalog editor.
     *
     * @param bizJid     the account that owns the catalog
     * @param productIds the identifiers of the products to delete
     * @return the deletion outcome, or {@link Optional#empty()} when the server returned no result
     * @throws NullPointerException           if {@code bizJid} or {@code productIds} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> deleteCatalogProduct(JidProvider bizJid, List<String> productIds);

    /**
     * Creates a new product catalog owned by the given business account.
     *
     * <p>Returns whether the catalog was created; {@link BusinessCatalogMutationResult#success()} is
     * {@code true} when the server provisioned the catalog.
     *
     * @apiNote
     * Convenience overload that calls {@link #createCatalog(JidProvider, String)} with no platform
     * hint.
     *
     * @param bizJid the account that will own the catalog
     * @return the creation outcome, or {@link Optional#empty()} when the server returned no result
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> createCatalog(JidProvider bizJid);

    /**
     * Creates a new product catalog owned by the given business account.
     *
     * <p>Returns whether the catalog was created; {@link BusinessCatalogMutationResult#success()} is
     * {@code true} when the server provisioned the catalog. The platform hint records the surface that
     * initiated creation.
     *
     * @apiNote
     * Backs the onboarding step that provisions a merchant's first catalog before any products can be
     * added.
     *
     * @param bizJid   the account that will own the catalog
     * @param platform the originating platform hint, or {@code null} to omit it
     * @return the creation outcome, or {@link Optional#empty()} when the server returned no result
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> createCatalog(JidProvider bizJid, String platform);

    /**
     * Creates a new collection inside a business catalog owned by the given account.
     *
     * <p>Returns the created collection as the server recorded it, carrying the server-assigned
     * identifier and the moderation outcome. The server does not echo the seeded products, so the
     * returned collection's {@link BusinessProductCollection#products() products} list is empty.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #createCatalogCollection(BusinessCatalogCollectionCreate)} with no session correlator.
     *
     * @param bizJid     the account that owns the catalog
     * @param name       the display name of the new collection
     * @param productIds the identifiers of the products to seed the collection with
     * @return the created collection as recorded by the server, or {@link Optional#empty()} when the
     *         server returned no collection
     * @throws NullPointerException           if {@code bizJid}, {@code name}, or {@code productIds} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessProductCollection> createCatalogCollection(JidProvider bizJid, String name, List<String> productIds);

    /**
     * Creates a new collection inside a business catalog owned by the given account.
     *
     * <p>Returns the created collection as the server recorded it, carrying the server-assigned
     * identifier and the moderation outcome. The server does not echo the seeded products, so the
     * returned collection's {@link BusinessProductCollection#products() products} list is empty.
     *
     * @apiNote
     * Backs the "Create collection" affordance in the business catalog editor, where the merchant names
     * a collection and optionally seeds it with products.
     *
     * @param input the catalog, collection name, and seed products; see
     *              {@link BusinessCatalogCollectionCreate}
     * @return the created collection as recorded by the server, or {@link Optional#empty()} when the
     *         server returned no collection
     * @throws NullPointerException           if {@code input} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessProductCollection> createCatalogCollection(BusinessCatalogCollectionCreate input);

    /**
     * Deletes one or more collections from a business catalog owned by the given account.
     *
     * <p>Removes every collection whose identifier appears in {@code collectionIds} and returns whether
     * the removal took effect; {@link BusinessCatalogMutationResult#success()} is {@code true} when the
     * server applied the deletion.
     *
     * @apiNote
     * Backs the "Delete collection" affordance in the business catalog editor.
     *
     * @param collectionIds    the identifiers of the collections to delete
     * @param bizJid           the account that owns the catalog
     * @param catalogSessionId the optional catalog session id correlating a browse session, or
     *                         {@code null} when not used
     * @return the deletion outcome, or {@link Optional#empty()} when the server returned no result
     * @throws NullPointerException           if {@code collectionIds} or {@code bizJid} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> deleteCatalogCollections(List<String> collectionIds, JidProvider bizJid, String catalogSessionId);

    /**
     * Updates a collection inside a business catalog owned by the given account.
     *
     * <p>Renames the collection and/or adds and removes products, then returns the collection as the
     * server recorded it, carrying its identifier and the moderation outcome the change triggered. The
     * server does not echo the collection's products, so the returned collection's
     * {@link BusinessProductCollection#products() products} list is empty.
     *
     * @apiNote
     * Backs the "Edit collection" affordance in the business catalog editor.
     *
     * @param input the collection identifier, owning catalog, optional new name, and add/remove
     *              product lists; see {@link BusinessCatalogCollectionEdit}
     * @return the updated collection as recorded by the server, or {@link Optional#empty()} when the
     *         server returned no collection
     * @throws NullPointerException           if {@code input} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessProductCollection> updateCatalogCollection(BusinessCatalogCollectionEdit input);

    /**
     * Reorders the collections of a business catalog owned by the given account.
     *
     * <p>Applies the given moves and returns whether the reorder took effect;
     * {@link BusinessCatalogMutationResult#success()} is {@code true} when the server applied the new
     * ordering. Each move carries the collection id and its source and destination index.
     *
     * @apiNote
     * Backs the drag-to-reorder affordance in the business catalog editor.
     *
     * @param bizJid the account that owns the catalog
     * @param moves  the ordered list of collection moves to apply
     * @return the reorder outcome, or {@link Optional#empty()} when the server returned no result
     * @throws NullPointerException           if {@code bizJid} or {@code moves} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> reorderCatalogCollections(JidProvider bizJid, List<BusinessCatalogCollectionMove> moves);

    /**
     * Appeals the moderation verdict on a product in a business catalog owned by the given account.
     *
     * <p>Forwards the appeal reason to the review pipeline and returns whether the appeal was accepted;
     * {@link BusinessCatalogMutationResult#success()} is {@code true} when the server accepted the
     * appeal.
     *
     * @apiNote
     * Backs the "Request review" affordance the merchant sees when a product is rejected by moderation.
     *
     * @param jid       the account that owns the catalog
     * @param productId the identifier of the rejected product
     * @param reason    the free-text appeal reason
     * @return the appeal outcome, or {@link Optional#empty()} when the server returned no result
     * @throws NullPointerException           if {@code jid}, {@code productId}, or {@code reason} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> appealCatalogProduct(JidProvider jid, String productId, String reason);

    /**
     * Appeals the moderation verdict on a collection in a business catalog owned by the given account.
     *
     * <p>Forwards the appeal reason to the review pipeline and returns whether the appeal was accepted;
     * {@link BusinessCatalogMutationResult#success()} is {@code true} when the server accepted the
     * appeal.
     *
     * @apiNote
     * Backs the "Request review" affordance the merchant sees when a collection is rejected by
     * moderation.
     *
     * @param productSetId the identifier of the rejected collection
     * @param jid          the account that owns the catalog
     * @param reason       the free-text appeal reason
     * @return the appeal outcome, or {@link Optional#empty()} when the server returned no result
     * @throws NullPointerException           if {@code productSetId}, {@code jid}, or {@code reason}
     *                                        is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> appealCatalogCollection(String productSetId, JidProvider jid, String reason);

    /**
     * Updates whether the shopping cart is enabled for a business account.
     *
     * <p>Toggles the cart for the account and returns the resulting cart state the server echoes back,
     * which normally matches {@code cartEnabled} but reflects whatever the server applied.
     *
     * @apiNote
     * Backs the "Cart" toggle in the business commerce settings; enabling the cart lets customers
     * collect catalog products into an order before checking out.
     *
     * @param bizJid      the business account whose commerce settings change
     * @param cartEnabled {@code true} to enable the cart, {@code false} to disable it
     * @return {@code true} when the cart is enabled after the update, {@code false} otherwise or when
     *         the server returned no result
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    boolean updateCatalogCommerceSettings(JidProvider bizJid, boolean cartEnabled);

    /**
     * Updates the hidden or visible state of one or more products in a business catalog owned by the
     * given account.
     *
     * <p>Applies the per-product visibility changes and returns whether the change took effect;
     * {@link BusinessCatalogMutationResult#success()} is {@code true} when the server applied the
     * change. Hidden products remain in the catalog but are not shown to customers.
     *
     * @apiNote
     * Backs the per-product visibility toggle in the business catalog editor.
     *
     * @param jid      the account that owns the catalog
     * @param products the per-product visibility changes to apply
     * @return the visibility-update outcome, or {@link Optional#empty()} when the server returned no
     *         result
     * @throws NullPointerException           if {@code jid} or {@code products} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the server reports an error
     */
    Optional<BusinessCatalogMutationResult> updateCatalogProductVisibility(JidProvider jid, List<BusinessCatalogProductVisibility> products);

    /**
     * Fetches a page of a WhatsApp Business catalog using only its owner.
     *
     * @apiNote
     * Convenience overload that calls {@link #fetchCatalog(JidProvider, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}, so the server applies every default.
     *
     * @param jid the catalog-owning business account
     * @return the catalog page carrying the products and paging cursors, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws NullPointerException           if {@code jid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessCatalogPage> fetchCatalog(JidProvider jid);

    /**
     * Fetches a page of a WhatsApp Business catalog.
     *
     * @apiNote
     * Backs the merchant-side catalog management view: unlike the customer-facing
     * {@link #queryBusinessCatalog(JidProvider)}, this read exposes the full per-product detail,
     * moderation status, and the cursors needed to page through the catalog. Tune pagination, image
     * rendering, and the variant projection through {@code options}; the
     * {@link CatalogFetchOptions#collectionId()} option restricts the page to a single collection.
     *
     * @param jid     the catalog-owning business account
     * @param options the read options tuning pagination, rendering, and projection
     * @return the catalog page carrying the products and paging cursors, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws NullPointerException           if {@code jid} or {@code options} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessCatalogPage> fetchCatalog(JidProvider jid, CatalogFetchOptions options);

    /**
     * Fetches a page of collections from a WhatsApp Business catalog using only its owner.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #fetchCatalogCollections(JidProvider, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}, so the server applies every default.
     *
     * @param bizJid the catalog-owning business account
     * @return the collections page carrying the collections and forward cursor, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessCatalogCollections> fetchCatalogCollections(JidProvider bizJid);

    /**
     * Fetches a page of collections from a WhatsApp Business catalog.
     *
     * @apiNote
     * Backs the merchant-side collections management view, exposing each collection's products with the
     * full per-product detail plus the forward cursor for paging through the collections. Tune
     * pagination, image rendering, and the variant projection through {@code options}.
     *
     * @param bizJid  the catalog-owning business account
     * @param options the read options tuning pagination, rendering, and projection
     * @return the collections page carrying the collections and forward cursor, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code bizJid} or {@code options} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessCatalogCollections> fetchCatalogCollections(JidProvider bizJid, CatalogFetchOptions options);

    /**
     * Fetches a single product from a WhatsApp Business catalog using only its owner and identifier.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #fetchCatalogProduct(JidProvider, String, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}, so the server applies every default.
     *
     * @param jid       the catalog-owning business account
     * @param productId the identifier of the product to fetch
     * @return the full product, or {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code jid} or {@code productId} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessProduct> fetchCatalogProduct(JidProvider jid, String productId);

    /**
     * Fetches a single product from a WhatsApp Business catalog.
     *
     * @apiNote
     * Backs the merchant-side product detail view, returning the full product including its media, sale
     * price, regulatory compliance, and moderation status. Tune image rendering, the variant
     * projection, and whether the compliance block is included through {@code options}.
     *
     * @param jid       the catalog-owning business account
     * @param productId the identifier of the product to fetch
     * @param options   the read options tuning rendering, projection, and the compliance block
     * @return the full product, or {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code jid}, {@code productId}, or {@code options} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessProduct> fetchCatalogProduct(JidProvider jid, String productId, CatalogFetchOptions options);

    /**
     * Fetches a list of products from a WhatsApp Business catalog using only its owner and ids.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #fetchCatalogProductList(JidProvider, List, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}, so the server applies every default.
     *
     * @param jid        the catalog-owning business account
     * @param productIds the identifiers of the products to fetch
     * @return the products resolved for the requested ids, never {@code null}
     * @throws NullPointerException           if {@code jid} or {@code productIds} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessProduct> fetchCatalogProductList(JidProvider jid, List<String> productIds);

    /**
     * Fetches a list of products from a WhatsApp Business catalog.
     *
     * @apiNote
     * Backs the merchant-side bulk product fetch, resolving an explicit set of product ids to their
     * full product detail in one round trip. Tune image rendering and direct-connection routing through
     * {@code options}.
     *
     * @param jid        the catalog-owning business account
     * @param productIds the identifiers of the products to fetch
     * @param options    the read options tuning rendering and routing
     * @return the products resolved for the requested ids, never {@code null}
     * @throws NullPointerException           if {@code jid}, {@code productIds}, or {@code options} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessProduct> fetchCatalogProductList(JidProvider jid, List<String> productIds, CatalogFetchOptions options);

    /**
     * Fetches a single collection from a WhatsApp Business catalog using only its owner and id.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #fetchCatalogSingleCollection(JidProvider, String, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}, so the server applies every default.
     *
     * @param bizJid the catalog-owning business account
     * @param id     the identifier of the collection to fetch
     * @return the collection with its products and next-page cursor, or {@link Optional#empty()} when
     *         the server returned no payload
     * @throws NullPointerException           if {@code bizJid} or {@code id} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessProductCollection> fetchCatalogSingleCollection(JidProvider bizJid, String id);

    /**
     * Fetches a single collection from a WhatsApp Business catalog.
     *
     * @apiNote
     * Backs the merchant-side collection detail view, returning the collection's products with the full
     * per-product detail; the returned collection's
     * {@link BusinessProductCollection#afterCursor() next-page cursor} continues paging through its
     * products. Tune pagination, image rendering, and the variant projection through {@code options}.
     *
     * @param bizJid  the catalog-owning business account
     * @param id      the identifier of the collection to fetch
     * @param options the read options tuning pagination, rendering, and projection
     * @return the collection with its products and next-page cursor, or {@link Optional#empty()} when
     *         the server returned no payload
     * @throws NullPointerException           if {@code bizJid}, {@code id}, or {@code options} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessProductCollection> fetchCatalogSingleCollection(JidProvider bizJid, String id, CatalogFetchOptions options);

    /**
     * Reports whether a WhatsApp Business catalog exposes at least one product category.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #queryCatalogHasCategories(JidProvider, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}. The catalog "has categories" when at least one category is
     * present, which drives whether the category browse surface is offered to customers.
     *
     * @param bizJid the catalog-owning business account
     * @return {@code true} when the catalog exposes at least one category, {@code false} otherwise
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    boolean queryCatalogHasCategories(JidProvider bizJid);

    /**
     * Reports whether a WhatsApp Business catalog exposes at least one product category.
     *
     * @apiNote
     * Drives whether the category browse surface is offered to customers: the catalog "has categories"
     * when at least one category is present. The {@link CatalogFetchOptions#imageWidth()},
     * {@link CatalogFetchOptions#imageHeight()}, {@link CatalogFetchOptions#catalogSessionId()}, and
     * {@link CatalogFetchOptions#directConnectionEncryptedInfo()} options tune the underlying read; the
     * other options do not apply.
     *
     * @param bizJid  the catalog-owning business account
     * @param options the read options tuning image rendering, the browse session, and routing
     * @return {@code true} when the catalog exposes at least one category, {@code false} otherwise
     * @throws NullPointerException           if {@code bizJid} or {@code options} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    boolean queryCatalogHasCategories(JidProvider bizJid, CatalogFetchOptions options);

    /**
     * Queries a list of products from a WhatsApp Business catalog using only its owner and ids.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #queryProductListJob(JidProvider, List, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}, so the server applies every default.
     *
     * @param catalogJid the catalog-owning business account
     * @param productIds the identifiers of the products to query
     * @return the products resolved for the requested ids, never {@code null}
     * @throws NullPointerException           if {@code catalogJid} or {@code productIds} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessProduct> queryProductListJob(JidProvider catalogJid, List<String> productIds);

    /**
     * Queries a list of products from a WhatsApp Business catalog.
     *
     * @apiNote
     * Backs the customer-facing product-list browse: unlike the merchant-side
     * {@link #fetchCatalogProductList(JidProvider, List)}, this read returns the public catalog view
     * for an explicit set of product ids. Tune image rendering and direct-connection routing through
     * {@code options}.
     *
     * @param catalogJid the catalog-owning business account
     * @param productIds the identifiers of the products to query
     * @param options    the read options tuning rendering and routing
     * @return the products resolved for the requested ids, never {@code null}
     * @throws NullPointerException           if {@code catalogJid}, {@code productIds}, or
     *                                        {@code options} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessProduct> queryProductListJob(JidProvider catalogJid, List<String> productIds, CatalogFetchOptions options);

    /**
     * Queries a single collection from a WhatsApp Business catalog using only its owner and id.
     *
     * @apiNote
     * Convenience overload that calls
     * {@link #querySingleProductCollection(JidProvider, String, CatalogFetchOptions)} with
     * {@link CatalogFetchOptions#empty()}, so the server applies every default.
     *
     * @param catalogJid   the catalog-owning business account
     * @param collectionId the identifier of the collection to query
     * @return the collection with its products and next-page cursor, or {@link Optional#empty()} when
     *         the server returned no payload
     * @throws NullPointerException           if {@code catalogJid} or {@code collectionId} is
     *                                        {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessProductCollection> querySingleProductCollection(JidProvider catalogJid, String collectionId);

    /**
     * Queries a single collection from a WhatsApp Business catalog.
     *
     * @apiNote
     * Backs the customer-facing collection browse: unlike the merchant-side
     * {@link #fetchCatalogSingleCollection(JidProvider, String)}, this read returns the public catalog
     * view of a collection; the returned collection's
     * {@link BusinessProductCollection#afterCursor() next-page cursor} continues paging through its
     * products. Tune pagination, image rendering, and the variant projection through {@code options}.
     *
     * @param catalogJid   the catalog-owning business account
     * @param collectionId the identifier of the collection to query
     * @param options      the read options tuning pagination, rendering, and projection
     * @return the collection with its products and next-page cursor, or {@link Optional#empty()} when
     *         the server returned no payload
     * @throws NullPointerException           if {@code catalogJid}, {@code collectionId}, or
     *                                        {@code options} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessProductCollection> querySingleProductCollection(JidProvider catalogJid, String collectionId, CatalogFetchOptions options);

    /**
     * Queries the capabilities available to this WhatsApp Business AI agent.
     *
     * @apiNote
     * The WhatsApp Business AI agent is the auto-reply assistant a merchant attaches to their business
     * account. This read lists every capability the assistant can offer paired with its current
     * availability to the account, so a caller can discover which AI features are unlocked before
     * configuring the more specific knowledge, rule, and reply-settings flows.
     *
     * @return the agent's configured state carrying the available capabilities, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiAgentHome> queryAiAbilities();

    /**
     * Seeds this WhatsApp Business AI agent's knowledge from the account's chat history.
     *
     * @apiNote
     * Backs the assistant's "learn from chat history" setup step, which teaches the auto-reply
     * assistant from the business's past conversations grouped by the customer they were held with.
     *
     * @param input the chat-history upload request, or {@code null} to omit it
     * @return the outcome reporting whether the chat-history backup was created, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> createAiChatHistory(AiChatHistoryUploadRequest input);

    /**
     * Deletes every example response of the given knowledge types for this WhatsApp Business AI
     * agent.
     *
     * @apiNote
     * Convenience overload that calls {@link #deleteAiExampleResponses(List, List)} with no entry ids,
     * deleting every question/answer entry of the named knowledge types.
     *
     * @param knowledgeTypesToDelete the knowledge types to delete, or {@code null} to omit them
     * @return the outcome reporting whether the entries were removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiExampleResponses(List<String> knowledgeTypesToDelete);

    /**
     * Deletes example responses (frequently-asked-question knowledge) for this WhatsApp Business AI
     * agent.
     *
     * @apiNote
     * Removes some of the assistant's frequently-asked-question knowledge: pass the knowledge types to
     * clear and, optionally, the specific question/answer entry ids to remove within those types.
     *
     * @param knowledgeTypesToDelete the knowledge types to delete, or {@code null} to omit them
     * @param faqIds                 the specific question/answer entry ids to delete, or {@code null} to
     *                               delete every entry of the named types
     * @return the outcome reporting whether the entries were removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiExampleResponses(List<String> knowledgeTypesToDelete, List<String> faqIds);

    /**
     * Queries this WhatsApp Business AI agent's example responses (the frequently-asked-question
     * knowledge the assistant answers from).
     *
     * @apiNote
     * Reads the knowledge facets of the agent's configured state: the ordered knowledge entries
     * (free-text statements and question/answer entries), the website-backed material and featured
     * products, the structured product entries, and whether the product-information feature is
     * unlocked for the account.
     *
     * @return the agent's configured state carrying its knowledge, or {@link Optional#empty()} when the
     *         server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiAgentHome> queryAiExampleResponses();

    /**
     * Updates this WhatsApp Business AI agent's example responses (the frequently-asked-question
     * knowledge the assistant answers from).
     *
     * @apiNote
     * Saves the assistant's frequently-asked-question knowledge as the whole FAQ set. When the update
     * references websites that fail validation, the result lists those website URLs among its affected
     * ids and carries the first reported validation error.
     *
     * @param faq the FAQ entries to save, or {@code null} to omit them
     * @return the outcome reporting whether the update applied, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> updateAiExampleResponses(List<AiFaqEntry> faq);

    /**
     * Queries this WhatsApp Business AI agent's potential knowledge pending review as of now.
     *
     * @apiNote
     * Convenience overload that calls {@link #queryAiKnowledgeReview(Instant)} with the current instant
     * as the review cut-off, mirroring WhatsApp Web's use of the wall clock.
     *
     * @return the review queue carrying the pending knowledge items, or {@link Optional#empty()} when
     *         the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiKnowledgeReview> queryAiKnowledgeReview();

    /**
     * Queries this WhatsApp Business AI agent's potential knowledge pending review.
     *
     * @apiNote
     * Reads the review queue that lets the agent's operator approve or discard knowledge the assistant
     * inferred from past conversations. The {@code timestamp} gates which pending items are returned.
     *
     * @param timestamp the review cut-off instant, or {@code null} to omit it
     * @return the review queue carrying the pending knowledge items, or {@link Optional#empty()} when
     *         the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiKnowledgeReview> queryAiKnowledgeReview(Instant timestamp);

    /**
     * Approves (commits) the reviewed potential-knowledge items of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Backs the knowledge-review surface's "approve" action: the {@code ids} are the knowledge-item
     * identifiers surfaced by {@link #queryAiKnowledgeReview()} that the owner chose to keep.
     *
     * @param ids the knowledge-item ids to approve, or {@code null} to omit them
     * @return the outcome reporting whether the items were approved, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> commitAiKnowledgeReview(List<String> ids);

    /**
     * Deletes a pending knowledge-review item of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Backs the knowledge-review surface's "discard" action, dropping a single inferred knowledge item
     * the owner chose not to keep.
     *
     * @param id the pending knowledge-review item id to delete, or {@code null} to omit it
     * @return the outcome reporting whether the item was removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiPendingKnowledge(String id);

    /**
     * Deletes the chat-history knowledge source of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Backs the knowledge-sources surface's "remove chat history" action, severing the agent's learning
     * from the account's chat history. The operation targets the authenticated business and takes no
     * arguments.
     *
     * @return the outcome reporting whether the source was removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiChatHistorySource();

    /**
     * Deletes a website knowledge source of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Backs the knowledge-sources surface's "remove website" action. The id is the data-source
     * identifier the website-ingestion flow assigned to the source, not a WhatsApp address.
     *
     * @param websiteDataSourceId the website data-source id to delete, or {@code null} to omit it
     * @return the outcome reporting whether the source was removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiWebsiteSource(String websiteDataSourceId);

    /**
     * Deletes an uploaded-file knowledge source of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Backs the knowledge-sources surface's "remove file" action. The id is the data-source identifier
     * the upload flow assigned to the file, not a WhatsApp address.
     *
     * @param uploadedFileDataSourceId the uploaded-file data-source id to delete, or {@code null} to
     *                                 omit it
     * @return the outcome reporting whether the source was removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiFileSource(String uploadedFileDataSourceId);

    /**
     * Queries the knowledge sources configured for this WhatsApp Business AI agent.
     *
     * @apiNote
     * Reads the learning-source facets of the agent's configured state: the list of configured
     * knowledge sources (uploaded files, chat history, and websites) and the progress of exporting the
     * account's chat history into the assistant's knowledge.
     *
     * @return the agent's configured state carrying the configured knowledge sources, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiAgentHome> queryAiKnowledgeSources();

    /**
     * Adds an uploaded file as a knowledge source for this WhatsApp Business AI agent.
     *
     * @apiNote
     * After a source file is staged on Meta's blob store, this kicks off the extraction that turns it
     * into knowledge the assistant can answer from. On success the id of the created knowledge source
     * is surfaced among the result's affected ids; on failure the result carries the extraction error
     * message.
     *
     * @param manifoldFilePath     the blob-store path of the uploaded source file, or {@code null} to
     *                             omit it
     * @param userProvidedFileName the file name the merchant typed when uploading, or {@code null} to
     *                             omit it
     * @return the outcome reporting whether extraction was accepted, or {@link Optional#empty()} when
     *         the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> uploadAiKnowledgeSource(String manifoldFilePath, String userProvidedFileName);

    /**
     * Creates a lead-capture flow for this WhatsApp Business AI agent.
     *
     * @apiNote
     * Adds a flow that lets the assistant collect contact details from interested customers. The
     * {@code request} describes the flow to create and leaves its flow id unset. The returned flow is
     * present only when the creation took effect.
     *
     * @param request the flow to create, or {@code null} to omit it
     * @return the created lead-capture flow, or {@link Optional#empty()} when the creation did not take
     *         effect or the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiLeadGenForm> createAiLeadGenFlow(AiLeadGenFlowInput request);

    /**
     * Updates a lead-capture flow of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Saves changes to a flow that collects contact details from interested customers. The
     * {@code request} describes the flow changes and carries the flow id being updated. The returned
     * flow is present only when the update took effect.
     *
     * @param request the flow changes, or {@code null} to omit them
     * @return the updated lead-capture flow, or {@link Optional#empty()} when the update did not take
     *         effect or the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiLeadGenForm> updateAiLeadGenFlow(AiLeadGenFlowInput request);

    /**
     * Deletes a lead-capture flow of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Removes a flow that collected contact details from interested customers. The {@code flowId} is
     * the server-assigned identifier of the flow, not a WhatsApp address.
     *
     * @param flowId the identifier of the lead-capture flow to delete, or {@code null} to omit it
     * @return the outcome reporting whether the flow was removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiLeadGenFlow(String flowId);

    /**
     * Queries the lead-capture flows of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Reads every lead-capture flow configured for the business account, including each flow's capture
     * fields and the leads gathered so far with the customers' phone-number and LID addresses.
     *
     * @return the lead-capture flows, empty when the account has none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAiLeadGenForm> queryAiLeadGenForms();

    /**
     * Marks all captured leads of a lead-capture flow of this WhatsApp Business AI agent as seen.
     *
     * @apiNote
     * Clears the unseen-lead badge for one lead-capture flow, marking every lead it has gathered as
     * already reviewed. The {@code flowId} is the server-assigned identifier of the flow, not a
     * WhatsApp address.
     *
     * @param flowId the identifier of the lead-capture flow whose leads are being marked as seen, or
     *               {@code null} to omit it
     * @return the outcome reporting whether the leads were marked, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> markAiLeadGenAllSeen(String flowId);

    /**
     * Creates minimal product-info knowledge for this WhatsApp Business AI agent.
     *
     * @apiNote
     * Convenience overload that calls {@link #createAiProductInfo(BusinessAiProductInfoCreate)} with
     * only a name, price, and description. No imagery is uploaded and the server picks default
     * thumbnail dimensions.
     *
     * @param name         the product name
     * @param complexPrice the product price, or {@code null} to emit a JSON {@code null}
     * @param description  the product description, or {@code null} to emit a JSON {@code null}
     * @return the created product entry, or {@link Optional#empty()} when the creation did not take
     *         effect or the server returned no payload
     * @throws NullPointerException           if {@code name} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiProductInfo> createAiProductInfo(String name, String complexPrice, String description);

    /**
     * Creates product-info knowledge for this WhatsApp Business AI agent.
     *
     * @apiNote
     * Registers a product the assistant can describe to customers. The structured price and the
     * existing-image references are already-encoded values forwarded verbatim, because their internal
     * shapes are not yet modelled as typed fields. The returned entry echoes the created product and is
     * present only when the creation took effect.
     *
     * @param input the product name, structured price, description, imagery, and thumbnail dimensions;
     *              see {@link BusinessAiProductInfoCreate}
     * @return the created product entry, or {@link Optional#empty()} when the creation did not take
     *         effect or the server returned no payload
     * @throws NullPointerException           if {@code input} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiProductInfo> createAiProductInfo(BusinessAiProductInfoCreate input);

    /**
     * Updates a product-info knowledge entry of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Saves changes to a product the assistant can describe to customers. The structured price and the
     * image references are forwarded verbatim because their internal shapes are not yet modelled as
     * typed fields. The returned entry is present only when the update took effect.
     *
     * @param input the product info id, changed fields, fresh image uploads, and reused image
     *              references; see {@link BusinessAiProductInfoEdit}
     * @return the updated product entry, or {@link Optional#empty()} when the update did not take
     *         effect or the server returned no payload
     * @throws NullPointerException           if {@code input} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiProductInfo> updateAiProductInfo(BusinessAiProductInfoEdit input);

    /**
     * Deletes one or more product-info knowledge entries of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Removes products the assistant can describe to customers. The ids are product-catalog
     * identifiers rather than WhatsApp addresses; the server reports one outcome per requested id, and
     * each outcome carries the product-catalog id it refers to among its affected ids.
     *
     * @param ids the product-catalog ids to delete, or {@code null} to omit them
     * @return the per-id deletion outcomes, empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAiMutationResult> deleteAiProductInfo(List<String> ids);

    /**
     * Updates this WhatsApp Business AI agent's re-engagement settings (the automatic follow-up
     * messages the assistant sends to re-engage a contact).
     *
     * @apiNote
     * Toggles automatic re-engagement and sets its budget for the assistant. The server confirms the
     * change by echoing the persisted settings.
     *
     * @param enabled whether re-engagement is enabled, or {@code null} to omit it
     * @param amount  the configured re-engagement amount, or {@code null} to omit it
     * @return the outcome reporting whether the settings were saved, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> updateAiReengagement(Boolean enabled, Long amount);

    /**
     * Disables the daily time window of this WhatsApp Business AI agent's auto-reply bot.
     *
     * @apiNote
     * Convenience overload that calls {@link #updateAiReplyBotEnabledTime(BusinessAiReplyBotSchedule)}
     * with the schedule disabled, the system zone, and a midnight-to-midnight window. The bot replies
     * at all hours regardless of the window when {@code enabled} is {@code false}.
     *
     * @param enabled whether the timed window is active
     * @return the outcome reporting whether the window was updated, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> updateAiReplyBotEnabledTime(boolean enabled);

    /**
     * Updates the daily time window during which this WhatsApp Business AI agent's auto-reply bot is
     * enabled.
     *
     * @apiNote
     * Backs the assistant's "active hours" setting: the bot only replies between the schedule's start
     * and end times in the configured zone when the schedule is enabled.
     *
     * @param schedule the timed-window schedule (enabled flag, zone, start, end); see
     *                 {@link BusinessAiReplyBotSchedule}
     * @return the outcome reporting whether the window was updated, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code schedule} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> updateAiReplyBotEnabledTime(BusinessAiReplyBotSchedule schedule);

    /**
     * Updates which chats trigger this WhatsApp Business AI agent's automatic replies.
     *
     * @apiNote
     * Sets the assistant's "reply to" chat scope. The {@code triggerChatType} is a server-defined
     * marker kept as a {@link String} because its full value set is not recoverable from the WhatsApp
     * client.
     *
     * @param triggerChatType the chat set the assistant replies to, or {@code null} to omit it
     * @return the outcome reporting whether the scope was changed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> updateAiReplyChatTrigger(String triggerChatType);

    /**
     * Queries this WhatsApp Business AI agent's automatic-reply settings.
     *
     * @apiNote
     * Reads the assistant's current chat scope (which chats it replies to) and its daily active-hours
     * window for the authenticated business account.
     *
     * @return the automatic-reply settings, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiReplySettings> queryAiReplySettings();

    /**
     * Creates an auto-reply behaviour rule for this WhatsApp Business AI agent.
     *
     * @apiNote
     * Adds a rule that shapes how the assistant talks to customers (a free-form instruction, an
     * emoji-usage setting, or a price-sharing setting). The {@code request} describes the rule to
     * create and leaves its rule id unset. The returned rule is present only when the creation took
     * effect.
     *
     * @param request the rule to create, or {@code null} to omit it
     * @return the created rule, or {@link Optional#empty()} when the creation did not take effect or
     *         the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiRule> createAiRule(AiRuleInput request);

    /**
     * Updates an auto-reply behaviour rule of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Saves changes to a rule that shapes how the assistant talks to customers. The {@code request}
     * describes the rule to update and carries the rule id being updated. The returned rule reflects
     * its post-update state and is present only when the update took effect.
     *
     * @param request the rule to update, or {@code null} to omit it
     * @return the updated rule, or {@link Optional#empty()} when the update did not take effect or the
     *         server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiRule> updateAiRule(AiRuleInput request);

    /**
     * Deletes an auto-reply behaviour rule of this WhatsApp Business AI agent.
     *
     * @apiNote
     * Removes a rule that shaped how the assistant talks to customers. The {@code ruleId} is a
     * server-assigned rule identifier, not a WhatsApp address.
     *
     * @param ruleId the server-assigned identifier of the rule to delete, or {@code null} to omit it
     * @return the outcome reporting whether the rule was removed, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> deleteAiRule(String ruleId);

    /**
     * Generates suggested auto-reply behaviour rules for this WhatsApp Business AI agent.
     *
     * @apiNote
     * Asks the server to synthesize a fresh set of behaviour rules for the assistant, which the
     * merchant can review before adopting. The suggestions are not persisted until created.
     *
     * @return the suggested rules, empty when the server synthesized none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAiRule> generateAiRules();

    /**
     * Places a shopping-cart order on behalf of a buyer with a WhatsApp Business seller.
     *
     * @apiNote
     * Backs the cart checkout flow: the buyer confirms a cart of products and this places the order
     * with the seller, returning the placed order's currency and the subtotal and total amounts. The
     * direct-connection encrypted info is an opaque server-defined blob carried only for checkouts that
     * route through the seller's private connection.
     *
     * @param seller                        the business account the order is placed with, or
     *                                      {@code null} to omit it
     * @param products                      the ordered line items, or {@code null} to omit them
     * @param directConnectionEncryptedInfo the opaque direct-connection encrypted info, or {@code null}
     *                                      to omit it
     * @return the placed order, or {@link Optional#empty()} when the seller returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the seller reports an error
     */
    Optional<BusinessOrder> createBusinessOrder(JidProvider seller, List<BusinessOrderItem> products, String directConnectionEncryptedInfo);

    /**
     * Sends an order-status update to a customer as the merchant.
     *
     * @apiNote
     * Posts an order-status native-flow message carrying the order's items, totals, new lifecycle
     * status and payment status. The lifecycle status and payment status are independent fields; pass
     * {@link OrderPaymentStatus#fromLifecycle(OrderLifecycleStatus)} for the conventional default.
     * Commits the {@code SEND_ORDER_STATUS} order-management metric.
     *
     * @param chat          the customer chat
     * @param order         the order being updated, whose reference id, items and totals are serialized
     * @param status        the new order lifecycle status
     * @param paymentStatus the order payment status
     * @return the key of the sent message
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the order carries no reference id
     */
    MessageKey updateOrderStatus(JidProvider chat, BusinessOrder order, OrderLifecycleStatus status, OrderPaymentStatus paymentStatus);

    /**
     * Sends an order payment-status update (for example, marking an order paid or unpaid) to a
     * customer as the merchant.
     *
     * @apiNote
     * Posts a payment-status native-flow message carrying the order's items, totals, lifecycle status
     * and payment status. Commits the {@code SEND_MARK_AS_PAID} metric when the payment status is
     * {@link OrderPaymentStatus#CAPTURED}, otherwise {@code SEND_MARK_AS_UNPAID}.
     *
     * @param chat          the customer chat
     * @param order         the order being updated, whose reference id, items and totals are serialized
     * @param status        the order lifecycle status
     * @param paymentStatus the new payment status
     * @return the key of the sent message
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the order carries no reference id
     */
    MessageKey updateOrderPaymentStatus(JidProvider chat, BusinessOrder order, OrderLifecycleStatus status, OrderPaymentStatus paymentStatus);

    /**
     * Resolves a WhatsApp Business short-link slug to the account that owns it.
     *
     * @apiNote
     * Backs short-link resolution: given the trailing slug of a memorable business link (for example
     * the {@code yourshop} of {@code wa.me/yourshop}), this looks up the owning account and returns its
     * privacy-preserving alternate identifier. Use {@link #queryCustomUrlUserProfile(String)} when the
     * primary contact identifier is needed instead.
     *
     * @param path the short-link slug to resolve, or {@code null} to omit it
     * @return the resolution outcome, reporting whether the slug resolved and, on success, the owning
     *         account's identity; or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessCustomUrlIdentity> queryCustomUrlUser(String path);

    /**
     * Resolves a WhatsApp Business short-link slug to the account that owns it.
     *
     * @apiNote
     * Backs profile-card short-link resolution: like {@link #queryCustomUrlUser(String)} this looks up
     * the owning account from a memorable business link's slug, but returns the account's primary
     * contact identifier rather than its privacy-preserving alternate identifier.
     *
     * @param path the short-link slug to resolve, or {@code null} to omit it
     * @return the resolution outcome, reporting whether the slug resolved and, on success, the owning
     *         account's identity; or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessCustomUrlIdentity> queryCustomUrlUserProfile(String path);

    /**
     * Searches the industries a WhatsApp Business account can be classified under, returning a flat
     * list of matches.
     *
     * @apiNote
     * Backs the category picker in the Business profile editor: as the user types, this returns the
     * matching industries (for example "Restaurant" or "Retail") as a flat list, localised to the given
     * locale. Use {@link #queryBusinessProfileCategoryTree(String, String)} to browse the same
     * industries as a drill-down tree instead.
     *
     * @param query  the search prefix the user has typed, or {@code null} to omit it
     * @param locale the locale used to localise the display names, or {@code null} to omit it
     * @return the matching categories, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessCategory> queryBusinessProfileCategories(String query, String locale);

    /**
     * Browses the industries a WhatsApp Business account can be classified under, returning them as a
     * drill-down tree.
     *
     * @apiNote
     * Backs the same category picker as {@link #queryBusinessProfileCategories(String, String)} but
     * returns the matching industries as a nested tree, so an app can let the user drill from a broad
     * area (for example "Food and Beverage") down to a specific category (for example "Bakery").
     *
     * @param query  the search prefix the user has typed, or {@code null} to omit it
     * @param locale the locale used to localise the display names, or {@code null} to omit it
     * @return the top level of the matching category tree, or an empty list when the server returned
     *         none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessCategoryNode> queryBusinessProfileCategoryTree(String query, String locale);

    /**
     * Queries the pricing bands a WhatsApp Business account can choose when configuring a paid feature.
     *
     * @apiNote
     * Backs the price-tier picker shown when setting up a paid Business feature: the server returns the
     * bands available in the account's market, each with a description and currency symbol localised to
     * the given locale.
     *
     * @param locale the locale used to localise the descriptions and currency symbols, or {@code null}
     *               to omit it
     * @return the available pricing bands, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessPriceTier> queryBusinessPriceTiers(String locale);

    /**
     * Queries the websites advertised on a WhatsApp Business profile, each paired with the safe
     * redirect to open instead of the raw address.
     *
     * @apiNote
     * Backs the profile website affordance: the server returns each website declared on the business
     * profile alongside the vetting redirect URL an app should navigate to so WhatsApp can check the
     * destination first.
     *
     * @param biz the business account whose advertised websites are queried
     * @return the advertised website links, or an empty list when the server returned none
     * @throws NullPointerException           if {@code biz} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessWebsiteLink> queryBusinessProfileShimlinks(JidProvider biz);

    /**
     * Re-fetches a WhatsApp Business shopping cart using only the business account and its product ids.
     *
     * @apiNote
     * Convenience overload that calls {@link #refreshBusinessCart(BusinessCartRefreshOptions)} with no
     * image dimensions, no direct-connection info, and no variant-info projection.
     *
     * @param biz        the business account whose cart is refreshed
     * @param productIds the retailer product ids in the cart
     * @return the refreshed cart with its current product and price details, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code biz} or {@code productIds} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessRefreshedCart> refreshBusinessCart(JidProvider biz, List<String> productIds);

    /**
     * Re-fetches a WhatsApp Business shopping cart with its current product and price details.
     *
     * @apiNote
     * Backs the cart refresh path: given the cart's product ids, the server returns the cart with each
     * product's current availability, media, and price, plus the aggregate price breakdown.
     *
     * @param options the business account, cart product ids, image dimensions, direct-connection
     *                token, and variant-info selector; see {@link BusinessCartRefreshOptions}
     * @return the refreshed cart with its current product and price details, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code options} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessRefreshedCart> refreshBusinessCart(BusinessCartRefreshOptions options);

    /**
     * Suggests addresses as a user types a physical location for a WhatsApp Business profile.
     *
     * @apiNote
     * Backs the address picker in the Business profile editor: as the user types a partial address, the
     * server returns ranked place suggestions, each with coordinates and a structured postal address.
     *
     * @param query the autocomplete query parameters (see
     *              {@link BusinessAddressAutocompleteQuery})
     * @return the ranked address suggestions, or an empty list when the server returned none
     * @throws NullPointerException           if {@code query} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAddressSuggestion> queryBusinessAddressAutocomplete(BusinessAddressAutocompleteQuery query);

    /**
     * Suggests addresses as a user types a physical location for a WhatsApp Business profile.
     *
     * @apiNote
     * Convenience overload of
     * {@link #queryBusinessAddressAutocomplete(BusinessAddressAutocompleteQuery)} for the common case in
     * which only the partial address text is supplied and the map center and use-case scope are left
     * for the server to default.
     *
     * @param query the partial address text the user has typed, or {@code null} to omit it
     * @return the ranked address suggestions, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default List<BusinessAddressSuggestion> queryBusinessAddressAutocomplete(String query) {
        return queryBusinessAddressAutocomplete(new BusinessAddressAutocompleteQuery(null, query, null));
    }

    /**
     * Publishes the legal-entity compliance disclosure of a WhatsApp Business account.
     *
     * @apiNote
     * Backs the compliance editor's save action in markets that require a merchant compliance
     * disclosure: the merchant supplies entity and grievance-officer details and the server returns the
     * persisted disclosure.
     *
     * @param edit the typed merchant-compliance edit to persist
     * @return the persisted compliance disclosure, or {@link Optional#empty()} when the server returned
     *         no payload
     * @throws NullPointerException           if {@code edit} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessMerchantCompliance> setMerchantCompliance(MerchantComplianceEdit edit);

    /**
     * Queries the public key a WhatsApp Business account publishes for sending it catalog data securely.
     *
     * @apiNote
     * Backs catalog encryption over a merchant's private connection: the returned public key and its
     * signature are used to encrypt catalog payloads addressed to the business after confirming the key
     * was issued by WhatsApp.
     *
     * @param biz the business account whose catalog public key is queried
     * @return the published public key with its signature, or {@link Optional#empty()} when the server
     *         returned no payload
     * @throws NullPointerException           if {@code biz} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessCatalogPublicKey> queryCatalogPublicKey(JidProvider biz);

    /**
     * Reports a product in a WhatsApp Business catalog for review.
     *
     * @apiNote
     * Backs the "Report product" affordance shown to buyers viewing a catalog product: the buyer
     * selects a product and supplies a free-text reason, and this files the report.
     *
     * @param catalog   the business catalog of the reported product, or {@code null} to omit it
     * @param productId the reported product id, or {@code null} to omit it
     * @param reason    the free-text report reason, or {@code null} to omit it
     * @return {@code true} when the report was accepted, {@code false} when it was rejected or the
     *         server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    boolean reportCatalogProduct(JidProvider catalog, String productId, String reason);

    /**
     * Queries the billing information of a WhatsApp Business broadcast (paid marketing) account.
     *
     * @apiNote
     * Backs the billing panel of the broadcast composer: given a budget, the server returns the billable
     * account's payment mode, estimated taxes, payment-section details, and any required billing action.
     * The asset id is a Facebook billable-asset id, not a WhatsApp address.
     *
     * @param assetId    the billable-asset id, or {@code null} to omit it
     * @param budget     the budget for which estimated taxes and totals are computed, or {@code null} to
     *                   omit it
     * @param entrypoint the originating surface, or {@code null} to omit it
     * @return the billable account with its billing info, or {@link Optional#empty()} when the relay
     *         returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<BusinessBroadcastBillingAccount> queryBroadcastBillingInfo(String assetId, Long budget, String entrypoint);

    /**
     * Queries the linked-account business information of a WhatsApp Business broadcast account.
     *
     * @apiNote
     * Backs the broadcast onboarding step that resolves which Facebook business, ad account, page, and
     * payment account back the broadcast surface.
     *
     * @param shouldReturnAdAccount whether the resolved ad account is returned, or {@code null} to omit
     *                              the lookup input
     * @return the resolved linked-account business information, or {@link Optional#empty()} when the
     *         relay returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<BusinessBroadcastTargetInfo> queryBroadcastBusinessInfo(Boolean shouldReturnAdAccount);

    /**
     * Queries a generative-AI message recommendation for a WhatsApp Business broadcast.
     *
     * @apiNote
     * Backs the broadcast composer's "suggest message" action: given the draft and the recipient message
     * history, the server returns tone-tagged message suggestions and follow-up prompts.
     *
     * @param query the recommendation query parameters (see
     *              {@link BusinessBroadcastGenAiRecommendationQuery})
     * @return the recommendation carrying the suggested messages, or {@link Optional#empty()} when the
     *         relay returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<BusinessBroadcastGenAiRecommendation> queryBroadcastGenAiRecommendation(BusinessBroadcastGenAiRecommendationQuery query);

    /**
     * Queries a generative-AI message recommendation for a WhatsApp Business broadcast.
     *
     * @apiNote
     * Convenience overload of
     * {@link #queryBroadcastGenAiRecommendation(BusinessBroadcastGenAiRecommendationQuery)} for the
     * common case in which the operator only supplies the acting business identity and the draft.
     *
     * @param actorId          the WhatsApp Business identity acting on behalf of the merchant, or
     *                         {@code null} to omit it
     * @param userMessageDraft the draft the operator wrote, or {@code null} to omit it
     * @return the recommendation carrying the suggested messages, or {@link Optional#empty()} when the
     *         relay returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    default Optional<BusinessBroadcastGenAiRecommendation> queryBroadcastGenAiRecommendation(String actorId, String userMessageDraft) {
        return queryBroadcastGenAiRecommendation(new BusinessBroadcastGenAiRecommendationQuery(actorId, null, null, userMessageDraft, null));
    }

    /**
     * Queries the remaining broadcast (paid marketing) message quota of a WhatsApp Business account.
     *
     * @apiNote
     * Backs the quota indicator in the broadcast composer, reporting how many marketing messages the
     * account may still send.
     *
     * @param data the quota-lookup data carrying the marketing-message terms acknowledgement, or
     *             {@code null} to omit it
     * @return the remaining broadcast quota, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<BusinessBroadcastQuota> queryBroadcastQuota(BusinessBroadcastQuotaData data);

    /**
     * Toggles the AI auto-reply control state for a single WhatsApp Business chat thread.
     *
     * @apiNote
     * Backs the "AI replies" switch shown on a business chat: it tells the server whether the
     * connected AI agent should automatically reply to that consumer's incoming messages, or stay
     * muted. The consumer thread is identified by both its LID and its raw phone-number user part
     * because the server keys the per-thread AI state on both forms.
     *
     * @param consumerLid  the consumer side of the thread, addressed by its LID
     * @param phoneNumber  the consumer's raw phone-number user part
     * @param threadStatus the AI control state to apply
     * @return the mutation result reporting whether the change was applied, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code consumerLid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiMutationResult> setAiAutoReplyControl(JidProvider consumerLid, String phoneNumber, BusinessAiAutoReplyState threadStatus);

    /**
     * Creates a click-to-WhatsApp marketing-messages ad campaign for a WhatsApp Business broadcast.
     *
     * @apiNote
     * Drives the broadcast composer's "promote" flow: it spins up a Meta ad campaign that drives
     * recipients into a WhatsApp Business chat, funded by the given ad account and tied to the given
     * Facebook page and WhatsApp Business Account.
     *
     * @param campaign the campaign-creation parameters (see {@link BusinessMarketingCampaignCreate})
     * @return the created campaign carrying its identifiers and status, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessMarketingCampaign> createMarketingCampaign(BusinessMarketingCampaignCreate campaign);

    /**
     * Creates a click-to-WhatsApp marketing-messages ad campaign for a WhatsApp Business broadcast.
     *
     * @apiNote
     * Convenience overload of {@link #createMarketingCampaign(BusinessMarketingCampaignCreate)} for the
     * common case in which the merchant only supplies the funding ad account, the campaign name, and
     * the lifetime spending cap.
     *
     * @param adAccountId    the Meta ad-account id funding the campaign, or {@code null} to omit it
     * @param campaignName   the campaign name, or {@code null} to omit it
     * @param lifetimeBudget the campaign's lifetime spending cap, or {@code null} to omit it
     * @return the created campaign carrying its identifiers and status, or {@link Optional#empty()}
     *         when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default Optional<BusinessMarketingCampaign> createMarketingCampaign(String adAccountId, String campaignName, String lifetimeBudget) {
        return createMarketingCampaign(new BusinessMarketingCampaignCreate(adAccountId, campaignName, lifetimeBudget, null, null));
    }

    /**
     * Maps a batch of custom chat labels to their click-to-WhatsApp third-party conversion events.
     *
     * @apiNote
     * Backs click-to-WhatsApp conversion attribution: it resolves the merchant's custom chat labels
     * (for example "new order", "shipped") into the third-party conversion type and subtype the ad
     * platform records against them.
     *
     * @param customLabels the custom-label names to resolve, or {@code null} to omit them
     * @param exptGroup    the click-to-WhatsApp custom-label algorithm experiment group, or
     *                     {@code null} to omit it
     * @return the conversion events, one per matched label; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<CtwaConversionEvent> query3pdEventsByCustomLabels(List<String> customLabels, String exptGroup);

    /**
     * Queries which Click-to-WhatsApp ad entry points the authenticated session may surface.
     *
     * @apiNote
     * Reports which ad entry points and experiences the session is entitled to surface, gating where
     * the "promote" and other ad-creation surfaces may appear in the client. Each entitlement carries
     * only its show gate; for the localised copy to render on each entry point, use
     * {@link #queryClickToWhatsAppAdEntryPointsWithCopy()}.
     *
     * @return the entry-point entitlements; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<AdEntryPointEntitlement> queryClickToWhatsAppAdEntryPoints();

    /**
     * Queries the Click-to-WhatsApp ad entry points the session may surface, with their rendered copy.
     *
     * @apiNote
     * The richer counterpart of {@link #queryClickToWhatsAppAdEntryPoints()}: it carries the same
     * show gating plus the localised primary and secondary copy strings the client renders for each
     * entitled entry point.
     *
     * @return the entry-point entitlements with localised copy; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<AdEntryPointEntitlement> queryClickToWhatsAppAdEntryPointsWithCopy();

    /**
     * Queries the dynamic Meta AI mode catalogue offered in the AI mode selector.
     *
     * @apiNote
     * Backs the Meta AI mode picker: it returns the selectable AI experiences (each with its
     * localised title and subtitle) the user may switch between.
     *
     * @return the Meta AI modes; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<MetaAiMode> queryDynamicAiModes();

    /**
     * Queries the Meta AI search null-state suggestions shown before the user types a query.
     *
     * @apiNote
     * Backs the AI search box's empty state: it returns the suggested-query tiles to display before
     * the user has typed anything.
     *
     * @param locale          the requesting locale, or {@code null} to omit it
     * @param nullStateSource the surface that requested the suggestions, or {@code null} to omit it
     * @param expConfig       the experiment-bucket ids, or {@code null} to omit them
     * @return the suggestions carrying the suggestion tiles, or {@link Optional#empty()} when the
     *         server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<MetaAiSearchSuggestions> queryMetaAiSearchNullStateSuggestions(String locale, String nullStateSource, List<Integer> expConfig);

    /**
     * Queries the caller's OpenID Connect sign-in state for the WhatsApp Business platform.
     *
     * @apiNote
     * Returns the opaque OIDC state blob tied to the authenticated WhatsApp Business session, used by
     * the cross-surface sign-in handshake.
     *
     * @return the OIDC state blob, or {@link Optional#empty()} when the relay omitted it
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<String> queryOidcState();

    /**
     * Queries the subscription entry-point configuration eligible for the authenticated session.
     *
     * @apiNote
     * Backs the Meta Verified and other subscription upsell surfaces: it reports which subscription
     * entry-points the session may surface, each with its web eligibility flag and redirection URI.
     *
     * @return the subscription entry points, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessSubscriptionEntryPoints> querySubscriptionEntryPoints();

    /**
     * Queries the caller's WhatsApp subscriptions and their feature flags.
     *
     * @apiNote
     * Reports the account's active subscriptions (for example Meta Verified) and the per-feature
     * flags they unlock, with their limits and expiry.
     *
     * @param platform the requesting platform, or {@code null} to omit it
     * @return the subscriptions and their feature flags, or {@link Optional#empty()} when the server
     *         returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessSubscriptions> queryBusinessSubscriptions(String platform);

    /**
     * Queries the WhatsApp Flows metadata for a business flow.
     *
     * @apiNote
     * Backs rendering an interactive WhatsApp Flow with a business: it fetches the flow's metadata
     * and the signed endpoint public key used to verify the business's flow endpoint before opening
     * the encrypted flow exchange.
     *
     * @param bizJid the business account hosting the flow
     * @param flowId the flow whose metadata is fetched, or {@code null} to omit it
     * @return the flow metadata, or {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessFlowMetadata> queryFlowMetadata(JidProvider bizJid, String flowId);

    /**
     * Queries the Facebook pages promotable from a business account.
     *
     * @apiNote
     * Backs the page picker in the ad-creation flow: it lists the Facebook pages the given business
     * account may promote ads for.
     *
     * @param userId the Facebook user id (business profile id) of the business account, or
     *               {@code null} to omit it
     * @return the promotable Facebook pages; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<FacebookPage> queryPromotableFacebookPages(String userId);

    /**
     * Resolves the WhatsApp phone numbers and LIDs backing a set of marketing brand identifiers.
     *
     * @apiNote
     * Backs marketing-messages opt-out resolution: it maps the caller's brand identifiers to the
     * concrete phone numbers (or LIDs) the server addresses them under.
     *
     * @param brandIds         the marketing brand identifiers to resolve, or {@code null} to omit
     *                         them
     * @param lidBasedResponse whether the server returns LIDs instead of phone numbers, or
     *                         {@code null} to omit the flag
     * @return the per-brand resolutions, one per requested brand identifier; empty when the server
     *         returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BrandPhoneNumberMapping> queryPhoneNumbersForBrandIds(List<String> brandIds, Boolean lidBasedResponse);

    /**
     * Evaluates the caller's WhatsApp Ads eligibility for a click-to-WhatsApp advertising flow.
     *
     * @apiNote
     * Backs the ad-account eligibility gate of the WhatsApp Ads onboarding flow: it returns whether
     * the caller may proceed into the given advertising flow, with any verdict other than a hard
     * deny treated as eligible.
     *
     * @param flowId    the advertising flow being evaluated, or {@code null} to omit it
     * @param requestId the de-duplication timestamp for this check, or {@code null} to omit it
     * @return the parsed eligibility verdict, or {@link Optional#empty()} when the relay returned
     *         no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<WhatsAppAdsEligibility> queryWaaEligibility(String flowId, Instant requestId);

    /**
     * Verifies a postcode against a WhatsApp Business direct-connection catalogue.
     *
     * @apiNote
     * Backs the postcode-gated catalogue check: it confirms whether the given postcode is serviceable
     * by the business's direct-connection catalogue session.
     *
     * @param bizJid                        the business account whose catalogue is checked
     * @param directConnectionEncryptedInfo the direct-connection cypher token tying the postcode to
     *                                      the catalogue session, or {@code null} to omit it
     * @return the parsed postcode verification, or {@link Optional#empty()} when the relay returned
     *         no payload
     * @throws NullPointerException           if {@code bizJid} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<BusinessPostcodeVerification> verifyCatalogPostcode(JidProvider bizJid, String directConnectionEncryptedInfo);

    /**
     * Queries the download manifest for a batch of on-device machine-learning models.
     *
     * @apiNote
     * Backs on-device ML model provisioning: it resolves the requested model names and versions into
     * the list of files the client must download, given the client's decode capabilities.
     *
     * @param modelRequestMetadatas    the models to resolve, or an empty list to omit the variable
     * @param clientCapabilityMetadata the client decode capabilities, or {@code null} to omit the
     *                                 variable
     * @return the parsed model manifest, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<NativeMachineLearningModelManifest> queryNativeMlModelManifest(List<ModelRequestMetadata> modelRequestMetadatas, ClientCapabilityMetadata clientCapabilityMetadata);

    /**
     * Clears the caller's cached account-type preference for the WhatsApp Ads sign-in flow.
     *
     * @apiNote
     * Backs the "switch account type" action in the WhatsApp Ads sign-in flow: it drops the cached
     * preference so the next sign-in re-prompts for the account type and Facebook ad page.
     *
     * @return the parsed reset acknowledgement, or {@link Optional#empty()} when the relay returned
     *         no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<WhatsAppAdsAccountTypeReset> queryAccountTypeAndAdPage();

    /**
     * Resolves a Facebook page's per-page ad-creation eligibility for a WhatsApp Ads advertising
     * flow.
     *
     * @apiNote
     * Backs the page-eligibility gate of the WhatsApp Ads ad-creation flow: it reports whether the
     * viewer may create ads for the named Facebook page.
     *
     * @param pageId the Facebook page identifier to resolve, or {@code null} to omit it
     * @return the parsed page eligibility, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<WhatsAppAdsPageEligibility> queryPageEligibility(String pageId);

    /**
     * Submits a WhatsApp support bug report.
     *
     * @apiNote
     * Backs the in-app "report a problem" flow: it forwards the bug-report payload to support and
     * returns the assigned report and task identifiers.
     *
     * @param request the typed bug-report submission request (see
     *                {@link SupportBugReportSubmissionRequest})
     * @return the parsed submission outcome, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws NullPointerException           if {@code request} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<SupportBugReportSubmission> submitSupportBugReport(SupportBugReportSubmissionRequest request);

    /**
     * Submits a WhatsApp support bug report.
     *
     * @apiNote
     * Convenience overload of
     * {@link #submitSupportBugReport(SupportBugReportSubmissionRequest)} for the common case in which
     * only the category and free-text description are supplied and no additional context is attached.
     *
     * @param category    the server-defined bug category code, or {@code null} to omit it
     * @param description the free-text bug description, or {@code null} to omit it
     * @return the parsed submission outcome, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    default Optional<SupportBugReportSubmission> submitSupportBugReport(String category, String description) {
        return submitSupportBugReport(new SupportBugReportSubmissionRequest(category, description, null,
                null, null, null, List.of()));
    }

    /**
     * Submits feedback on a WhatsApp support-assistant message.
     *
     * @apiNote
     * Backs the thumbs-up/thumbs-down controls on a support-assistant reply: it records the rating
     * (and any negative-feedback reasons) against the rated message.
     *
     * @param messageId     the stanza id of the assistant message being rated, or {@code null} to
     *                      omit it
     * @param feedbackTypes the feedback kinds to report, or {@code null} to omit them
     * @return the parsed submission outcome, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<SupportMessageFeedbackSubmission> submitSupportMessageFeedback(String messageId, List<SupportMessageFeedbackKind> feedbackTypes);

    /**
     * Creates or onboards a WhatsApp Ads ad account for a click-to-WhatsApp advertising flow.
     *
     * @apiNote
     * Backs the ad-account onboarding step of the WhatsApp Ads advertising flow: it provisions (or
     * resumes) the caller's ad account and returns its identifier and onboarding status.
     *
     * @param flowId    the advertising flow being onboarded, or {@code null} to omit it
     * @param requestId the de-duplication timestamp for this request, or {@code null} to omit it
     * @return the parsed ad account, or {@link Optional#empty()} when the relay returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<WhatsAppAdsAdAccount> onboardWaaAccount(String flowId, Instant requestId);

    /**
     * Queries the caller's Meta cross-posting and account-linking service data.
     *
     * @apiNote
     * Backs the cross-posting and Facebook/Instagram account-linking panels: it returns the linked
     * destination accounts, the additional-feature-set eligibility, and the per-destination
     * link-eligibility flags for the session.
     *
     * @return the parsed service-data view, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<CrossPostingServiceData> queryCrossPostingServiceData();

    /**
     * Propagates the caller's Global Privacy Control opt-out into the linked Meta advertising
     * surfaces.
     *
     * @apiNote
     * Backs the Global Privacy Control opt-out toggle: it pushes the caller's universal opt-out
     * state into the linked Meta advertising surfaces and reports whether the propagation
     * succeeded; the WhatsApp client marks the user preference completed only on a successful
     * propagation.
     *
     * @return {@code true} when the propagation succeeded, {@code false} when the relay reported a
     *         failed propagation or returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    boolean updateGlobalPrivacyControlOptOut();

    /**
     * Checks the cross-posting eligibility of a set of WhatsApp statuses against the targeted Meta
     * destinations.
     *
     * @apiNote
     * Backs status cross-posting to Facebook and Instagram: it checks whether the given statuses
     * may be cross-posted to the targeted destinations and returns the per-purpose public keys, the
     * per-status resolved destination identities, and the per-destination eligibility parameters
     * the WhatsApp client folds into its per-status state map.
     *
     * @param query the eligibility query parameters (see {@link CrossPostingEligibilityQuery})
     * @return the parsed eligibility tree, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<CrossPostingEligibility> checkCrossPostingEligibility(CrossPostingEligibilityQuery query);

    /**
     * Requests a single-use business account nonce from the relay.
     *
     * @apiNote
     * Issues a proof-of-possession nonce for the WhatsApp-to-Facebook business account-linking flow
     * over the WhatsApp Web GraphQL transport; the issued nonce is echoed back to the Meta-side surface. This is
     * the relay GraphQL counterpart of the stanza-based {@link #queryAccountNonce(String)}.
     *
     * @param scope the nonce scope, or {@code null} to omit it
     * @return the parsed account nonce, or {@link Optional#empty()} when the relay returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<BusinessAccountNonce> queryBusinessAccountNonce(String scope);

    /**
     * Queries the Facebook and WhatsApp ad-identity accounts linked to the authenticated session
     * over the WhatsApp Web GraphQL transport.
     *
     * @apiNote
     * Surfaces the linked Facebook page and WhatsApp ad identity (with their click-to-WhatsApp ad
     * status) tied to the session. This is the relay GraphQL counterpart of the stanza-based
     * {@link #queryLinkedAccounts()}.
     *
     * @return the parsed linked-accounts view, or {@link Optional#empty()} when the relay returned
     *         no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the relay reports a GraphQL
     *                                        error
     */
    Optional<BusinessLinkedAdAccounts> queryRelayLinkedAccounts();

    /**
     * Queries the set of WhatsApp features the server has disabled for a session acting on behalf of
     * an authorized business agent.
     *
     * @apiNote
     * Backs the delegated-agent UI gate: an empty result signals the session is not a delegated
     * agent, a present policy lists the feature names the UI must hide.
     *
     * @return the parsed policy, or {@link Optional#empty()} when the server reported no policy
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AuthorizedAgentFeaturePolicy> queryAgentFeaturePolicy();

    /**
     * Redeems a Business Platform authorization code for the durable access token and browser
     * session cookies the Business Platform surfaces expect.
     *
     * @apiNote
     * Backs the Business Platform onboarding flow that exchanges the one-time authorization code for
     * a durable credential bundle; the redeemed code is treated as redaction-sensitive on the wire.
     *
     * @param applicationId the Meta application identifier the access token is minted for
     * @param code          the one-time authorization code to redeem, or {@code null} to omit it
     * @return the minted credential bundle, or {@link Optional#empty()} when the server returned no
     *         credentials
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessPlatformAuthToken> exchangeBusinessPlatformAuthCode(long applicationId, String code);

    /**
     * Queries whether the authenticated web session resolves to a valid WhatsApp user.
     *
     * @apiNote
     * Backs the post-login validity check: flows that assume a fully provisioned account first
     * confirm the session cookie still maps to a usable WhatsApp user.
     *
     * @return {@code true} when the server confirmed the session resolves to a valid user,
     *         {@code false} when it did not or returned no verdict
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    boolean queryWebSessionUserValidity();

    /**
     * Queries the activity status of the WhatsApp GenAI agent channel attached to the caller.
     *
     * @apiNote
     * Backs the channel-header live status indicator: it returns the activity descriptor (machine
     * code plus human-readable text) the channel UI renders next to the agent's name.
     *
     * @return the parsed activity status, or {@link Optional#empty()} when the channel reported no
     *         status
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AiChannelAgentStatus> queryAiChannelAgentStatus();

    /**
     * Queries the slash-command catalog advertised by the WhatsApp GenAI agent channel attached to
     * the caller.
     *
     * @apiNote
     * Backs the agent slash-command picker: each entry exposes the invoked name, the description
     * shown in the picker, and the prompt template the command expands into.
     *
     * @return the advertised commands; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<AiChannelCommand> queryAiChannelCommands();

    /**
     * Queries the public identity of the WhatsApp GenAI agent channel attached to the caller.
     *
     * @apiNote
     * Backs the channel header: it returns the agent display name and avatar image followers see at
     * the top of the channel.
     *
     * @return the parsed identity, or {@link Optional#empty()} when the server returned no identity
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AiChannelIdentity> queryAiChannelIdentity();

    /**
     * Queries the pairing state between the caller's session and the WhatsApp GenAI agent channel.
     *
     * @apiNote
     * Backs the agent-channel controls gate: it reports whether the session is bound to a channel,
     * whether pairing is complete, the channel lifecycle status, and the Facebook-side channel
     * identifier the management deep-link targets.
     *
     * @return the parsed linked status, or {@link Optional#empty()} when the server returned no
     *         status
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AiChannelLinkedStatus> queryAiChannelLinkedStatus();

    /**
     * Authorizes a chat opened on behalf of a third-party partner via an external deep-link.
     *
     * @apiNote
     * Backs the deep-link confirmation sheet shown when a third-party app or web surface opens a
     * WhatsApp chat with one of its customers; the returned partner name is the copy rendered in
     * that sheet.
     *
     * @param options the authorisation parameters (see
     *                {@link ExternalChatDeepLinkAuthorizationOptions})
     * @return the parsed authorisation, or {@link Optional#empty()} when the server returned no
     *         verdict
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<ExternalChatDeepLinkAuthorization> authorizeExternalChatDeepLink(ExternalChatDeepLinkAuthorizationOptions options);

    /**
     * Authorizes a chat opened on behalf of a third-party partner via an external deep-link.
     *
     * @apiNote
     * Convenience overload of
     * {@link #authorizeExternalChatDeepLink(ExternalChatDeepLinkAuthorizationOptions)} for the common
     * case in which only the chat being opened and the deep-link kind are known.
     *
     * @param recipient    the chat being opened, or {@code null} to omit it
     * @param deeplinkType the deep-link type that opened the chat, or {@code null} to omit it
     * @return the parsed authorisation, or {@link Optional#empty()} when the server returned no
     *         verdict
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default Optional<ExternalChatDeepLinkAuthorization> authorizeExternalChatDeepLink(JidProvider recipient, String deeplinkType) {
        return authorizeExternalChatDeepLink(new ExternalChatDeepLinkAuthorizationOptions(recipient, deeplinkType, null, null));
    }

    /**
     * Redeems an OpenID Connect authorization code for the Facebook access token used by the
     * Click-to-WhatsApp advertising flow.
     *
     * @apiNote
     * Backs the Facebook sign-in step of the Click-to-WhatsApp advertising flow: it mints the
     * Facebook access token from the OIDC code and anti-forgery nonce returned by the identity
     * provider.
     *
     * @param code  the OpenID Connect authorization code, or {@code null} to omit it
     * @param state the OpenID Connect anti-forgery nonce, or {@code null} to omit it
     * @return the minted Facebook credential pair, or {@link Optional#empty()} when the server
     *         returned no credentials
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<FacebookOidcAccessToken> exchangeOidcCodeForAccessToken(String code, String state);

    /**
     * Queries the onboarding metadata rendered during a WhatsApp Business signup flow.
     *
     * @apiNote
     * Backs the signup consent screen: it returns the consent copy and the privacy-policy link the
     * screen renders for the in-progress signup. The phone number is the raw signup string, not a
     * WhatsApp address.
     *
     * @param signupId    the in-progress signup identifier, or {@code null} to omit it
     * @param phoneNumber the phone number the signup is attached to, or {@code null} to omit it
     * @return the parsed signup metadata, or {@link Optional#empty()} when the server returned no
     *         metadata
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessSignupMetadata> querySignupMetadata(String signupId, String phoneNumber);

    /**
     * Provisions the advertising-platform page tied to a WhatsApp Ads identity.
     *
     * @apiNote
     * Backs the WhatsApp Ads identity setup that either provisions a new advertising-platform page
     * for the advertiser's phone number or resumes the existing one. The phone number and account
     * nonce are treated as redaction-sensitive on the wire.
     *
     * @param phoneNumber the advertiser's phone number in national format, or {@code null} to omit
     *                    it
     * @param code        the account-nonce verification token, or {@code null} to omit it
     * @return the provisioned advertising page, or {@link Optional#empty()} when the server
     *         returned no page
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<WhatsAppAdsIdentityPage> createWhatsAppAdsIdentity(String phoneNumber, String code);

    /**
     * Registers already-uploaded WhatsApp media against a Click-to-WhatsApp native ad so it can run
     * on the status surface.
     *
     * @apiNote
     * Backs the status-surface attachment step of the native-ad creation flow: callers pass the
     * advertising-platform media identifiers and their image-or-video kind, and the server confirms
     * one entry per registered medium.
     *
     * @param mediaList the media entries to register, or {@code null} to omit them
     * @return the registered media entries; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<AdMediaRegistration> linkMediaToNativeAd(List<AdMediaLink> mediaList);

    /**
     * Uploads Click-to-WhatsApp ad media into Meta's advertising-platform media store.
     *
     * @apiNote
     * Backs the ad-media upload step of the WhatsApp Business advertising flow. The Facebook access
     * token is treated as redaction-sensitive on the wire.
     *
     * @param options the upload parameters (see {@link AdMediaUploadOptions})
     * @return the uploaded media descriptors; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<AdMediaUpload> uploadAdMedia(AdMediaUploadOptions options);

    /**
     * Uploads Click-to-WhatsApp ad media into Meta's advertising-platform media store.
     *
     * @apiNote
     * Convenience overload of {@link #uploadAdMedia(AdMediaUploadOptions)} for the common case in
     * which the merchant has only the funding ad account and the media identifiers ready.
     *
     * @param adAccountId the advertising-platform ad-account identifier, or {@code null} to omit it
     * @param mediaIds    the advertising-platform media identifiers to upload, or {@code null} to omit
     *                    them
     * @return the uploaded media descriptors; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default List<AdMediaUpload> uploadAdMedia(String adAccountId, List<String> mediaIds) {
        return uploadAdMedia(new AdMediaUploadOptions(adAccountId, null, mediaIds, null));
    }

    /**
     * Queries Meta AI search type-ahead suggestions for a partial query prefix.
     *
     * @apiNote
     * Backs the in-progress Meta AI search box: it returns the ranked suggestion tiles for the
     * given query prefix, scoped to the requesting locale, experiment buckets, and supported
     * suggestion modalities.
     *
     * @param query the type-ahead query parameters (see {@link MetaAiSearchTypeAheadQuery})
     * @return the parsed suggestions bundle, or {@link Optional#empty()} when the server returned
     *         no suggestions
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<MetaAiSearchSuggestions> queryMetaAiSearchTypeAheadSuggestions(MetaAiSearchTypeAheadQuery query);

    /**
     * Queries Meta AI search type-ahead suggestions for a partial query prefix.
     *
     * @apiNote
     * Convenience overload of {@link #queryMetaAiSearchTypeAheadSuggestions(MetaAiSearchTypeAheadQuery)}
     * for the common case in which the caller only supplies the partial prefix and the locale.
     *
     * @param query  the partial search prefix, or {@code null} to omit it
     * @param locale the requesting locale, or {@code null} to omit it
     * @return the parsed suggestions bundle, or {@link Optional#empty()} when the server returned
     *         no suggestions
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default Optional<MetaAiSearchSuggestions> queryMetaAiSearchTypeAheadSuggestions(String query, String locale) {
        return queryMetaAiSearchTypeAheadSuggestions(new MetaAiSearchTypeAheadQuery(query, locale, null, null));
    }

    /**
     * Queries the caller's eligibility for the WhatsApp Business native-ads creation entry point.
     *
     * @apiNote
     * Backs the gating of the "create native ad" entry point. The phone number is the advertiser's
     * national-format phone number, not a WhatsApp address.
     *
     * @param phoneNumber the advertiser's phone number in national format, or {@code null} to omit
     *                    it
     * @return the parsed eligibility view, or {@link Optional#empty()} when the server returned no
     *         eligibility info
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<NativeAdsEligibility> queryNativeAdsEligibility(String phoneNumber);

    /**
     * Queries the WhatsApp Business quick-promotion banners eligible to show on a set of surfaces.
     *
     * @apiNote
     * Backs the in-app promotional banners shown on WhatsApp Business surfaces: it asks the server,
     * for each given surface identifier, which promotional banners the user is currently eligible
     * to see; the trigger context carries the client signals the eligibility engine matches against
     * (Business-build flag, client version, country, locale).
     *
     * @param surfaceIds     the surface identifiers to evaluate eligibility for, or {@code null} to
     *                       omit them
     * @param triggerContext the client trigger context, or {@code null} to omit it
     * @return one batch per surface listing the eligible banners; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<QuickPromotionSurfaceBatch> queryQuickPromotions(List<String> surfaceIds, QuickPromotionTriggerContext triggerContext);

    /**
     * Queries the consumer WhatsApp quick-promotion banners eligible to show on a set of surfaces.
     *
     * @apiNote
     * Backs the in-app promotional banners shown on consumer WhatsApp surfaces: it asks the server,
     * for each given surface identifier, which promotional banners the user is currently eligible
     * to see; the trigger context carries the client signals the eligibility engine matches against.
     *
     * @param surfaceIds     the surface identifiers to evaluate eligibility for, or {@code null} to
     *                       omit them
     * @param triggerContext the client trigger context, or {@code null} to omit it
     * @return one batch per surface listing the eligible banners; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<QuickPromotionSurfaceBatch> queryConsumerQuickPromotions(List<String> surfaceIds, QuickPromotionTriggerContext triggerContext);

    /**
     * Logs an interaction with a WhatsApp Business quick-promotion banner.
     *
     * @apiNote
     * Reports an impression, dismiss, or primary tap on a Business quick-promotion banner so the
     * pacing engine can decide whether to keep showing the same banner.
     *
     * @param log the interaction record (see {@link QuickPromotionActionLog})
     * @return the server acknowledgement, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<QuickPromotionLogAcknowledgement> logPromotionAction(QuickPromotionActionLog log);

    /**
     * Logs an interaction with a consumer WhatsApp quick-promotion banner.
     *
     * @apiNote
     * Reports an impression, dismiss, or primary tap on a consumer quick-promotion banner so the
     * pacing engine can decide whether to keep showing the same banner.
     *
     * @param log the interaction record (see {@link QuickPromotionActionLog})
     * @return the server acknowledgement, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<QuickPromotionLogAcknowledgement> logConsumerPromotionAction(QuickPromotionActionLog log);

    /**
     * Queries the WhatsApp anonymous-credential service configuration for a named project.
     *
     * @apiNote
     * Fetches the per-project parameters the client needs before blinding tokens for the
     * anonymous-credential service: the cipher suite, public key, evaluation and redemption limits
     * per token, the configuration expiry, and the token lifetime.
     *
     * @param projectName the credential-service project name, or {@code null} to omit it
     * @return the parsed configuration, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AnonymousCredentialServiceConfig> queryAnonymousCredentialServiceConfig(String projectName);

    /**
     * Requests a batch issuance of WhatsApp anonymous credentials.
     *
     * @apiNote
     * Submits a batch of blinded tokens against the requested credential-service project and
     * configuration; on success the server returns one signed evaluation and one DLEQ proof per
     * token, which the client unblinds into usable anonymous tokens.
     *
     * @param request the issuance request parameters (see
     *                {@link AnonymousCredentialIssuanceRequest})
     * @return the parsed issuance outcome, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AnonymousCredentialIssuance> issueAnonymousCredentials(AnonymousCredentialIssuanceRequest request);

    /**
     * Requests a batch issuance of WhatsApp anonymous credentials.
     *
     * @apiNote
     * Convenience overload of {@link #issueAnonymousCredentials(AnonymousCredentialIssuanceRequest)}
     * for the common case in which the caller only supplies the project, the configuration, and the
     * blinded tokens.
     *
     * @param projectName     the credential-service project name, or {@code null} to omit it
     * @param configurationId the configuration the tokens were blinded against, or {@code null} to
     *                        omit it
     * @param blindedTokens   the base64url-encoded blinded tokens to issue, or {@code null} to omit
     *                        them
     * @return the parsed issuance outcome, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default Optional<AnonymousCredentialIssuance> issueAnonymousCredentials(String projectName, String configurationId, List<String> blindedTokens) {
        return issueAnonymousCredentials(new AnonymousCredentialIssuanceRequest(projectName, configurationId, blindedTokens, null));
    }

    /**
     * Queries the AI bot persona profiles for a batch of persona identifiers.
     *
     * @apiNote
     * Returns the full persona profile (display name, description, professional-status marker, and
     * creator) for each bot persona identifier; distinct from
     * {@link #queryBotProfile(JidProvider)}, which fetches a single bot's profile by its address.
     *
     * @param personaIds the bot persona identifiers to fetch, or {@code null} to omit them
     * @return the parsed bot profiles; empty when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BotProfile> queryBotProfiles(List<String> personaIds);

    /**
     * Submits a free-form WhatsApp support contact form.
     *
     * @apiNote
     * Files a support request with a short free-text description, an opaque diagnostic bundle
     * collected by the client, and the originating support flow; on success the server returns the
     * assigned ticket identifier and the WhatsApp support phone number the user can message about
     * the ticket.
     *
     * @param description     the free-text issue description, or {@code null} to omit it
     * @param diagnosticsJson the JSON-encoded diagnostic bundle collected by the client, or
     *                        {@code null} to omit it
     * @param contextFlow     the originating support flow (for example
     *                        {@link SupportContactFormContextFlow#GENERAL}), or {@code null} to omit it
     * @return the parsed submission outcome, or {@link Optional#empty()} when the server returned
     *         no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<SupportContactFormSubmission> submitSupportContactForm(String description, String diagnosticsJson, SupportContactFormContextFlow contextFlow);

    /**
     * Files an appeal against the suspension of a WhatsApp group.
     *
     * @apiNote
     * Submits an appeal for a suspended group with an optional free-text reason and an opaque
     * client-debug bundle; the server reports a verdict (whether the appeal was accepted or
     * rejected) and the instant at which the appeal was filed.
     *
     * @param groupJid          the suspended group, or {@code null} to omit it
     * @param appealReason      the free-text appeal reason, or {@code null} to omit it
     * @param clientDebugBundle the JSON-encoded client-debug bundle, or {@code null} to omit it
     * @return the parsed appeal outcome, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<GroupSuspensionAppeal> appealGroupSuspension(JidProvider groupJid, String appealReason, String clientDebugBundle);

    /**
     * Creates a draft for a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Click-to-WhatsApp ads are paid promotions that open a chat with the business when tapped. Before
     * publishing one, the merchant assembles it as a draft that can be edited and is charged for only
     * once published. This saves a new draft and returns it so its identifier can drive later edits and
     * publishing.
     *
     * @param input the boosted-component draft contents, or {@code null} to omit them
     * @return the created draft, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdDraft> createAdDraft(LwiBoostedComponentInput input);

    /**
     * Edits the draft of a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Changes the contents of an unpublished Click-to-WhatsApp ad draft (a paid promotion that opens a
     * chat with the business when tapped) and returns the updated draft.
     *
     * @param input the changed boosted-component draft contents, or {@code null} to omit them
     * @return the edited draft, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdDraft> editAdDraft(LwiBoostedComponentInput input);

    /**
     * Discards the draft of a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Removes an unpublished Click-to-WhatsApp ad draft (a paid promotion that opens a chat with the
     * business when tapped) and reports whether the deletion took effect through
     * {@link BusinessAdMutationResult#success()}.
     *
     * @param draftId the opaque identifier of the draft to delete, as returned by
     *                {@link #createAdDraft(LwiBoostedComponentInput)}, or {@code null} to omit it
     * @return the deletion outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> deleteAdDraft(String draftId);

    /**
     * Deletes a live Click-to-WhatsApp ad.
     *
     * @apiNote
     * Removes a running Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when
     * tapped) and reports whether the deletion took effect through
     * {@link BusinessAdMutationResult#success()}.
     *
     * @param boostId the identifier of the live ad to delete
     * @return the deletion outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> deleteAd(String boostId);

    /**
     * Pauses a running Click-to-WhatsApp ad.
     *
     * @apiNote
     * Temporarily stops delivery of a live Click-to-WhatsApp ad (a paid promotion that opens a chat
     * with the business when tapped) and reports whether the change took effect through
     * {@link BusinessAdMutationResult#success()}; the affected ad's identifier is echoed in
     * {@link BusinessAdMutationResult#affectedIds()}. The ad can later be restarted with
     * {@link #resumeAd(String)}.
     *
     * @param boostId the identifier of the ad to pause
     * @return the pause outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> pauseAd(String boostId);

    /**
     * Resumes a paused Click-to-WhatsApp ad.
     *
     * @apiNote
     * Restarts delivery of a previously paused Click-to-WhatsApp ad (a paid promotion that opens a chat
     * with the business when tapped) and reports whether the change took effect through
     * {@link BusinessAdMutationResult#success()}; the affected ad's identifier is echoed in
     * {@link BusinessAdMutationResult#affectedIds()}.
     *
     * @param boostId the identifier of the ad to resume
     * @return the resume outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> resumeAd(String boostId);

    /**
     * Attests the advertiser's identity for a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Ads in regulated categories require the advertiser to certify their identity before they can run.
     * This records that certification and reports whether it took effect through
     * {@link BusinessAdMutationResult#success()}; the name recorded against the certification is echoed
     * in {@link BusinessAdMutationResult#affectedIds()}. Whether certification is required can be
     * checked first with {@link #queryAdCertificationRequired()}.
     *
     * @param source the surface the certification was made from, or {@code null} to omit it
     * @return the certification outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> certifyAd(String source);

    /**
     * Publishes a Click-to-WhatsApp ad from an assembled draft.
     *
     * @apiNote
     * Confirms an assembled draft and brings the Click-to-WhatsApp ad (a paid promotion that opens a
     * chat with the business when tapped) live, returning the running ad with its identifier, budget,
     * duration, audience, and creatives.
     *
     * @param input the boosted-component ad specification, or {@code null} to omit it
     * @return the published ad, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessBoostedComponent> createBoostedComponent(LwiBoostedComponentInput input);

    /**
     * Queries the advertising account that funds a merchant's Click-to-WhatsApp ads.
     *
     * @apiNote
     * Resolves the advertising account that ad spend is billed to: its display name, the currency it
     * bills in, and the linked payment method, so the merchant can confirm which account and currency
     * an ad will use before committing a budget.
     *
     * @param adAccountId the advertising-account identifier to resolve
     * @return the resolved advertising account, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdAccount> queryAdAccountDetails(String adAccountId);

    /**
     * Queries the budget choices offered for a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Returns the pre-computed spend amounts the merchant can pick from and the lowest daily spend the
     * server allows, so the budget for a Click-to-WhatsApp ad (a paid promotion that opens a chat with
     * the business when tapped) can be chosen without being set too low to deliver.
     *
     * @param legacyAdAccountId the funding ad-account identifier, or {@code null} to omit it
     * @param budget            the budget amount in minor units, or {@code null} to omit it
     * @param currency          the currency code, or {@code null} to omit it
     * @return the offered budget choices, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdBudgetOptions> queryAdBudgetOptions(String legacyAdAccountId, Long budget, String currency);

    /**
     * Queries the payment state for a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Resolves the current payment method shown to the merchant and any billing-setup step that must be
     * completed before paying for a Click-to-WhatsApp ad (a paid promotion that opens a chat with the
     * business when tapped). The merchant may proceed to pay when
     * {@link BusinessAdPaymentSection#requiredSetup()} is empty.
     *
     * @param assetId the identifier of the asset the payment state is resolved for
     * @param budget  the selected budget, or {@code null} to omit it
     * @return the resolved payment state, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdPaymentSection> queryAdPaymentSection(String assetId, Long budget);

    /**
     * Sends the payment reminder for a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Triggers the server-side notification that prompts the merchant to complete payment setup for a
     * Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when tapped) and
     * reports whether the reminder was sent through {@link BusinessAdMutationResult#success()}.
     *
     * @return the send outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> sendAdPaymentNotification();

    /**
     * Queries whether billing setup is required before paying for a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Reports any billing-setup step the advertising account must complete before it can be charged for
     * a Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when tapped). A
     * non-empty {@link BusinessAdPaymentSection#requiredSetup()} means setup is outstanding; an empty
     * one means the merchant may pay directly. Unlike {@link #queryAdPaymentSection(String, Long)},
     * this carries only the setup step, not the descriptive payment row.
     *
     * @param assetId the identifier of the asset the billing state is resolved for
     * @param budget  the selected budget, or {@code null} to omit it
     * @return the resolved payment state carrying any outstanding setup step, or
     *         {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdPaymentSection> queryAdBillingSetupRequired(String assetId, Long budget);

    /**
     * Queries the advertiser-certification state for the current account.
     *
     * @apiNote
     * Reads the account's current advertiser-certification state so a caller can decide whether the
     * advertiser must complete certification (via {@link #certifyAd(String)}) before running a
     * Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when tapped). The
     * returned value is a server-defined state marker.
     *
     * @return the certification state marker, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<String> queryAdCertificationRequired();

    /**
     * Queries whether a server-side feature flag is enabled for an advertising account.
     *
     * @apiNote
     * Resolves a boolean feature flag that gates parts of the advertising surface for a given
     * advertising account, optionally recording that the flag was checked. The flag value and the
     * checking semantics are server-defined; the flag is treated as disabled when the server returns no
     * value.
     *
     * @param accountId            the advertising-account identifier being probed, or {@code null} to
     *                             omit it
     * @param defaultValue         the fallback value when the flag does not resolve, or {@code null} to
     *                             omit it
     * @param checkWithMultipleAdAccounts whether to record the check with multiple advertising
     *                             accounts, or {@code null} to omit it
     * @param recordCheck          whether the server records that the flag was checked, or {@code null}
     *                             to omit it
     * @param flagName             the name of the boolean flag, or {@code null} to omit it
     * @param shouldFetch          whether the server should resolve and return the flag, or
     *                             {@code null} to omit it
     * @param flagGroupName        the name of the flag group the flag belongs to, or {@code null} to
     *                             omit it
     * @return {@code true} when the flag resolved to enabled, {@code false} otherwise
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    boolean queryAdAccountFeatureFlag(String accountId, Boolean defaultValue, Boolean checkWithMultipleAdAccounts, Boolean recordCheck, String flagName, Boolean shouldFetch, String flagGroupName);

    /**
     * Confirms the contact email for a Click-to-WhatsApp advertising account.
     *
     * @apiNote
     * Submits the merchant's onboarding details to confirm the contact email tied to a
     * Click-to-WhatsApp advertising account and reports whether the change took effect through
     * {@link BusinessAdMutationResult#success()}; the confirmed email is echoed in
     * {@link BusinessAdMutationResult#affectedIds()} and any rejection reason in
     * {@link BusinessAdMutationResult#errorMessage()}.
     *
     * @param confirmation the typed email-onboarding confirmation (see
     *                     {@link BusinessAdEmailOnboardingConfirmation})
     * @return the confirmation outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code confirmation} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> confirmAdEmailOnboarding(BusinessAdEmailOnboardingConfirmation confirmation);

    /**
     * Confirms the contact email for a Click-to-WhatsApp advertising account.
     *
     * @apiNote
     * Convenience overload of
     * {@link #confirmAdEmailOnboarding(BusinessAdEmailOnboardingConfirmation)} that takes the
     * verification code, the email being verified, and the silent nonce returned from the send-code
     * step directly.
     *
     * @param code        the verification code from the email, or {@code null} to omit it
     * @param email       the email address being verified, or {@code null} to omit it
     * @param silentNonce the silent nonce from the send-code step, or {@code null} to omit it
     * @return the confirmation outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default Optional<BusinessAdMutationResult> confirmAdEmailOnboarding(String code, String email, String silentNonce) {
        return confirmAdEmailOnboarding(new BusinessAdEmailOnboardingConfirmation(code, email, silentNonce));
    }

    /**
     * Sends an email verification code for a Click-to-WhatsApp advertising account.
     *
     * @apiNote
     * Dispatches the verification email used to confirm the contact email of a Click-to-WhatsApp
     * advertising account and reports whether the email was sent through
     * {@link BusinessAdMutationResult#success()}; any failure reason is carried by
     * {@link BusinessAdMutationResult#errorMessage()}.
     *
     * @param request the typed send request (see
     *                {@link BusinessAdEmailVerificationCodeRequest})
     * @return the send outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws NullPointerException           if {@code request} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> sendAdEmailVerificationCode(BusinessAdEmailVerificationCodeRequest request);

    /**
     * Sends an email verification code for a Click-to-WhatsApp advertising account.
     *
     * @apiNote
     * Convenience overload of
     * {@link #sendAdEmailVerificationCode(BusinessAdEmailVerificationCodeRequest)} that takes the
     * target email directly.
     *
     * @param email the target email address, or {@code null} to omit it
     * @return the send outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default Optional<BusinessAdMutationResult> sendAdEmailVerificationCode(String email) {
        return sendAdEmailVerificationCode(new BusinessAdEmailVerificationCodeRequest(email));
    }

    /**
     * Queries the audience picker contents for a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Backs the audience step of the Click-to-WhatsApp ad flow (a paid promotion that opens a chat with
     * the business when tapped): it returns the ready-made audiences suggested for the chosen goal and
     * budget, a default targeting to pre-fill, and the merchant's reusable saved audiences.
     *
     * @param query the audience picker query parameters (see
     *              {@link BusinessAdAudienceSectionQuery})
     * @return the audience picker contents, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdAudienceSection> queryAdAudienceSection(BusinessAdAudienceSectionQuery query);

    /**
     * Queries the plain-language description of who a Click-to-WhatsApp ad will reach.
     *
     * @apiNote
     * Renders a serialized targeting into the readable sentences shown to the merchant while building a
     * Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when tapped), so the
     * merchant can read back who the ad will be shown to without inspecting the raw targeting. Each
     * returned line pairs a targeting facet with its rendered values.
     *
     * @param query the targeting-description query parameters (see
     *              {@link BusinessAdTargetingSentencesQuery})
     * @return the targeting description lines, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAdTargetingDescription> queryAdTargetingSentences(BusinessAdTargetingSentencesQuery query);

    /**
     * Queries the plain-language description of who a Click-to-WhatsApp ad will reach for the given
     * targeting and account, without the audience option or location-only narrowing.
     *
     * @apiNote
     * Convenience overload of {@link #queryAdTargetingSentences(BusinessAdTargetingSentencesQuery)}
     * for the common case in which the merchant only supplies the targeting and the ad account.
     *
     * @param adAccountId         the advertising-account identifier, or {@code null} to omit it
     * @param targetingSpecString the serialized targeting to describe, or {@code null} to omit it
     * @return the targeting description lines, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default List<BusinessAdTargetingDescription> queryAdTargetingSentences(String adAccountId, String targetingSpecString) {
        return queryAdTargetingSentences(new BusinessAdTargetingSentencesQuery(adAccountId, null, null, targetingSpecString));
    }

    /**
     * Saves a reusable audience for Click-to-WhatsApp ads.
     *
     * @apiNote
     * Persists a targeting under a name so the merchant can reapply it to future Click-to-WhatsApp ads
     * (paid promotions that open a chat with the business when tapped) without rebuilding it, returning
     * the saved audience with its identifier.
     *
     * @param legacyAdAccountId   the advertising-account identifier the audience is saved under
     * @param targetingSpecString the serialized targeting to save
     * @param name                the display name of the saved audience
     * @return the saved audience, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdSavedAudience> createSavedAudience(String legacyAdAccountId, String targetingSpecString, String name);

    /**
     * Revises a saved audience for Click-to-WhatsApp ads.
     *
     * @apiNote
     * Renames a saved audience or revises its targeting and returns the updated audience, so the
     * merchant can reapply it to future Click-to-WhatsApp ads (paid promotions that open a chat with the
     * business when tapped).
     *
     * @param name                the new display name of the saved audience
     * @param savedAudienceId     the identifier of the saved audience to revise
     * @param targetingSpecString the revised serialized targeting
     * @return the revised audience, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdSavedAudience> editSavedAudience(String name, String savedAudienceId, String targetingSpecString);

    /**
     * Removes a saved audience for Click-to-WhatsApp ads.
     *
     * @apiNote
     * Removes one of the merchant's reusable saved audiences and reports whether the removal took effect
     * through {@link BusinessAdMutationResult#success()}; the identifier the server reported as removed
     * is echoed in {@link BusinessAdMutationResult#affectedIds()}.
     *
     * @param savedAudienceId the identifier of the saved audience to remove
     * @return the removal outcome, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdMutationResult> deleteSavedAudience(String savedAudienceId);

    /**
     * Browses the interest taxonomy used to target Click-to-WhatsApp ads.
     *
     * @apiNote
     * Lists the interests directly under a taxonomy path so the merchant can drill into the categories
     * people are grouped by when narrowing who a Click-to-WhatsApp ad (a paid promotion that opens a
     * chat with the business when tapped) is shown to.
     *
     * @param adAccountId  the advertising-account identifier the browse is scoped to, or {@code null}
     *                     to omit it
     * @param audiencePath the taxonomy path to browse, or {@code null} to omit it
     * @return the interests under the path, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAdInterest> browseAdInterests(String adAccountId, String audiencePath);

    /**
     * Searches the interests used to target Click-to-WhatsApp ads.
     *
     * @apiNote
     * Returns interests matching a free-text query so the merchant can add them when narrowing who a
     * Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when tapped) is shown
     * to.
     *
     * @param query       the free-text interest query
     * @param adAccountId the advertising-account identifier the search is scoped to, or {@code null} to
     *                    omit it
     * @param count       the maximum number of results, or {@code null} to omit it
     * @return the matching interests, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAdInterest> searchAdInterests(String query, String adAccountId, Integer count);

    /**
     * Suggests further interests to target a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Given the interests already chosen for a Click-to-WhatsApp ad (a paid promotion that opens a chat
     * with the business when tapped), returns related interests the merchant may add to widen the
     * audience.
     *
     * @param query the interest-suggestion query parameters (see
     *              {@link BusinessAdInterestSuggestionQuery})
     * @return the suggested interests, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAdInterest> suggestAdInterests(BusinessAdInterestSuggestionQuery query);

    /**
     * Suggests further interests to target a Click-to-WhatsApp ad for the given scalar parameters.
     *
     * @apiNote
     * Convenience overload of {@link #suggestAdInterests(BusinessAdInterestSuggestionQuery)} that
     * builds the query from the three scalar fields.
     *
     * @param adAccountId            the advertising-account identifier the suggestion is scoped to
     * @param detailedTargetingItems the chosen interests used as the seed, or {@code null} to omit them
     * @param count                  the maximum number of suggestions, or {@code null} to omit it
     * @return the suggested interests, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default List<BusinessAdInterest> suggestAdInterests(String adAccountId, List<DetailedTargetingItem> detailedTargetingItems, Integer count) {
        return suggestAdInterests(new BusinessAdInterestSuggestionQuery(adAccountId, detailedTargetingItems, count));
    }

    /**
     * Searches nearby places to target a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Returns local places matching a free-text query so the merchant can restrict a Click-to-WhatsApp
     * ad (a paid promotion that opens a chat with the business when tapped) to a nearby area; matched
     * places carry their map point for local targeting.
     *
     * @param query the free-text place query
     * @param first the maximum number of results, or {@code null} to omit it
     * @return the matched nearby places, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAdLocation> searchAdLocalLocations(String query, Integer first);

    /**
     * Searches regions, countries, and cities to target a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Returns wider places matching a free-text query so the merchant can restrict a Click-to-WhatsApp
     * ad (a paid promotion that opens a chat with the business when tapped) to regions, countries, or
     * cities, optionally limited to certain kinds of place.
     *
     * @param query         the free-text place query
     * @param first         the maximum number of results, or {@code null} to omit it
     * @param locationTypes the kinds of place to include, or {@code null} to omit them
     * @return the matched places, or an empty list when the server returned none
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    List<BusinessAdLocation> searchAdRegionalLocations(String query, Integer first, List<String> locationTypes);

    /**
     * Queries whether a Click-to-WhatsApp ad's targeting triggers European advertising-transparency
     * rules.
     *
     * @apiNote
     * Reports whether the chosen targeting falls under the European Union advertising-transparency rules
     * that require extra disclosure, so the editor can prompt the merchant for that disclosure before a
     * Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when tapped) is run.
     *
     * @param adAccountId      the advertising-account identifier, or {@code null} to omit it
     * @param targetSpecString the serialized targeting to evaluate, or {@code null} to omit it
     * @return {@code true} when the targeting is subject to the rules, {@code false} otherwise
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    boolean queryAdTargetingEuComplianceStatus(String adAccountId, String targetSpecString);

    /**
     * Resolves the displayable location of a Click-to-WhatsApp ad's image.
     *
     * @apiNote
     * Resolves an uploaded image, stored by hash, into the location the editor fetches it from so the
     * merchant can preview it while building a Click-to-WhatsApp ad (a paid promotion that opens a chat
     * with the business when tapped).
     *
     * @param legacyAdAccountId the advertising-account identifier the image belongs to, or {@code null}
     *                          to omit it
     * @param imageHash         the uploaded image hash to resolve, or {@code null} to omit it
     * @return the resolved image location, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<URI> queryAdImageUrl(String legacyAdAccountId, String imageHash);

    /**
     * Resolves the playable location of a Click-to-WhatsApp ad's video.
     *
     * @apiNote
     * Resolves a video, stored by identifier, into the location the editor plays it from so the merchant
     * can preview it while building a Click-to-WhatsApp ad (a paid promotion that opens a chat with the
     * business when tapped).
     *
     * @param videoId the video identifier to resolve
     * @return the resolved playable location, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<URI> queryAdVideoUrl(String videoId);

    /**
     * Resolves the thumbnail location of a Click-to-WhatsApp ad's video.
     *
     * @apiNote
     * Resolves a video, stored by identifier, into the location of its preferred thumbnail so the
     * merchant sees a still preview while building a Click-to-WhatsApp ad (a paid promotion that opens a
     * chat with the business when tapped).
     *
     * @param videoId the video identifier to resolve
     * @return the resolved thumbnail location, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<URI> queryAdVideoThumbnailUrl(String videoId);

    /**
     * Resolves the playable location of a Click-to-WhatsApp ad's preview video.
     *
     * @apiNote
     * Resolves a video, stored by identifier, into the location the live-ad preview plays it from so the
     * merchant can review how a Click-to-WhatsApp ad (a paid promotion that opens a chat with the
     * business when tapped) will look.
     *
     * @param videoId the video identifier to resolve
     * @return the resolved playable location, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<URI> queryAdPreviewVideo(String videoId);

    /**
     * Queries the opening screen for creating a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Returns the data the first screen of the Click-to-WhatsApp ad flow (a paid promotion that opens a
     * chat with the business when tapped) needs: the budget steps the merchant can pick, the platforms
     * the ad may run on, the promoted page, how many advertising accounts are linked, and any onboarding
     * email to confirm.
     *
     * @param query the ad-creation root query parameters (see
     *              {@link BusinessAdCreationRootQuery})
     * @return the opening creation screen, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdCreationScreen> queryAdCreationRoot(BusinessAdCreationRootQuery query);

    /**
     * Queries the dashboard for managing Click-to-WhatsApp ads.
     *
     * @apiNote
     * Returns the data the management dashboard needs: the promoted page, the most recent unpublished
     * draft, and a page of the merchant's running and past Click-to-WhatsApp ads (paid promotions that
     * open a chat with the business when tapped).
     *
     * @param query the ad-management root query parameters (see
     *              {@link BusinessAdManagementRootQuery})
     * @return the management dashboard, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdManagementScreen> queryAdManagementRoot(BusinessAdManagementRootQuery query);

    /**
     * Queries the cost summary reviewed before publishing a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Returns the billing breakdown shown on the final review step of the Click-to-WhatsApp ad flow (a
     * paid promotion that opens a chat with the business when tapped): which advertising account is
     * billed, the estimated taxes itemised by name, and the estimated grand total.
     *
     * @param assetId        the asset identifier the summary is resolved for
     * @param budget         the budget amount in minor units, or {@code null} to omit it
     * @param budgetType     the budget-type token, or {@code null} to omit it
     * @param currency       the currency code, or {@code null} to omit it
     * @param durationInDays the campaign duration in days, or {@code null} to omit it
     * @return the cost summary, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdCreationSummary> queryAdCreationSummaryContent(String assetId, Long budget, String budgetType, String currency, Integer durationInDays);

    /**
     * Queries the confirmation screen shown after a Click-to-WhatsApp ad goes live.
     *
     * @apiNote
     * Returns the data the success screen shows after a Click-to-WhatsApp ad (a paid promotion that
     * opens a chat with the business when tapped) is published: which advertising account was billed and
     * how the chosen payment method is presented.
     *
     * @param assetId the asset identifier the success screen is resolved for
     * @param budget  the published budget, or {@code null} to omit it
     * @return the confirmation screen, or {@link Optional#empty()} when the server returned no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdSuccessScreen> queryAdCreationSuccessModal(String assetId, Long budget);

    /**
     * Queries the estimated daily reach of a Click-to-WhatsApp ad.
     *
     * @apiNote
     * Backs the reach-estimate curve shown in the ads-creation flow: it projects how many people a
     * Click-to-WhatsApp ad (a paid promotion that opens a chat with the business when tapped) is
     * expected to reach per day for a chosen budget, audience, and placement, so the merchant can gauge
     * the impact of their spend before publishing.
     *
     * @param query the daily-reach query parameters (see
     *              {@link BusinessAdEstimatedReachQuery})
     * @return the projected budget-estimate curve, or {@link Optional#empty()} when the server returned
     *         no payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdBudgetEstimate> queryEstimatedDailyReach(BusinessAdEstimatedReachQuery query);

    /**
     * Adjusts a Click-to-WhatsApp ad targeting spec so it complies with a single regulated category.
     *
     * @apiNote
     * Backs the regulated-category targeting controls in the ad-creation flow: when the audience falls
     * under one regulated category (for example housing, employment, or credit), the server rewrites
     * the targeting into a compliant form and the client applies the rewritten targeting back to its
     * audience-targeting controls.
     *
     * @param tuning the single-category tuning parameters (see
     *               {@link BusinessAdRegulatedCategoryTuning})
     * @return the adjusted targeting spec, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdTargetingTuningResult> adjustAdTargetingForRegulatedCategory(BusinessAdRegulatedCategoryTuning tuning);

    /**
     * Adjusts a Click-to-WhatsApp ad targeting spec for a single regulated category using scalar
     * parameters.
     *
     * @apiNote
     * Convenience overload of
     * {@link #adjustAdTargetingForRegulatedCategory(BusinessAdRegulatedCategoryTuning)} that builds
     * the tuning from the four scalar fields.
     *
     * @param adAccountId       the advertising-account identifier the targeting belongs to, or
     *                          {@code null} to omit it
     * @param targetingSpec     the JSON-encoded targeting spec to adjust, or {@code null} to omit it
     * @param regulatedCategory the regulated category to adjust for, or {@code null} to omit it
     * @param tuningOptions     the tuning options, or {@code null} to omit them
     * @return the adjusted targeting spec, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    default Optional<AdTargetingTuningResult> adjustAdTargetingForRegulatedCategory(String adAccountId, String targetingSpec, String regulatedCategory, TuningOptions tuningOptions) {
        return adjustAdTargetingForRegulatedCategory(new BusinessAdRegulatedCategoryTuning(adAccountId, targetingSpec, regulatedCategory, tuningOptions));
    }

    /**
     * Adjusts a Click-to-WhatsApp ad targeting spec so it complies with several regulated categories
     * and special-ad-category countries at once.
     *
     * @apiNote
     * Backs the regulated-category targeting controls in the ad-creation flow when several regulated
     * categories and special-ad-category countries apply at once: the server rewrites the targeting
     * into a compliant form and the client applies the rewritten targeting back to its
     * audience-targeting controls.
     *
     * @param tuning the multi-category tuning parameters (see
     *               {@link BusinessAdRegulatedCategoryBatchTuning})
     * @return the adjusted targeting spec, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdTargetingTuningResult> adjustAdTargetingForRegulatedCategories(BusinessAdRegulatedCategoryBatchTuning tuning);

    /**
     * Queries whether a Click-to-WhatsApp ad targeting spec is subject to the EU Digital Services
     * Act advertising-disclosure rules.
     *
     * @apiNote
     * Backs the disclosure controls in the ad-creation flow: it tells the merchant whether their
     * audience targeting triggers the EU Digital Services Act rules so the flow can surface the
     * required transparency affordances.
     *
     * @param adAccountId   the Facebook ad-account identifier the targeting belongs to
     * @param targetingSpec the JSON-encoded targeting spec to evaluate
     * @return the compliance verdict, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdTargetingComplianceStatus> queryAdTargetingEuDigitalServicesActStatus(String adAccountId, String targetingSpec);

    /**
     * Queries whether a Click-to-WhatsApp ad targeting spec is subject to the Taiwan
     * financial-services advertising rules.
     *
     * @apiNote
     * Backs the regulated-category targeting controls in the ad-creation flow: it tells the
     * merchant whether their audience targeting triggers the Taiwan financial-services advertising
     * rules so the flow can surface the required regulated-category affordances.
     *
     * @param adAccountId   the Facebook ad-account identifier the targeting belongs to
     * @param targetingSpec the JSON-encoded targeting spec to evaluate
     * @return the compliance verdict, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdTargetingComplianceStatus> queryAdTargetingTaiwanFinancialServicesStatus(String adAccountId, String targetingSpec);

    /**
     * Queries whether a Click-to-WhatsApp ad targeting spec is subject to the Australian
     * financial-services advertising rules.
     *
     * @apiNote
     * Backs the regulated-category targeting controls in the ad-creation flow: it tells the
     * merchant whether their audience targeting triggers the Australian financial-services
     * advertising rules so the flow can surface the required regulated-category affordances.
     *
     * @param adAccountId   the Facebook ad-account identifier the targeting belongs to
     * @param targetingSpec the JSON-encoded targeting spec to evaluate
     * @return the compliance verdict, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdTargetingComplianceStatus> queryAdTargetingAustraliaFinancialServicesStatus(String adAccountId, String targetingSpec);

    /**
     * Queries whether a Click-to-WhatsApp ad targeting spec is subject to the Singapore
     * universal-category advertising rules.
     *
     * @apiNote
     * Backs the regulated-category targeting controls in the ad-creation flow: it tells the
     * merchant whether their audience targeting triggers the Singapore universal-category
     * advertising rules so the flow can surface the required regulated-category affordances.
     *
     * @param adAccountId   the Facebook ad-account identifier the targeting belongs to
     * @param targetingSpec the JSON-encoded targeting spec to evaluate
     * @return the compliance verdict, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdTargetingComplianceStatus> queryAdTargetingSingaporeUniversalCategoryStatus(String adAccountId, String targetingSpec);

    /**
     * Queries whether a Click-to-WhatsApp ad targeting spec is subject to the India
     * financial-services advertising rules.
     *
     * @apiNote
     * Backs the regulated-category targeting controls in the ad-creation flow: it tells the
     * merchant whether their audience targeting triggers the India financial-services advertising
     * rules so the flow can surface the required regulated-category affordances.
     *
     * @param adAccountId   the Facebook ad-account identifier the targeting belongs to
     * @param targetingSpec the JSON-encoded targeting spec to evaluate
     * @return the compliance verdict, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<AdTargetingComplianceStatus> queryAdTargetingIndiaFinancialServicesStatus(String adAccountId, String targetingSpec);

    /**
     * Queries whether the linked WhatsApp Business account may onboard the AI business agent.
     *
     * @apiNote
     * Backs the AI-tools tile shown to business accounts: it checks whether the account is
     * eligible to onboard the AI business agent, so the tile can show or hide the onboarding
     * affordance.
     *
     * @return the eligibility verdict, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAiToolsEligibility> queryAiToolsEligibility();

    /**
     * Queries the Facebook actor whose advertising account funds the linked merchant's
     * Click-to-WhatsApp ads.
     *
     * @apiNote
     * Backs the billing-info button group in the ads-management header: it resolves the linked
     * actor's display profile (the kind of actor, the profile-picture URL, and the actor's
     * Facebook identifier) so the header can show whose account funds the ads.
     *
     * @return the linked billing actor, or {@link Optional#empty()} when the server returned no
     *         payload
     * @throws WhatsAppServerRuntimeException if the transport fails or the server reports an error
     */
    Optional<BusinessAdBillingActor> queryAdBillingInfoProfile();
}
