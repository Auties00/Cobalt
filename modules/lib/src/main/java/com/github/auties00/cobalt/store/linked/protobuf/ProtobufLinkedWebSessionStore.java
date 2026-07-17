package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaAccessTokenSession;
import com.github.auties00.cobalt.wire.linked.business.webgraphql.WhatsAppWebGraphQlSession;
import com.github.auties00.cobalt.store.linked.LinkedWebSessionStore;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * The persisted {@link LinkedWebSessionStore} implementation serialized inline with the aggregate store.
 *
 * <p>Both credential bundles are nested {@code MESSAGE} fields so they ride along in the single
 * {@code store.proto} snapshot and survive a restart, letting the WhatsApp Web GraphQL session be re-bootstrapped and
 * the Facebook GraphQL token reused without re-pairing.
 */
@SuppressWarnings("UnusedReturnValue")
@ProtobufMessage
public final class ProtobufLinkedWebSessionStore implements LinkedWebSessionStore {
    /**
     * The persisted Click-to-WhatsApp Ads-Manager session for the {@code http_comet} transport.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private CtwaAccessTokenSession facebookGraphQlSession;

    /**
     * The persisted WhatsApp Web GraphQL session for the {@code http_relay} transport.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private WhatsAppWebGraphQlSession whatsAppWebGraphQlSession;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param facebookGraphQlSession the persisted Facebook GraphQL session, or {@code null}
     * @param whatsAppWebGraphQlSession the persisted WhatsApp Web GraphQL session, or {@code null}
     */
    ProtobufLinkedWebSessionStore(CtwaAccessTokenSession facebookGraphQlSession, WhatsAppWebGraphQlSession whatsAppWebGraphQlSession) {
        this.facebookGraphQlSession = facebookGraphQlSession;
        this.whatsAppWebGraphQlSession = whatsAppWebGraphQlSession;
    }

    @Override
    public Optional<CtwaAccessTokenSession> facebookGraphQlSession() {
        return Optional.ofNullable(facebookGraphQlSession);
    }

    @Override
    public LinkedWebSessionStore setFacebookGraphQlSession(CtwaAccessTokenSession facebookGraphQlSession) {
        this.facebookGraphQlSession = facebookGraphQlSession;
        return this;
    }

    @Override
    public Optional<WhatsAppWebGraphQlSession> whatsAppWebGraphQlSession() {
        return Optional.ofNullable(whatsAppWebGraphQlSession);
    }

    @Override
    public LinkedWebSessionStore setWhatsAppWebGraphQlSession(WhatsAppWebGraphQlSession whatsAppWebGraphQlSession) {
        this.whatsAppWebGraphQlSession = whatsAppWebGraphQlSession;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ProtobufLinkedWebSessionStore that
                            && Objects.equals(facebookGraphQlSession, that.facebookGraphQlSession)
                            && Objects.equals(whatsAppWebGraphQlSession, that.whatsAppWebGraphQlSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facebookGraphQlSession, whatsAppWebGraphQlSession);
    }
}
