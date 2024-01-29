package it.auties.whatsapp.local;

import it.auties.whatsapp.model.GithubActions;
import it.auties.whatsapp.registration.MobileMetadata;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class UpdateMobileTokensTest {
    @Test
    public void updateCredentials() {
        if (GithubActions.isActionsEnvironment()) {
            System.out.println("Skipping token update: detected non-local environment");
            return;
        }
        System.out.println("Updating token...");
        var home = getHomeDirectory();
        MobileMetadata.setAndroidCache(home.resolve("src/main/resources/token/android"));
        CompletableFuture.allOf(MobileMetadata.downloadWhatsappApk(true), MobileMetadata.downloadWhatsappApk(false)).join();
        System.out.println("Updated token!");
    }

    private Path getHomeDirectory() {
        var cobalt = Path.of("").toAbsolutePath();
        while (!hasSrcChild(cobalt)){
            cobalt = cobalt.getParent();
        }

        return cobalt;
    }

    private boolean hasSrcChild(Path cobalt) {
        if(cobalt == null) {
            return false;
        }

        var srcPath = cobalt.resolve("src");
        return Files.exists(srcPath);
    }
}