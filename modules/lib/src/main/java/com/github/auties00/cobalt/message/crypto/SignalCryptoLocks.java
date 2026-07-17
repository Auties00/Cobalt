package com.github.auties00.cobalt.message.crypto;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Serialises the non-atomic Signal read-ratchet-store cycles shared between the outbound
 * {@link com.github.auties00.cobalt.message.send.crypto.MessageEncryption} and the inbound
 * {@link com.github.auties00.cobalt.message.receive.crypto.MessageDecryption} pipelines.
 * <p>
 * A pairwise Signal session and a group sender-key chain are mutated in place by every encrypt and decrypt: each
 * operation loads the record, derives a message key, ratchets the chain forward, and stores the record back. That cycle
 * is not atomic, so two operations against the same session or sender key must never run concurrently. A single instance
 * is shared by both pipelines so that an outbound encrypt and an inbound decrypt touching the same device session or
 * sender-key chain contend on the same lock; distinct sessions and sender keys map to distinct locks and proceed in
 * parallel.
 *
 * @implNote This implementation keys one {@link ReentrantLock} per {@link SignalProtocolAddress} and per
 * {@link SignalSenderKeyName} in two {@link ConcurrentMap} registries. WhatsApp Web needs no equivalent because its
 * JavaScript runs single-threaded and never encrypts or decrypts concurrently; Cobalt runs both directions on virtual
 * threads, so it serialises explicitly. Locks are created on first use and retained for the client lifetime; the
 * registries are bounded by the number of distinct devices and sender keys the client talks to, the same order as the
 * session and sender-key state the store already holds.
 */
public final class SignalCryptoLocks {
    /**
     * The logger for {@link SignalCryptoLocks}.
     */
    private static final System.Logger LOGGER = Log.get(SignalCryptoLocks.class);

    /**
     * Holds one lock per pairwise Signal session, keyed by the device {@link SignalProtocolAddress}.
     */
    private final ConcurrentMap<SignalProtocolAddress, ReentrantLock> sessionLocks;

    /**
     * Holds one lock per group sender-key chain, keyed by the {@link SignalSenderKeyName}.
     */
    private final ConcurrentMap<SignalSenderKeyName, ReentrantLock> senderKeyLocks;

    /**
     * Constructs an empty lock registry.
     */
    public SignalCryptoLocks() {
        this.sessionLocks = new ConcurrentHashMap<>();
        this.senderKeyLocks = new ConcurrentHashMap<>();
    }

    /**
     * Runs the given action while holding the lock for the pairwise Signal session identified by {@code address}.
     * <p>
     * Serialises the full encrypt or decrypt cycle against one device session; operations on other sessions proceed in
     * parallel.
     *
     * @param <T>     the type returned by {@code action}
     * @param address the device {@link SignalProtocolAddress} whose session is being mutated
     * @param action  the encrypt or decrypt cycle to run under the lock
     * @return the value returned by {@code action}
     * @throws NullPointerException if any argument is {@code null}
     * @throws RuntimeException     if {@code action} throws; the lock is released before the exception propagates
     */
    public <T> T withSession(SignalProtocolAddress address, Supplier<T> action) {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "acquiring session lock user={0} device={1}", new LogRedactable.User(address.name()), address.id());
        }
        return withLock(sessionLocks, address, action);
    }

    /**
     * Runs the given result-less action while holding the lock for the pairwise Signal session identified by
     * {@code address}.
     * <p>
     * Behaves as {@link #withSession(SignalProtocolAddress, Supplier)} for actions that mutate session state without
     * producing a value.
     *
     * @param address the device {@link SignalProtocolAddress} whose session is being mutated
     * @param action  the session-mutating action to run under the lock
     * @throws NullPointerException if any argument is {@code null}
     * @throws RuntimeException     if {@code action} throws; the lock is released before the exception propagates
     */
    public void withSession(SignalProtocolAddress address, Runnable action) {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "acquiring session lock user={0} device={1}", new LogRedactable.User(address.name()), address.id());
        }
        withLock(sessionLocks, address, action);
    }

    /**
     * Runs the given action while holding the lock for the group sender-key chain identified by {@code senderKeyName}.
     * <p>
     * Serialises the full encrypt, decrypt, distribution-import, or rotation cycle against one sender-key chain;
     * operations on other sender keys proceed in parallel.
     *
     * @param <T>           the type returned by {@code action}
     * @param senderKeyName the {@link SignalSenderKeyName} whose chain is being mutated
     * @param action        the sender-key cycle to run under the lock
     * @return the value returned by {@code action}
     * @throws NullPointerException if any argument is {@code null}
     * @throws RuntimeException     if {@code action} throws; the lock is released before the exception propagates
     */
    public <T> T withSenderKey(SignalSenderKeyName senderKeyName, Supplier<T> action) {
        Objects.requireNonNull(senderKeyName, "senderKeyName cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "acquiring sender-key lock group={0} sender={1}",
                    new LogRedactable.User(senderKeyName.groupId()), new LogRedactable.User(senderKeyName.sender().name()));
        }
        return withLock(senderKeyLocks, senderKeyName, action);
    }

    /**
     * Runs the given result-less action while holding the lock for the group sender-key chain identified by
     * {@code senderKeyName}.
     * <p>
     * Behaves as {@link #withSenderKey(SignalSenderKeyName, Supplier)} for actions that mutate sender-key state without
     * producing a value, such as importing a distribution or rotating the chain.
     *
     * @param senderKeyName the {@link SignalSenderKeyName} whose chain is being mutated
     * @param action        the sender-key-mutating action to run under the lock
     * @throws NullPointerException if any argument is {@code null}
     * @throws RuntimeException     if {@code action} throws; the lock is released before the exception propagates
     */
    public void withSenderKey(SignalSenderKeyName senderKeyName, Runnable action) {
        Objects.requireNonNull(senderKeyName, "senderKeyName cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "acquiring sender-key lock group={0} sender={1}",
                    new LogRedactable.User(senderKeyName.groupId()), new LogRedactable.User(senderKeyName.sender().name()));
        }
        withLock(senderKeyLocks, senderKeyName, action);
    }

    /**
     * Runs {@code action} while holding the {@link ReentrantLock} mapped to {@code key} in {@code locks}, creating the
     * lock on first use.
     *
     * @param <K>    the lock-key type, either a {@link SignalProtocolAddress} or a {@link SignalSenderKeyName}
     * @param <T>    the type returned by {@code action}
     * @param locks  the registry mapping each key to its lock
     * @param key    the Signal-state identity to serialise on
     * @param action the action to run while holding the lock
     * @return the value returned by {@code action}
     */
    private static <K, T> T withLock(ConcurrentMap<K, ReentrantLock> locks, K key, Supplier<T> action) {
        var lock = locks.computeIfAbsent(key, _ -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the result-less {@code action} while holding the {@link ReentrantLock} mapped to {@code key} in
     * {@code locks}, creating the lock on first use.
     *
     * @param <K>    the lock-key type, either a {@link SignalProtocolAddress} or a {@link SignalSenderKeyName}
     * @param locks  the registry mapping each key to its lock
     * @param key    the Signal-state identity to serialise on
     * @param action the action to run while holding the lock
     */
    private static <K> void withLock(ConcurrentMap<K, ReentrantLock> locks, K key, Runnable action) {
        var lock = locks.computeIfAbsent(key, _ -> new ReentrantLock());
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
