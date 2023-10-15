package it.auties.whatsapp.local;

import it.auties.whatsapp.model.GithubActions;
import it.auties.whatsapp.model.GithubSecrets;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UpdateGPGTest {
    @Test
    public void updateCredentials() throws IOException {
        if (GithubActions.isActionsEnvironment()) {
            System.out.println("Skipping credentials update: detected non local environment");
            return;
        }
        System.out.println("Updating credentials...");
        GithubSecrets.updateCredentials();
        System.out.println("Updated credentials!");
    }
}