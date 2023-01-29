package it.auties.whatsapp.socket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

abstract class Handler {
  private static final int DEFAULT_CORES = 10;

  private ExecutorService service;
  private CountDownLatch latch;

  protected void dispose() {
    latch = null;
    if (service == null) {
      return;
    }
    service.shutdownNow();
  }

  protected CountDownLatch getOrCreateLatch() {
    if (latch != null) {
      return latch;
    }
    return this.latch = new CountDownLatch(1);
  }

  protected void completeLatch() {
    if (latch == null) {
      return;
    }
    latch.countDown();
  }

  protected void awaitLatch() {
    try {
      if (latch == null) {
        return;
      }
      latch.await();
    } catch (InterruptedException exception) {
      throw new RuntimeException("Cannot await latch", exception);
    }
  }

  protected ExecutorService getOrCreateService() {
    if (service != null && !service.isShutdown()) {
      return service;
    }
    return this.service = Executors.newSingleThreadExecutor();
  }

  protected ExecutorService getOrCreatePooledService() {
    if (service != null && !service.isShutdown()) {
      return service;
    }
    return this.service = Executors.newFixedThreadPool(DEFAULT_CORES);
  }

  protected ScheduledExecutorService getOrCreateScheduledService() {
    if (service != null && !service.isShutdown()) {
      return (ScheduledExecutorService) service;
    }
    var result = Executors.newSingleThreadScheduledExecutor();
    this.service = result;
    return result;
  }
}
