package com.github.auties00.cobalt.wire.core.jid;

/**
 * Provides a uniform way to obtain a {@link Jid} from any WhatsApp model type that is
 * addressable through the WhatsApp protocol.
 *
 * <p>Several types in the WhatsApp domain carry an associated JID, such as contacts,
 * chats, newsletters, and server descriptors. This interface unifies access to the
 * underlying {@code Jid} by requiring each implementor to expose a {@link #toJid()}
 * method. API consumers can therefore accept a {@code JidProvider} wherever a JID is
 * needed, freeing callers from having to adapt between the concrete model types.
 *
 * <p>Implementors include {@link Jid} (which returns itself) and {@link JidServer} (which
 * returns a server-only JID for its domain), as well as the addressable domain types
 * ({@code Contact}, {@code Chat}, {@code Newsletter}) that live in the Linked model module.
 * This interface is non-sealed because those implementors reside in a downstream module,
 * which the module system does not permit a sealed hierarchy to span.
 */
public interface JidProvider {
    /**
     * Returns the {@link Jid} that addresses this object in the WhatsApp protocol.
     *
     * @return the non-null {@code Jid} for this object
     */
    Jid toJid();
}
