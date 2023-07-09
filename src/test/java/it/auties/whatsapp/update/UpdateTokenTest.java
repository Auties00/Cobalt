package it.auties.whatsapp.update;

import it.auties.whatsapp.github.GithubActions;
import it.auties.whatsapp.github.GithubSecrets;
import it.auties.whatsapp.util.MetadataHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class UpdateTokenTest {
    @Test
    public void updateCredentials() {
        if (GithubActions.isActionsEnvironment()) {
            System.out.println("Skipping token update: detected non-local environment");
            return;
        }
        System.out.println("Updating token...");
        var home = getHomeDirectory();
        MetadataHelper.setAndroidCache(home.resolve("src/main/resources/token/android"));
        CompletableFuture.allOf(MetadataHelper.downloadWhatsappApk(true), MetadataHelper.downloadWhatsappApk(false))
                        .join();
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