package org.example.whatsapp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.standard.TextMessage;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

// This is the main class of our bot
public class OpenAIBot {
    // Constants
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
            
    // Things we need to contact the openai api
    private static final Semaphore semaphore = new Semaphore(1);
    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final AtomicReference<String> sessionId = new AtomicReference<>();
    private static final AtomicReference<String> parentMessageId = new AtomicReference<>();
    private static final String bearerToken = System.getenv("openai_token");
    private static final String cfToken = System.getenv("openai_cf");

    public static void main(String... args) throws ExecutionException, InterruptedException {
        Whatsapp.lastConnection()
                .addLoggedInListener(() -> System.out.println("Connected!"))
                .addNewMessageListener(OpenAIBot::onNewMessage)
                .connect()
                .get();
    }

    private static void onNewMessage(Whatsapp api, MessageInfo info) {
        if (!(info.message().content() instanceof TextMessage textMessage)) {
            return;
        }

        if(!textMessage.text().contains("/ai") || info.chat().name().toLowerCase().contains("denis")){
            return;
        }

        try {
            semaphore.acquire();
            var messageId = generateId();
            var lastMessageId = parentMessageId.getAndSet(messageId);
            System.out.printf("Last message id: %s%n", lastMessageId);
            var openAiRequest = new ChatRequest(
                    "next",
                    List.of(
                            new ChatMessage(
                                   messageId,
                                   "user",
                                   Map.of(
                                           "content_type", "text",
                                           "parts", List.of(textMessage.text().replace("/ai", ""))
                                   )
                            )
                    ),
                    sessionId.get(),
                    Objects.requireNonNullElse(lastMessageId, generateId()),
                    "text-davinci-002-render"
            );
            var cookieHandler = new CookieManager();
            cookieHandler.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(10))
                    .cookieHandler(cookieHandler)
                    .build(); // Avoid rate limiting
            var uri = URI.create("https://chat.openai.com/backend-api/conversation");
            var cookie = new HttpCookie("cf_clearance", cfToken);
            cookie.setDomain(".chat.openai.com");
            cookie.setPath("/");
            cookie.setVersion(0);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setMaxAge(1800);
            cookieHandler.getCookieStore().add(URI.create("http://chat.openai.com"), cookie);

            var httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(ofString(jsonMapper.writeValueAsString(openAiRequest)))
                    .header("User-Agent", USER_AGENT)
                    .header("Host", "chat.openai.com")
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .header("Authorization", "Bearer %s".formatted(bearerToken))
                    .build();
            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if(response.statusCode() != 200){
                api.sendMessage(info.chatJid(),
                        "An error occurred while querying OpenAI: %s(status code %s)".formatted(response.body(),
                                response.statusCode()), info);
                return;
            }

            try(var body = response.body()){
                var jsonBody = new String(body.readAllBytes(), StandardCharsets.UTF_8);
                var jsonLastPart = Pattern.compile("data:([\\S\\s]*?)data:", Pattern.MULTILINE)
                        .matcher(jsonBody)
                        .results()
                        .reduce((first, second) -> second)
                        .orElseThrow()
                        .group(1);
                var chatResponse = jsonMapper.readValue(jsonLastPart, ChatRequest.class);
                sessionId.set(chatResponse.conversationId());
                if(chatResponse.messages().isEmpty()){
                    api.sendMessage(info.chatJid(), "An error occurred while querying OpenAI: the question cannot be answered", info);
                    return;
                }

                var chatMessageResponse = chatResponse.messages().get(0);
                var chatMessageResponseParts = (List<?>) chatMessageResponse.content().get("parts");
                if(chatMessageResponseParts == null || chatMessageResponseParts.isEmpty()){
                    api.sendMessage(info.chatJid(), "An error occurred while querying OpenAI: the question cannot be answered", info);
                    return;
                }

                var message = chatMessageResponseParts.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining())
                        .trim();
                api.sendMessage(info.chatJid(), message, info);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            api.sendMessage(info.chatJid(),
                    "An error occurred while querying OpenAI: %n%s".formatted(ex.getMessage()), info);
        }finally {
            semaphore.release();
        }
    }

    private static String generateId() {
        return UUID.randomUUID()
                .toString()
                .toLowerCase(Locale.ROOT);
    }
}