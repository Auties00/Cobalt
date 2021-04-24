package it.auties.whatsapp4j;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.response.impl.UserInformationResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

/**
 * A simple class to check that the library is working
 */
@Log
public class WhatsappTest implements WhatsappListener {
  private static @NotNull CountDownLatch latch;
  private static @NotNull WhatsappAPI whatsappAPI;

  @BeforeAll
  public static void init(){
    log.info("Initializing api to start testing...");
    whatsappAPI = new WhatsappAPI();
    latch = new CountDownLatch(1);
  }

  @Test
  public void testConnectionFlowBasic() throws InterruptedException{
      log.info("Registering listener...");
      whatsappAPI.registerListener(this);
      log.info("Connecting...");
      whatsappAPI.connect();
      latch.await();
  }

  @Override
  public void onLoggedIn(UserInformationResponse info) {
    log.info("Connected!");
    whatsappAPI.disconnect();
    latch.countDown();
  }
}