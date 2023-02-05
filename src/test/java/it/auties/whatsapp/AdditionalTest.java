package it.auties.whatsapp;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdditionalTest {
  private static final ExecutorService service = Executors.newCachedThreadPool();
  public static void main(String[] args) throws InterruptedException {
    var size = 10;
    var latch = new CountDownLatch(size);
    for(var i = 0; i < size; i++){
      onUnlock(size, latch, i);
    }

    for(var i = 0; i < size / 2; i++){
      onIteration(i);
    }

    latch.await();
  }

  private static void onUnlock(int size, CountDownLatch latch, int i) {
    CompletableFuture.runAsync(() -> {
      try {
        SECONDS.sleep(size);
        System.out.println("Unlocked: " + i);
        latch.countDown();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }, service);
  }

  private static void onIteration(int i) {
    CompletableFuture.runAsync(() -> {
      try {
        System.out.println("Running iteration: " + i);
        SECONDS.sleep(i);
        System.out.println("Iteration: " + i);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }, service);
  }
}
