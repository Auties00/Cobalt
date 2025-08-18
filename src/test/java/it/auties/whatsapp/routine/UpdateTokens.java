package it.auties.whatsapp.routine;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp.util.Medias;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

public class UpdateTokens {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private static final String SOURCE_NAME = "BinaryTokens.java";
    private static final String TOKEN_REGEX = "<script defer=\"defer\" src=\"/app.([^\"]*).js\">";
    private static final String SINGLE_BYTE_REGEX = "t.SINGLE_BYTE_TOKEN=\\[\"(.*?)\"]";
    private static final String DICTIONARY_ASSIGNMENT_REGEX = "\\.DICTIONARY_[0-9]_TOKEN=([a-z]);";
    private static final String DICTIONARY_DECLARATION_REGEX = "const %s=\\[\"(.*?)\"]";
    private static final String PROPS_REGEX = "\\.ABPropConfigs=\\{(.*?)]}},";

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Creating tokens class...");
        var javascriptSource = getJavascriptSource();
        var singleByteToken = getSingleByteTokens(javascriptSource);
        var doubleByteTokens = getDoubleByteTokens(javascriptSource);
        var props = getAbPropsList(javascriptSource);
        var sourceFile = getSourceFile().formatted(singleByteToken, doubleByteTokens, props);
        Files.writeString(findTokensFile(), sourceFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.printf("Created tokens class at %s%n", findTokensFile());
    }

    private static HttpRequest createRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "no-cors")
                .header("Sec-Fetch-Dest", "empty")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .GET()
                .build();
    }

    private static Path findTokensFile() {
        return Path.of("src/main/java/it/auties/whatsapp/binary/BinaryTokens.java").toAbsolutePath();
    }

    private static String getSingleByteTokens(String javascriptSource) {
        return '"' + findResult(javascriptSource, SINGLE_BYTE_REGEX) + '"';
    }

    private static String getDoubleByteTokens(String javascriptSource) {
        return Pattern.compile(DICTIONARY_ASSIGNMENT_REGEX, Pattern.MULTILINE)
                .matcher(javascriptSource)
                .results()
                .map(result -> result.group(1))
                .map(letter -> '"' + findResult(javascriptSource, DICTIONARY_DECLARATION_REGEX.formatted(letter)) + '"')
                .collect(Collectors.joining(", "));
    }

    private static String getAbPropsList(String source) {
       try {
           var props = findResult(source, PROPS_REGEX) + "]";
           var json = '{' + props.replaceAll("!0", "true").replaceAll("!1", "false") + '}';
           return new ObjectMapper().enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                   .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                   .reader()
                   .forType(new TypeReference<Map<String, List<Object>>>() {})
                   .with(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                   .<Map<String, List<Object>>>readValue(json)
                   .entrySet()
                   .stream()
                   .map(entry -> {
                       var code = entry.getValue().get(0);
                       var type = entry.getValue().get(1);
                       var on = parseValue(entry.getValue().get(2), type);
                       var off = parseValue(entry.getValue().get(3), type);
                       var value =  "new BinaryProperty(\"%s\", %s, %s, %s)".formatted(entry.getKey(), code, on, off);
                       return "        properties.put(%s, %s);".formatted((int) Double.parseDouble(code.toString()), value);
                   })
                   .collect(Collectors.joining("\n"));
       }catch (IOException exception) {
           throw new UncheckedIOException("Cannot read json", exception);
       }
    }

    private static Object parseValue(Object value, Object type) {
        if (!type.equals("string")) {
            return value;
        }

        var string = value.toString();
        if(string.contains("\"")) {
            return "\"\"\"\n%s\"\"\"".formatted(string);
        }

        return '"' + string.replaceAll("\\\\/", "\\\\\\\\/").replaceAll("\\\\\\.", "\\\\\\\\.") + '"';
    }

    private static String getSourceFile() throws IOException {
        var sourceStream = ClassLoader.getSystemResourceAsStream(SOURCE_NAME);
        Objects.requireNonNull(sourceStream, "Cannot find source resource at %s".formatted(SOURCE_NAME));
        return new String(sourceStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String getJavascriptSource() throws IOException, InterruptedException {
        var whatsappRequest = createRequest(Medias.WEB_ORIGIN);
        var whatsappResponse = HTTP_CLIENT.send(whatsappRequest, ofString());
        var token = findResult(whatsappResponse.body(), TOKEN_REGEX);
        var sourceRequest = createRequest("%s/app.%s.js".formatted(Medias.WEB_ORIGIN, token));
        return HTTP_CLIENT.send(sourceRequest, ofString()).body();
    }

    private static String findResult(String input, String regex) {
        return Pattern.compile(regex, Pattern.MULTILINE)
                .matcher(input)
                .results()
                .map(e -> e.group(1))
                .findFirst()
                .orElseThrow();
    }
}
