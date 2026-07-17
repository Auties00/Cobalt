package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the bulk {@code <iq xmlns="encrypt" type="get"/>} that asks the relay for the long-term
 * identity public key of one or more peer devices.
 *
 * <p>This request materialises Signal-protocol identity records for newly discovered devices before
 * any Signal session can be initiated. WA Web drives it from the contact-sync path
 * ({@code WAWebContactSyncApi}, {@code WAWebAdvSyncDeviceListApi}) after a fresh
 * {@code <list>...</list>} of devices is received, and from {@code WAWebGalaxyFlowsIdentityFetcher}
 * for the Galaxy-flows lazy fetch. The request body is a single {@code <identity/>} child carrying
 * one {@code <user jid="..."/>} grandchild per device.
 */
@WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
public final class IqGetIdentityKeysRequest implements IqStanza.Request {
    /**
     * The non-empty list of device JIDs being queried; each entry becomes the {@code jid}
     * attribute of one {@code <user/>} grandchild.
     */
    private final List<Jid> deviceJids;

    /**
     * Constructs a bulk identity-key request for the supplied device JIDs.
     *
     * <p>The list is defensively copied; subsequent mutation of the passed-in list does not affect
     * this instance. WA Web batches as many device JIDs as the relay accepts in one {@code <iq>};
     * the caller is expected to slice oversized lists before constructing this request.
     *
     * @param deviceJids the device JIDs to query, non-empty
     * @throws NullPointerException     if {@code deviceJids} is {@code null}
     * @throws IllegalArgumentException if {@code deviceJids} is empty
     */
    public IqGetIdentityKeysRequest(List<Jid> deviceJids) {
        Objects.requireNonNull(deviceJids, "deviceJids cannot be null");
        if (deviceJids.isEmpty()) {
            throw new IllegalArgumentException("deviceJids cannot be empty");
        }
        this.deviceJids = List.copyOf(deviceJids);
    }

    /**
     * Returns the unmodifiable list of device JIDs being queried.
     *
     * @return the device JIDs
     */
    public List<Jid> deviceJids() {
        return deviceJids;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq>} addressed to {@link JidServer#user()} with {@code xmlns="encrypt"}
     * and {@code type="get"}, wrapping {@link #deviceJids()} as one {@code <user jid="..."/>} per
     * entry under a single {@code <identity/>} parent. Each {@link Jid} is rendered through
     * {@link StanzaBuilder#attribute(String, com.github.auties00.cobalt.wire.core.jid.JidProvider)} in its
     * canonical form.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebGetIdentityKeysJob",
            exports = "getAndStoreIdentityKeys", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var userNodes = new ArrayList<Stanza>(deviceJids.size());
        for (var deviceJid : deviceJids) {
            var userNode = new StanzaBuilder()
                    .description("user")
                    .attribute("jid", deviceJid)
                    .build();
            userNodes.add(userNode);
        }
        var identityNode = new StanzaBuilder()
                .description("identity")
                .content(userNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", RandomIdUtils.newId())
                .attribute("xmlns", "encrypt")
                .attribute("type", "get")
                .attribute("to", JidServer.user())
                .content(identityNode);
    }

    /**
     * Compares this request to another instance for equality.
     *
     * @param obj the candidate instance
     * @return {@code true} when {@code obj} is an {@code IqGetIdentityKeysRequest} carrying the
     *         same device JID list in the same order
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqGetIdentityKeysRequest) obj;
        return Objects.equals(this.deviceJids, that.deviceJids);
    }

    /**
     * Returns a hash code derived from the device JID list.
     *
     * @return the combined hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(deviceJids);
    }

    /**
     * Returns the record-style rendering for this request.
     *
     * @return the rendered string
     */
    @Override
    public String toString() {
        return "IqGetIdentityKeysRequest[deviceJids=" + deviceJids + ']';
    }
}
