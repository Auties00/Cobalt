package it.auties.whatsapp.socket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

abstract class Handler {
  private static final int DEFAULT_CORES = 10;

  private final AtomicReference<ExecutorService> service;
  private final AtomicReference<ExecutorService> fallbackService;
  private final AtomicReference<CountDownLatch> latch;

  public Handler(){
    this.service = new AtomicReference<>();
    this.fallbackService = new AtomicReference<>();
    this.latch = new AtomicReference<>();
  }

  protected void dispose() {
    var latchValue = latch.getAndSet(null);
    if(latchValue != null) {
      latchValue.countDown();
    }

    var serviceValue = service.getAndSet(null);
    if (serviceValue != null) {
      serviceValue.shutdownNow();
    }

    var fallbackServiceValue = fallbackService.getAndSet(null);
    if (fallbackServiceValue != null) {
      fallbackServiceValue.shutdownNow();
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

  protected void completeLatch() {
    var value = latch.get();
    if (value == null) {
      return;
    }
    value.countDown();
  }

  protected void awaitLatch() {
    try {
      var value = latch.get();
      if (value == null) {
        return;
      }
      value.await();
    } catch (InterruptedException exception) {
      throw new RuntimeException("Cannot await latch", exception);
    }
  }

  protected ExecutorService getOrCreateService() {
    var value = service.get();
    if (value != null && !value.isShutdown()) {
      return value;
    }
    var newValue = Executors.newSingleThreadExecutor();
    service.set(newValue);
    return newValue;
  }

  protected ExecutorService getOrCreateFallbackService() {
    var value = fallbackService.get();
    if (value != null && !value.isShutdown()) {
      return value;
    }
    var newValue = Executors.newSingleThreadExecutor();
    fallbackService.set(newValue);
    return newValue;
  }

  protected ExecutorService getOrCreatePooledService() {
    var value = service.get();
    if (value != null && !value.isShutdown()) {
      return value;
    }
    var newValue = Executors.newFixedThreadPool(DEFAULT_CORES);
    service.set(newValue);
    return newValue;
  }

  protected ScheduledExecutorService getOrCreateScheduledService() {
    var value = service.get();
    if (value != null && !value.isShutdown()) {
      return (ScheduledExecutorService) value;
    }
    var newValue = Executors.newSingleThreadScheduledExecutor();
    service.set(newValue);
    return newValue;
  }
}
