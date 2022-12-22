package it.auties.whatsapp.test;

import it.auties.whatsapp.github.GithubActions;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

public class TokensCollectorTest {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final String SOURCE_NAME = "Tokens.java";
    private static final String TOKEN_REGEX = "<script defer=\"defer\" src=\"/app.([^\"]*).js\">";
    private static final String SINGLE_BYTE_REGEX = "t.SINGLE_BYTE_TOKEN=\\[\"(.*?)\"]";
    private static final String DICTIONARY_0_REGEX = "const n=\\[\"(.*?)\"]";
    private static final String DICTIONARY_1_REGEX = "const r=\\[\"(.*?)\"]";
    private static final String DICTIONARY_2_REGEX = "const i=\\[\"(.*?)\"]";
    private static final String DICTIONARY_3_REGEX = "const a=\\[\"(.*?)\"]";

    private static HttpRequest createRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "no-cors")
                .header("Sec-Fetch-Dest", "empty")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .GET()
                .build();
    }

    @Test
    public void createClass() throws IOException, InterruptedException {
        if (GithubActions.isActionsEnvironment()) {
            System.out.println("Skipping tokens collector: detected non local environment");
            return;
        }

        System.out.println("Creating tokens class...");
        var javascriptSource = getJavascriptSource();
        var singleByteToken = getSingleByteTokens(javascriptSource);
        var doubleByteTokens = getDoubleByteTokens(javascriptSource);
        var sourceFile = getSourceFile().formatted(singleByteToken, doubleByteTokens);
        Files.writeString(findTokensFile(), sourceFile, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        System.out.printf("Created tokens class at %s%n", findTokensFile());
    }

    @SneakyThrows
    private Path findTokensFile() {
        return Path.of("src/main/java/it/auties/whatsapp/binary/Tokens.java")
                .toAbsolutePath();
    }

    private String getSingleByteTokens(String javascriptSource) {
        return getTokens(javascriptSource, SINGLE_BYTE_REGEX);
    }

    private String getDoubleByteTokens(String javascriptSource) {
        return "%s,%s,%s,%s".formatted(getTokens(javascriptSource, DICTIONARY_0_REGEX),
                getTokens(javascriptSource, DICTIONARY_1_REGEX), getTokens(javascriptSource, DICTIONARY_2_REGEX),
                getTokens(javascriptSource, DICTIONARY_3_REGEX));
    }

    private String getTokens(String source, String regex) {
        return "\"%s\"".formatted(findResult(source, regex));
    }

    private String getSourceFile() throws IOException {
        var sourceStream = ClassLoader.getSystemResourceAsStream(SOURCE_NAME);
        Objects.requireNonNull(sourceStream, "Cannot find source resource at %s".formatted(SOURCE_NAME));
        return new String(sourceStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String getJavascriptSource() throws IOException, InterruptedException {
        var whatsappRequest = createRequest("https://web.whatsapp.com");
        var whatsappResponse = HTTP_CLIENT.send(whatsappRequest, ofString());
        var token = findResult(whatsappResponse.body(), TOKEN_REGEX);
        var sourceRequest = createRequest("https://web.whatsapp.com/app.%s.js".formatted(token));
        return HTTP_CLIENT.send(sourceRequest, ofString())
                .body();
    }

    private String findResult(String input, String regex) {
        return Pattern.compile(regex, Pattern.MULTILINE)
                .matcher(input)
                .results()
                .map(e -> e.group(1))
                .findFirst()
                .orElseThrow();
    }
}
