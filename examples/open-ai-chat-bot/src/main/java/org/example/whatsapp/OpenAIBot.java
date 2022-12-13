package org.example.whatsapp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.standard.TextMessage;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.net.URI.create;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

// This is the main class of our bot
public class OpenAIBot {
    // Constants
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36";
            
    // Things we need to contact the openai api
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager())
            .build(); // Avoid rate limiting
    private static final Semaphore semaphore = new Semaphore(1);
    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final AtomicReference<String> sessionId = new AtomicReference<>();
    private static final AtomicReference<String> parentMessageId = new AtomicReference<>();
    private static final String bearerToken = System.getenv("openai_token");

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
            var httpRequest = HttpRequest.newBuilder()
                    .uri(create("https://chat.openai.com/backend-api/conversation"))
                    .POST(ofString(jsonMapper.writeValueAsString(openAiRequest)))
                    .header("User-Agent", USER_AGENT)
                    .header("Host", "chat.openai.com")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .header("Authorization", "Bearer %s".formatted(bearerToken))
                    .build();
            var response = httpClient.send(httpRequest, request -> new HttpSseSubscriber());
            if(response.statusCode() != 200){
                api.sendMessage(info.chatJid(),
                        "An error occurred while querying OpenAI: %s(status code %s)".formatted(response.body(),
                                response.statusCode()), info);
                return;
            }

            var jsonLastPart = response.body().peekLast();
            if(jsonLastPart == null){
                api.sendMessage(info.chatJid(), "An error occurred while querying OpenAI: the question cannot be answered", info);
                return;
            }

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