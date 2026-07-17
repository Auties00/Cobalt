package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;

/**
 * The sealed root of the factories that construct and load {@link WhatsAppStore} instances.
 *
 * <p>The two client flavours keep fundamentally different state, so each owns its own factory contract:
 * {@link LinkedWhatsAppStoreFactory} builds and loads the socket-based Linked sessions (keyed by client
 * type and UUID or phone number), while {@link CloudWhatsAppStoreFactory} builds and loads the Cloud API
 * sessions (keyed by phone number id). Their {@code create} and {@code load} signatures do not overlap,
 * so this supertype carries no shared methods; it exists so the two factory families share a sealed root
 * the same way {@link WhatsAppStore} is the sealed root of the two store families, and a {@code switch}
 * over a {@code WhatsAppStoreFactory} is exhaustive with those two cases.
 *
 * @see WhatsAppStore
 * @see LinkedWhatsAppStoreFactory
 * @see CloudWhatsAppStoreFactory
 */
public sealed interface WhatsAppStoreFactory permits LinkedWhatsAppStoreFactory, CloudWhatsAppStoreFactory {
}
