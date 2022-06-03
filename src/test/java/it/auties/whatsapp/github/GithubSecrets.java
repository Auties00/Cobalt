package it.auties.whatsapp.github;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.utils.LibraryLoader;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.utils.ConfigUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

import static java.net.URI.create;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

@Log
@UtilityClass
public class GithubSecrets implements JacksonProvider {
    private final LazySodiumJava SODIUM = new LazySodiumJava(new SodiumJava(LibraryLoader.Mode.BUNDLED_ONLY));
    private final String REQUEST_PATH = "https://api.github.com/repos/Auties00/WhatsappWeb4j";
    private final String PUBLIC_KEY_PATH = "actions/secrets/public-key";
    private final String UPDATE_SECRET_PATH = "actions/secrets/%s".formatted(GithubActions.CREDENTIALS_NAME);

    private final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final Preferences PREFERENCES = Preferences.userRoot();

    public void updateCredentials() throws IOException, InterruptedException {
        var credentials = getCredentialsAsJson();
        var publicKey = getPublicKey();
        var cypheredCredentials = encryptCredentials(publicKey, credentials);
        updateSecret(publicKey.keyId(), cypheredCredentials);
    }

    private String getCredentialsAsJson(){
        return Objects.requireNonNull(PREFERENCES.get("it.auties.whatsapp.manager.WhatsappKeysManager", null), "Missing credentials");
    }

    private byte[] encryptCredentials(GithubKey publicKey, String credentials) {
        var publicKeyBytes = Base64.getDecoder().decode(publicKey.key());
        var messageBytes = Base64.getEncoder().encode(credentials.getBytes());
        var cypher = new byte[messageBytes.length + 48];

        var result = SODIUM.cryptoBoxSeal(cypher, messageBytes, messageBytes.length, publicKeyBytes);
        if(!result){
            throw new IllegalStateException("crypto_box_seal failed");
        }

        return cypher;
    }

    private GithubKey getPublicKey() throws IOException, InterruptedException {
        var request = createPublicKeyRequest();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return JSON.readValue(response.body(), GithubKey.class);
    }

    private HttpRequest createPublicKeyRequest() throws IOException {
        return HttpRequest.newBuilder()
                .GET()
                .uri(create("%s/%s".formatted(REQUEST_PATH, PUBLIC_KEY_PATH)))
                .header("Authorization", "Bearer %s".formatted(loadGithubToken()))
                .header("Accept", "application/vnd.github.v3+json")
                .build();
    }

    private void updateSecret(String keyId, byte[] cypheredCredentials) throws IOException, InterruptedException {
        var request = createUpdateSecretRequest(keyId, cypheredCredentials);
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if(response.statusCode() != 201 && response.statusCode() != 204){
            throw new IllegalStateException("Cannot update credentials with status code %s".formatted(response.statusCode()));
        }

        log.info("Sent credentials");
        log.info("Status code: %s".formatted(response.statusCode()));
        log.info("Response: %s".formatted(response.body()));
    }

    private HttpRequest createUpdateSecretRequest(String keyId, byte[] cypheredCredentials) throws IOException {
        return HttpRequest.newBuilder()
                .PUT(ofString(JSON.writeValueAsString(createUpdateSecretParams(keyId, cypheredCredentials))))
                .uri(create("%s/%s".formatted(REQUEST_PATH, UPDATE_SECRET_PATH)))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", "Bearer %s".formatted(loadGithubToken()))
                .build();
    }

    private Map<String, ?> createUpdateSecretParams(String keyId, byte[] cypheredCredentials) {
        return Map.of(
                "encrypted_value", cypheredCredentials,
                "key_id", keyId
        );
    }

    private String loadGithubToken() throws IOException {
        var config = ConfigUtils.loadConfiguration();
        return Objects.requireNonNull(config.getProperty("github_token"), "Missing github_token in configuration");
    }
}
