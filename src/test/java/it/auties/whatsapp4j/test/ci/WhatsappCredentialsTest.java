package it.auties.whatsapp4j.test.ci;

import it.auties.whatsapp4j.test.github.GithubActions;
import it.auties.whatsapp4j.test.github.GithubSecrets;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Log
public class WhatsappCredentialsTest {
    @Test
    public void updateCredentials() throws IOException, InterruptedException {
        if(GithubActions.isActionsEnvironment()){
            log.info("Skipping credentials update: detected non local environment");
            return;
        }

        log.info("Updating credentials...");
        GithubSecrets.updateCredentials();
        log.info("Updated credentials!");
    }
}
