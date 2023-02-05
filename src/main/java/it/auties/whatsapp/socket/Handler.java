package it.auties.whatsapp.socket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

abstract class Handler {
  private final AtomicReference<ExecutorService> service;
  private final AtomicReference<CountDownLatch> latch;
  private final AtomicReference<Semaphore> semaphore;

  public Handler(){
    this.service = new AtomicReference<>();
    this.latch = new AtomicReference<>();
    this.semaphore = new AtomicReference<>();
  }

  protected void dispose() {
    var serviceValue = service.getAndSet(null);
    if (serviceValue != null) {
      serviceValue.shutdownNow();
    }
  }

  protected CountDownLatch getOrCreateLatch() {
    var value = latch.get();
    if (value != null) {
      return value;
    }
    var newValue = new CountDownLatch(1);
    latch.set(newValue);
    return newValue;
  }

  protected ExecutorService getOrCreateService() {
    var value = service.get();
    if (value != null && !value.isShutdown()) {
      return value;
    }
    var newValue = createService();
    service.set(newValue);
    return newValue;
  }

  protected ExecutorService createService() {
    return Executors.newSingleThreadExecutor();
  }

  protected AutoCloseable getOrCreateSemaphore() throws InterruptedException{
    var value = semaphore.get();
    if (value != null) {
      value.acquire();
      return value::release;
    }
    var newValue = new Semaphore(1);
    semaphore.set(newValue);
    newValue.acquire();
    return newValue::release;
  }

  protected void completeLatch() {
    getOrCreateLatch().countDown();
  }

  protected void awaitLatch() {
    try {
      getOrCreateLatch().await();
    } catch (InterruptedException exception) {
      throw new RuntimeException("Cannot await latch", exception);
    }
  }
}
