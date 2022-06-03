package it.auties.whatsapp.ci;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.github.GithubActions;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.utils.ConfigUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class WhatsappSpamTest implements WhatsappListener, JacksonProvider {
    private static Whatsapp api;
    private static ContactJid contact;
    private static ScheduledExecutorService executor;
    
    @BeforeAll
    public void init() throws IOException {
        System.out.println("Initializing api to start testing...");
        executor = newSingleThreadScheduledExecutor();
        createApi();
        api.registerListener(this);
        loadConfig();
    }

    private void createApi() throws IOException {
        if (!GithubActions.isActionsEnvironment()) {
            System.out.println("Detected local environment");
            api = Whatsapp.newConnection();
            return;
        }

        api = Whatsapp.newConnection(
                WhatsappOptions.defaultOptions(),
                loadGithubParameter(GithubActions.STORE_NAME, WhatsappStore.class),
                loadGithubParameter(GithubActions.CREDENTIALS_NAME, WhatsappKeys.class)
        );
    }

    private <T> T loadGithubParameter(String parameter, Class<T> type) throws IOException {
        System.out.println("Detected github actions environment");
        var keysJson = Base64.getDecoder().decode(System.getenv(parameter));
        return JSON.readValue(keysJson, type);
    }

    private void loadConfig() throws IOException {
        if(GithubActions.isActionsEnvironment()) {
            System.out.println("Loading environment variables...");
            contact = ContactJid.of(System.getenv(GithubActions.CONTACT_NAME));
            System.out.println("Loaded environment variables...");
            return;
        }

        System.out.println("Loading configuration file...");
        var props = ConfigUtils.loadConfiguration();
        contact = ContactJid.of(Objects.requireNonNull(props.getProperty("contact"), "Missing contact property in config"));
        System.out.println("Loaded configuration file");
    }

    @Test
    @Order(1)
    public synchronized void testConnection() throws ExecutionException, InterruptedException {
        System.out.println("Connecting...");
        api.connect().get();
        api.await();
        System.out.println("Connected!");
    }

    @Override
    public void onNodeSent(Node outgoing) {
        System.out.printf("Sending node: %s%n", outgoing);
    }

    @Override
    public void onNodeReceived(Node incoming) {
        System.out.printf("Received node: %s%n", incoming);
    }

    @Override
    public void onLoggedIn() {
        System.out.println("On logged in");
        executor.scheduleAtFixedRate(WhatsappSpamTest::sendMessage,
                0L, 500, TimeUnit.MILLISECONDS);
        delayedExecutor(1, TimeUnit.MINUTES)
                .execute(WhatsappSpamTest::logOut);
    }

    private static void sendMessage() {
        System.out.println("Sending message");
        api.sendMessage(contact, "Hello :)");
    }

    private static void logOut() {
        executor.shutdownNow();
        api.disconnect();
    }
}
