package com.github.auties00.cobalt.client;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientBuilder;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientBuilder;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;

/**
 * Root builder that branches into the two {@link WhatsAppClient} flavours.
 *
 * <p>This is the entry point reachable through {@link WhatsAppClient#builder()}. It exposes
 * {@link #linkedApi()} for the socket-based Web/Mobile clients and {@link #cloudApi()} for the Cloud
 * API client; each returns a flavour-specific sub-builder that guides the caller through the steps
 * that apply to that transport.
 *
 * @see WhatsAppClient
 * @see LinkedWhatsAppClientBuilder
 * @see CloudWhatsAppClientBuilder
 */
public final class WhatsAppClientBuilder {
    /**
     * The shared root builder, accessed via {@link WhatsAppClient#builder()}.
     */
    static final WhatsAppClientBuilder INSTANCE = new WhatsAppClientBuilder();

    /**
     * Private singleton constructor; obtain the instance via {@link WhatsAppClient#builder()}.
     */
    private WhatsAppClientBuilder() {

    }

    /**
     * Returns the builder for the socket-based {@link LinkedWhatsAppClient} flavours.
     *
     * <p>The returned builder offers the web companion linking flow, the mobile registration flow,
     * and a low-level custom-store flow.
     *
     * @return a fresh Linked client builder
     */
    public LinkedWhatsAppClientBuilder linkedApi() {
        return LinkedWhatsAppClient.builder();
    }

    /**
     * Returns the builder for the {@link CloudWhatsAppClient} flavour, backed by the
     * {@link CloudWhatsAppStoreFactory#persistent() persistent} store factory.
     *
     * <p>The returned builder collects the Cloud credentials (access token, phone number id, WhatsApp
     * Business Account id) and the webhook receiver configuration before producing the client.
     *
     * @return a fresh Cloud client builder
     */
    public CloudWhatsAppClientBuilder cloudApi() {
        return CloudWhatsAppClient.builder();
    }

    /**
     * Returns the builder for the {@link CloudWhatsAppClient} flavour, backed by the given store factory.
     *
     * @apiNote
     * Supply {@link CloudWhatsAppStoreFactory#temporary()} for a RAM-only session or
     * {@link CloudWhatsAppStoreFactory#persistent(java.nio.file.Path)} for a custom storage directory.
     *
     * @param storeFactory the factory that resolves the backing store
     * @return a fresh Cloud client builder
     * @throws NullPointerException if {@code storeFactory} is {@code null}
     */
    public CloudWhatsAppClientBuilder cloudApi(CloudWhatsAppStoreFactory storeFactory) {
        return CloudWhatsAppClient.builder(storeFactory);
    }
}
