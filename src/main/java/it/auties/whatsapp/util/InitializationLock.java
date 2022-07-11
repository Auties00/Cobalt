package it.auties.whatsapp.util;

import java.util.concurrent.CountDownLatch;

public class InitializationLock<T> {
    private T element;
    private final CountDownLatch latch;
    public InitializationLock(){
        this.latch = new CountDownLatch(1);
    }

    public InitializationLock<T> write(T element){
        latch.countDown();
        this.element = element;
        return this;
    }

    public T read(){
        try {
            latch.await();
            return element;
        }catch (InterruptedException exception){
            throw new RuntimeException("Cannot read value", exception);
        }
    }
}
