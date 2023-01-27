package it.auties.whatsapp.socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

abstract class Handler {
  private static final int DEFAULT_CORES = 10;

  private ExecutorService service;

  protected void dispose() {
    if(service == null){
      return;
    }

    service.shutdownNow();
  }

  protected ExecutorService getOrCreateService(){
    if(service != null && !service.isShutdown()){
      return service;
    }

    return this.service = Executors.newSingleThreadExecutor();
  }

  protected ExecutorService getOrCreatePooledService(){
    if(service != null && !service.isShutdown()){
      return service;
    }

    return this.service = Executors.newFixedThreadPool(DEFAULT_CORES);
  }

  protected ScheduledExecutorService getOrCreateScheduledService(){
    if(service != null && !service.isShutdown()){
      return (ScheduledExecutorService) service;
    }

    var result = Executors.newSingleThreadScheduledExecutor();
    this.service = result;
    return result;
  }

  protected void shutdownService() {
    service.shutdownNow();
  }
}
