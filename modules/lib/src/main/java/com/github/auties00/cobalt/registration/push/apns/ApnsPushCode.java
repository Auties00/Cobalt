package com.github.auties00.cobalt.registration.push.apns;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.registration.push.apns.courier.ApnsPayloadTag;

import java.io.IOException;
import java.lang.System.Logger.Level;

/**
 * Hands the WhatsApp verification code received over the APNS courier stream to the caller of
 * {@link ApnsClient#getPushCode()}.
 *
 * <p>A single-value, set-once synchronisation primitive. The producer
 * ({@link ApnsCourierConnection}) calls {@link #deliver(String)} once a {@code regcode} entry is
 * observed in the JSON payload of an incoming {@link ApnsPayloadTag#NOTIFICATION}. The consumer
 * ({@link ApnsClient}) blocks in {@link #waitForCode()} until either the value is delivered or
 * {@link #close()} is invoked. Replay-safe: the first delivery wins, so a code arriving before
 * {@link #waitForCode()} is called is not lost, and subsequent deliveries (for example on courier
 * reconnect) do not overwrite the original.
 *
 * @implNote This implementation uses plain {@code synchronized} plus {@code wait}/{@code notifyAll}
 *           on a dedicated monitor rather than {@link java.util.concurrent.CompletableFuture} or
 *           {@link java.util.concurrent.locks.ReentrantLock} because JEP 491 (JDK 24) removed
 *           carrier-thread pinning on {@code synchronized}, making wait/notify fully
 *           virtual-thread friendly and cheaper than the lock-based alternatives.
 */
final class ApnsPushCode {
    /**
     * The logger for {@link ApnsPushCode}.
     */
    private static final System.Logger LOGGER = Log.get(ApnsPushCode.class);

    /**
     * Guards {@link #code} and {@link #closed}.
     *
     * <p>A dedicated monitor rather than {@code synchronized (this)} so external callers that
     * inadvertently synchronise on the holder cannot deadlock the producer or consumer paths.
     */
    private final Object lock;

    /**
     * Holds the verification code value once delivered.
     *
     * <p>{@code null} until the first {@link #deliver(String)} call lands; stays set for the lifetime
     * of the holder so a code arriving before {@link #waitForCode()} is called is not lost.
     */
    private String code;

    /**
     * Records whether {@link #close()} has been invoked.
     *
     * <p>Once set, any thread parked in {@link #waitForCode()} unblocks and surfaces an
     * {@link IOException} rather than waiting forever.
     */
    private boolean closed;

    /**
     * Constructs an empty holder.
     *
     * <p>The caller publishes the instance to its single producer and one or more consumers via a
     * happens-before edge, typically by storing it in a {@code final} field on a containing object.
     */
    ApnsPushCode() {
        this.lock = new Object();
    }

    /**
     * Blocks the calling thread until either a code is delivered or the holder is closed.
     *
     * <p>Returns immediately if a value was already delivered before the call. Safe to call from
     * multiple threads concurrently; every caller observes the same delivered value.
     *
     * @implNote This implementation loops on {@link Object#wait()} until one of the two predicates
     *           ({@code code != null} or {@code closed}) becomes true, absorbing spurious wakeups.
     *
     * @return the delivered verification code
     * @throws InterruptedException if the caller is interrupted while waiting
     * @throws IOException          if the holder was closed before any code arrived
     */
    String waitForCode() throws InterruptedException, IOException {
        synchronized (lock) {
            while (code == null && !closed) {
                lock.wait();
            }
            if (code == null) {
                throw new IOException("ApnsPushCode is closed");
            }
            return code;
        }
    }

    /**
     * Stores the first verification code seen and wakes every waiter blocked in
     * {@link #waitForCode()}.
     *
     * <p>Subsequent invocations are no-ops: WhatsApp's registration flow only ever sends one code per
     * session, and replays after a courier reconnect must surface the original value rather than
     * overwrite it. A {@code null} input is silently dropped so the producer can call this even when
     * the notification JSON has no {@code regcode} field.
     *
     * @param code the verification code value, or {@code null} when the source JSON had no
     *             {@code regcode}
     */
    void deliver(String code) {
        if (code == null) {
            return;
        }
        synchronized (lock) {
            if (this.code == null) {
                this.code = code;
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns push code stored {0}", new LogRedactable.Code(code));
                lock.notifyAll();
            }
        }
    }

    /**
     * Marks the holder closed and wakes every pending waiter.
     *
     * <p>Idempotent. Invoked by {@link ApnsClient#close()} so any thread parked in
     * {@link #waitForCode()} surfaces an {@link IOException} instead of waiting forever.
     */
    void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            if (Log.DEBUG && code == null) LOGGER.log(Level.DEBUG, "apns push code holder closed before delivery");
            closed = true;
            lock.notifyAll();
        }
    }
}
