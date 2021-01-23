package it.auties.whatsapp4j.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@UtilityClass
public class WhatsappContactUtils {
    private final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @SneakyThrows
    public static boolean isOnWhatsapp(@NotNull String check){
        var url = "https://wa.me/%s".formatted(check.contains("@") ? check.split("@")[0] : check);
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
