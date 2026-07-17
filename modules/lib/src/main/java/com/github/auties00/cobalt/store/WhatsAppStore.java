package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

/**
 * The sealed root of the persistent state backing a {@link com.github.auties00.cobalt.client.WhatsAppClient}.
 *
 * <p>The two client flavours keep fundamentally different state, so the store is split by flavour:
 * {@link LinkedWhatsAppStore} holds the rich session state of the socket-based Linked client (Signal
 * keys, chats, contacts, messages, app-state versions, listeners), while {@link CloudWhatsAppStore}
 * holds only the credential and webhook configuration the Cloud API client needs. This type is the
 * common supertype of both; a {@code switch} over a {@code WhatsAppStore} is exhaustive with those two
 * cases.
 */
public sealed interface WhatsAppStore permits LinkedWhatsAppStore, CloudWhatsAppStore {
}
