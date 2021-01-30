package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.constant.ProtoBuf;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@UtilityClass
public class WhatsappUtils {
    private final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public @NotNull Optional<String> extractText(@NotNull ProtoBuf.Message message) {
        if (message.hasConversation()) {
            return Optional.of(message.getConversation());
        }

        if (message.hasExtendedTextMessage()) {
            return Optional.of(message.getExtendedTextMessage().getText());
        }

        return Optional.empty();
    }

    public @NotNull String parseJid(@NotNull String jid){
        return jid.replaceAll("@c.us", "@s.whatsapp.net");
    }

    public @NotNull String randomId(){
        return BytesArray.random(10).toHex();
    }

    @SneakyThrows
    public static boolean isOnWhatsapp(@NotNull String jid){
        var url = "https://wa.me/%s".formatted(jid.contains("@") ? jid.split("@")[0] : jid);
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();

        var location = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).headers().firstValue("Location");
        if(location.isEmpty()){
            return false;
        }

        var locationUrl = new URL(location.get());
        return locationUrl.getPath().endsWith("send/");
    }
}
