package it.auties.whatsapp4j.test.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.test.sodium.Sodium;
import it.auties.whatsapp4j.test.utils.ConfigUtils;
import it.auties.whatsapp4j.test.utils.SodiumUtils;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URISyntaxException;
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
public class GithubSecrets {
    private final String REQUEST_PATH = "https://api.github.com/repos/Auties00/WhatsappWeb4j";
    private final String PUBLIC_KEY_PATH = "actions/secrets/public-key";
    private final String UPDATE_SECRET_PATH = "actions/secrets/%s".formatted(GithubVariables.CREDENTIALS_NAME);

    private final Sodium SODIUM = SodiumUtils.loadLibrary();
    private final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final Preferences PREFERENCES = Preferences.userRoot();
    private final ObjectMapper JACKSON = new ObjectMapper();

    public void updateCredentials() throws IOException, InterruptedException {
        var credentials = getCredentialsAsJson();
        var publicKey = getPublicKey();
        var cypheredCredentials = encryptCredentials(publicKey, credentials);
        updateSecret(publicKey.keyId(), cypheredCredentials);
    }

    private String getCredentialsAsJson(){
        return PREFERENCES.get(WhatsappKeysManager.PREFERENCES_PATH, "empty");
    }

    private String encryptCredentials(GithubKey publicKey, String credentials) {
        SodiumUtils.loadLibrary();

        var publicKeyBytes = publicKey.key().getBytes();
        var messageBytes = credentials.getBytes();
        var cypher = new byte[messageBytes.length + 48];

        var result = SODIUM.crypto_box_seal(cypher, messageBytes, messageBytes.length, publicKeyBytes);
        Validate.isTrue(result == 0, "crypto_box_seal failed");

        return Base64.getEncoder().encodeToString(cypher);
    }

    private GithubKey getPublicKey() throws IOException, InterruptedException {
        var request = createPublicKeyRequest();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return JACKSON.readValue(response.body(), GithubKey.class);
    }

    private HttpRequest createPublicKeyRequest() throws IOException {
        return HttpRequest.newBuilder()
                .GET()
                .uri(create("%s/%s".formatted(REQUEST_PATH, PUBLIC_KEY_PATH)))
                .header("Authorization", "Bearer %s".formatted(loadGithubToken()))
                .header("Accept", "application/vnd.github.v3+json")
                .build();
    }

    private void updateSecret(String keyId, String cypheredCredentials) throws IOException, InterruptedException {
        var request = createUpdateSecretRequest(keyId, cypheredCredentials);
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Sent credentials");
        log.info("Status code: %s".formatted(response.statusCode()));
        log.info("Response: %s".formatted(response.body()));
        Validate.isTrue(response.statusCode() == 201 || response.statusCode() == 204, "Cannot update credentials");
    }

    private HttpRequest createUpdateSecretRequest(String keyId, String cypheredCredentials) throws IOException {
        return HttpRequest.newBuilder()
                .PUT(ofString(JACKSON.writeValueAsString(createUpdateSecretParams(keyId, cypheredCredentials))))
                .uri(create("%s/%s".formatted(REQUEST_PATH, UPDATE_SECRET_PATH)))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", "Bearer %s".formatted(loadGithubToken()))
                .build();
    }

    private Map<String, String> createUpdateSecretParams(String keyId, String cypheredCredentials) {
        return Map.of(
                "encrypted_value", cypheredCredentials,
                "key_id", keyId
        );
    }

    private String loadGithubToken() throws IOException {
        var config = ConfigUtils.loadConfiguration();
        return Objects.requireNonNull(config.getProperty("github_token"), "Missing github token in configuration");
    }
}
