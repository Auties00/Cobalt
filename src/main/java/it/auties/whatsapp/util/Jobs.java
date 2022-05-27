package it.auties.whatsapp.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@UtilityClass
public class Jobs {
    private final Semaphore LOCK = new Semaphore(1);
    public void run(Runnable runnable){
        run(() -> {
            runnable.run();
            return null;
        });
    }

    @SneakyThrows
    public <T> T run(Supplier<T> runnable){
        try {
            LOCK.acquire();
            return runnable.get();
        }catch (InterruptedException exception){
            throw new RuntimeException("Cannot lock on job");
        } finally {
            LOCK.release();
        }
    }
}
