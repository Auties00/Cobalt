package it.auties.whatsapp.util;

import java.util.concurrent.CountDownLatch;

public class InitializationLock<T> {
    private final CountDownLatch latch;
    private T element;

    public InitializationLock() {
        this.latch = new CountDownLatch(1);
    }

    public InitializationLock<T> write(T element) {
        latch.countDown();
        this.element = element;
        return this;
    }

    public T read() {
        await();
        return element;
    }

    public void await() {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            throw new RuntimeException("Cannot lock", exception);
        }
    }
}
