package it.auties.whatsapp.ci;

import it.auties.whatsapp.github.GithubActions;
import it.auties.whatsapp.github.GithubSecrets;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GithubCredentialsTest {
    @Test
    public void updateCredentials() throws IOException, InterruptedException {
        if (GithubActions.isActionsEnvironment()) {
            System.out.println("Skipping credentials update: detected non local environment");
            return;
        }

        System.out.println("Updating credentials...");
        GithubSecrets.updateCredentials();
        System.out.println("Updated credentials!");
    }
}