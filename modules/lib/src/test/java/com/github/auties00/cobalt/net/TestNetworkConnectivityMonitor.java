package com.github.auties00.cobalt.net;

import com.github.auties00.vigil.ConnectivityEvent;
import com.github.auties00.vigil.ConnectivityListener;
import com.github.auties00.vigil.ConnectivityMonitor;
import com.github.auties00.vigil.ConnectivityState;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link ConnectivityMonitor} whose online state is driven programmatically by
 * {@link #setOnline(boolean)}, letting supervisor tests exercise offline gating and the offline-to-online
 * edge without any native code.
 */
final class TestNetworkConnectivityMonitor implements ConnectivityMonitor {
    private final Object lock = new Object();
    private final List<ConnectivityListener> listeners = new CopyOnWriteArrayList<>();
    private volatile ConnectivityState state;

    TestNetworkConnectivityMonitor(boolean initiallyOnline) {
        this.state = initiallyOnline ? ConnectivityState.ONLINE : ConnectivityState.OFFLINE;
    }

    @Override
    public ConnectivityMonitor start() {
        return this;
    }

    @Override
    public ConnectivityState state() {
        return state;
    }

    @Override
    public boolean isEventDriven() {
        return false;
    }

    @Override
    public void addListener(ConnectivityListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ConnectivityListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void awaitOnline() throws InterruptedException {
        synchronized (lock) {
            while (state != ConnectivityState.ONLINE) {
                lock.wait();
            }
        }
    }

    @Override
    public boolean awaitOnline(Duration timeout) throws InterruptedException {
        var deadline = System.nanoTime() + timeout.toNanos();
        synchronized (lock) {
            while (state != ConnectivityState.ONLINE) {
                var remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    break;
                }
                lock.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            }
            return state == ConnectivityState.ONLINE;
        }
    }

    @Override
    public void close() {
        // No resources to release
    }

    /**
     * Sets the simulated online state, waking await-online waiters and firing a transition event to
     * registered listeners on an offline-to-online edge.
     *
     * @param now the new online state
     */
    void setOnline(boolean now) {
        var next = now ? ConnectivityState.ONLINE : ConnectivityState.OFFLINE;
        ConnectivityState previous;
        synchronized (lock) {
            previous = state;
            if (previous == next) {
                return;
            }
            state = next;
            lock.notifyAll();
        }
        var event = new ConnectivityEvent(previous, next, Instant.now());
        for (var listener : listeners) {
            listener.onConnectivityChanged(event);
        }
    }
}
