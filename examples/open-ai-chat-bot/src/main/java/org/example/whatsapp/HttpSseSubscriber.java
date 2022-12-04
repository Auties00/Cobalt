package org.example.whatsapp;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpSseSubscriber implements HttpResponse.BodySubscriber<LinkedList<String>> {
    private final CompletableFuture<LinkedList<String>> future;
    private final LinkedList<String> result;
    private final Consumer<String> update;
    private Subscription subscription;
    public HttpSseSubscriber() {
        this(null);
    }

    public HttpSseSubscriber(Consumer<String> update) {
        this.future = new CompletableFuture<>();
        this.result = new LinkedList<>();
        this.update = update;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        try {
            this.subscription.request(1);
        } catch (Exception exception) {
            this.future.completeExceptionally(exception);
            this.subscription.cancel();
        }
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        try {
            var input = buffers.stream()
                    .map(buffer -> UTF_8.decode(buffer).toString())
                    .collect(Collectors.joining())
                    .trim();
            if (input.startsWith("data: ")) {
                var toAdd = input.replaceFirst("data: ", "");
                if(!toAdd.equalsIgnoreCase("[done]")) {
                    result.add(toAdd);
                }
            } else if(result.isEmpty()) {
                result.add(input);
            }else {
                result.set(result.size() - 1, result.get(result.size() - 1) + input);
            }

            if (update != null) {
                update.accept(input);
            }

            this.subscription.request(1);
        } catch (Throwable exception) {
            this.future.completeExceptionally(exception);
            this.subscription.cancel();
        }
    }

    @Override
    public void onError(Throwable exception) {
        future.completeExceptionally(exception);
    }

    @Override
    public void onComplete() {
        future.complete(result);
    }

    @Override
    public CompletionStage<LinkedList<String>> getBody() {
        return future;
    }
}
