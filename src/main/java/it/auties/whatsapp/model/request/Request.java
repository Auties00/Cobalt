package it.auties.whatsapp.model.request;

import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.binary.Encoder;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.socket.SocketSession;
import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.JacksonProvider;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import lombok.NonNull;

/**
 * An abstract model class that represents a request made from the client to the server.
 */
@SuppressWarnings("UnusedReturnValue")
public record Request(String id, @NonNull Object body, @NonNull CompletableFuture<Node> future,
                      Function<Node, Boolean> filter, Throwable caller)
    implements JacksonProvider {

  /**
   * The timeout in seconds before a Request wrapping a Node fails
   */
  private static final int TIMEOUT = 60;

  /**
   * The delayed executor used to cancel futures
   */
  private static final Executor EXECUTOR = delayedExecutor(TIMEOUT, SECONDS);

  private Request(String id, Function<Node, Boolean> filter, @NonNull Object body) {
    this(id, body, new CompletableFuture<>(), filter,
        Exceptions.current(getTimeoutMessage(body)));
    EXECUTOR.execute(this::cancelTimedFuture);
  }

  private static String getTimeoutMessage(Object body) {
    return body instanceof Node node ? "Node timed out(%s), no response from WhatsApp".formatted(node)
        : "Binary timed out, no response from WhatsApp";
  }

  /**
   * Constructs a new request with the provided body expecting a response
   */
  public static Request of(@NonNull Node body, Function<Node, Boolean> filter) {
    return new Request(body.id(), filter, body);
  }

  /**
   * Constructs a new request with the provided body expecting a response
   */
  public static Request of(@NonNull Object body) {
    try {
      return new Request(null, null, PROTOBUF.writeValueAsBytes(body));
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot encode %s".formatted(body), exception);
    }
  }

  private void cancelTimedFuture() {
    if (future.isDone()) {
      return;
    }
    future.completeExceptionally(caller);
  }

  /**
   * Sends a request to the WebSocket linked to {@code session}.
   *
   * @param session the WhatsappWeb's WebSocket session
   * @param store   the store
   */
  public CompletableFuture<Node> sendWithPrologue(@NonNull SocketSession session, @NonNull Keys keys,
      @NonNull Store store) {
    return send(session, keys, store, true, false);
  }

  /**
   * Sends a request to the WebSocket linked to {@code session}.
   *
   * @param store   the store
   * @param session the WhatsappWeb's WebSocket session
   * @return this request
   */
  public CompletableFuture<Node> send(@NonNull SocketSession session, @NonNull Keys keys,
      @NonNull Store store) {
    return send(session, keys, store, false, true);
  }

  /**
   * Sends a request to the WebSocket linked to {@code session}.
   *
   * @param store   the store
   * @param session the WhatsappWeb's WebSocket session
   * @return this request
   */
  public CompletableFuture<Void> sendWithNoResponse(@NonNull SocketSession session, @NonNull Keys keys,
      @NonNull Store store) {
    return send(session, keys, store, false, false)
        .thenRunAsync(() -> {});
  }

  /**
   * Sends a request to the WebSocket linked to {@code session}.
   *
   * @param store    the store
   * @param session  the WhatsappWeb's WebSocket session
   * @param prologue whether the prologue should be prepended to the request
   * @param response whether the request expects a response
   * @return this request
   */
  public CompletableFuture<Node> send(@NonNull SocketSession session, @NonNull Keys keys, @NonNull Store store, boolean prologue, boolean response) {
    var ciphered = encryptMessage(keys);
    var buffer = Bytes.of(prologue ? keys.prologue() : new byte[0])
        .appendInt(ciphered.length >> 16)
        .appendShort(65535 & ciphered.length)
        .append(ciphered)
        .toByteArray();
    session.sendBinary(buffer)
        .thenRunAsync(() -> {
          if (!response) {
            future.complete(null);
            return;
          }

          store.addRequest(this);
        })
        .exceptionallyAsync(throwable -> {
          future.completeExceptionally(new IOException("Cannot send %s, an unknown exception occurred".formatted(this), throwable));
          return null;
        });
    return future;
  }

  /**
   * Completes this request using {@code response}
   *
   * @param response the response used to complete {@link Request#future}
   */
  public boolean complete(Node response, boolean exceptionally) {
    if (response == null) {
      future.complete(Node.of("xmlstreamend"));
      return true;
    }
    if (exceptionally) {
      future.completeExceptionally(new RuntimeException("Cannot process request %s with %s".formatted(this, response), caller));
      return true;
    }
    if (filter != null && !filter.apply(response)) {
      return false;
    }
    future.complete(response);
    return true;
  }

  private byte[] encryptMessage(Keys keys) {
    var encodedBody = body();
    var body = switch (encodedBody) {
      case byte[] bytes -> bytes;
      case Node node -> {
        var encoder = new Encoder(keys.clientType());
        yield encoder.encode(node);
      }
      default -> throw new IllegalArgumentException(
          "Cannot create request, illegal body: %s".formatted(encodedBody));
    };
    if (keys.writeKey() == null) {
      return body;
    }
    return AesGmc.encrypt(keys.writeCounter(true), body, keys.writeKey().toByteArray());
  }
}
