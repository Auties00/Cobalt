package it.auties.whatsapp.crypto;

import it.auties.whatsapp.util.SignalSpecification;
import lombok.SneakyThrows;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

interface BlockingCipher extends SignalSpecification {
    Semaphore LOCK = new Semaphore(1);

    default void run(Runnable runnable) {
        run(() -> {
            runnable.run();
            return null;
        });
    }

    default  <T> T run(Supplier<T> runnable) {
        try {
            LOCK.acquire();
            return runnable.get();
        } catch (InterruptedException exception){
            throw new RuntimeException("Cannot acquire lock", exception);
        }finally {
            LOCK.release();
        }
    }
}
