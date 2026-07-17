package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientOfflineResumeState;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppConnectionStore;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The {@link LinkedWhatsAppConnectionStore} holding this session's connection-runtime state.
 *
 * <p>This sub-store of {@link ProtobufWhatsAppStore} holds only transient, per-connection state, so
 * unlike the persisted sub-stores it is not a protobuf message: the owning aggregate allocates a fresh
 * instance on every construction and never serializes it.
 *
 * @implNote
 * This implementation starts in {@link LinkedWhatsAppClientOfflineResumeState#INIT} with a fresh
 * single-count latch; {@link #setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState)} counts the
 * latch down on {@link LinkedWhatsAppClientOfflineResumeState#COMPLETE} and re-arms it on
 * {@link LinkedWhatsAppClientOfflineResumeState#INIT} so a fresh connection can wait again.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
final class ProtobufLinkedWhatsAppConnectionStore implements LinkedWhatsAppConnectionStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppConnectionStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppConnectionStore.class);

    /**
     * The optional HTTP/SOCKS proxy configuration.
     */
    private WhatsAppClientProxy proxy;

    /**
     * The offline-resume state.
     */
    private volatile LinkedWhatsAppClientOfflineResumeState offlineResumeState;

    /**
     * The latch coordinating offline-delivery completion.
     */
    private volatile CountDownLatch offlineDeliveryLatch;

    /**
     * The opaque server-routing token for load balancer pinning.
     */
    private byte[] routingInfo;

    /**
     * The routing domain hint.
     */
    private String routingDomain;

    /**
     * Constructs an empty connection sub-store in the {@link LinkedWhatsAppClientOfflineResumeState#INIT}
     * state with a fresh offline-delivery latch.
     */
    ProtobufLinkedWhatsAppConnectionStore() {
        this.offlineResumeState = LinkedWhatsAppClientOfflineResumeState.INIT;
        this.offlineDeliveryLatch = new CountDownLatch(1);
    }

    @Override
    public Optional<WhatsAppClientProxy> proxy() {
        return Optional.ofNullable(proxy);
    }

    @Override
    public LinkedWhatsAppConnectionStore setProxy(WhatsAppClientProxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public LinkedWhatsAppClientOfflineResumeState offlineResumeState() {
        return offlineResumeState;
    }

    @Override
    public LinkedWhatsAppConnectionStore setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState state) {
        Objects.requireNonNull(state, "state cannot be null");
        var previous = this.offlineResumeState;
        this.offlineResumeState = state;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "offline resume state {0} -> {1}", previous, state);
        if (state == LinkedWhatsAppClientOfflineResumeState.COMPLETE) {
            offlineDeliveryLatch.countDown();
        } else if (state == LinkedWhatsAppClientOfflineResumeState.INIT) {
            offlineDeliveryLatch = new CountDownLatch(1);
        }
        return this;
    }

    @Override
    public boolean isResumeFromRestartComplete() {
        return offlineResumeState != LinkedWhatsAppClientOfflineResumeState.INIT
               && offlineResumeState != LinkedWhatsAppClientOfflineResumeState.RESUME_ON_RESTART;
    }

    @Override
    public void waitForOfflineDeliveryEnd() {
        if (offlineResumeState == LinkedWhatsAppClientOfflineResumeState.COMPLETE) {
            return;
        }
        try {
            var completed = offlineDeliveryLatch.await(5, TimeUnit.MINUTES);
            if (!completed && Log.WARNING) {
                LOGGER.log(Level.WARNING, "offline delivery wait timed out after 5 minutes");
            }
        } catch (InterruptedException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "offline delivery wait interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Optional<byte[]> routingInfo() {
        return Optional.ofNullable(routingInfo);
    }

    @Override
    public LinkedWhatsAppConnectionStore setRoutingInfo(byte[] routingInfo) {
        this.routingInfo = routingInfo;
        return this;
    }

    @Override
    public Optional<String> routingDomain() {
        return Optional.ofNullable(routingDomain);
    }

    @Override
    public LinkedWhatsAppConnectionStore setRoutingDomain(String routingDomain) {
        this.routingDomain = routingDomain;
        return this;
    }
}
