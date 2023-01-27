package it.auties.whatsapp.model.request;

import static it.auties.whatsapp.crypto.Handshake.PROLOGUE;
import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.binary.Encoder;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.exception.ErroneousBinaryRequestException;
import it.auties.whatsapp.exception.ErroneousNodeRequestException;
import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.JacksonProvider;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import lombok.NonNull;

/**
 * An abstract model class that represents a request made from the client to the server.
 */
@SuppressWarnings("UnusedReturnValue")
public record Request(String id, @NonNull Object body, @NonNull CompletableFuture<Node> future, Function<Node, Boolean> filter, Throwable caller)
    implements JacksonProvider {

  /**
   * The binary encoder, used to encode requests that take as a parameter a node
   */
  private static final Encoder ENCODER = new Encoder();

  /**
   * The timeout in seconds before a Request wrapping a Node fails
   */
  private static final int TIMEOUT = 60;

  /**
   * The delayed executor used to cancel futures
   */
  private static final Executor EXECUTOR = delayedExecutor(TIMEOUT, SECONDS);

  private Request(String id, Function<Node, Boolean> filter, @NonNull Object body) {
    this(id, body, new CompletableFuture<>(), filter, Exceptions.current());
    EXECUTOR.execute(this::cancelTimedFuture);
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
    var exception = body instanceof Node node ?
        new ErroneousNodeRequestException(
            "Node timed out(%s), no response from WhatsApp".formatted(node), node,
            caller) :
        new ErroneousBinaryRequestException("Binary timed out, no response from WhatsApp", body,
            caller);
    future.completeExceptionally(exception);
  }

  /**
   * Sends a request to the WebSocket linked to {@code session}.
   *
   * @param session the WhatsappWeb's WebSocket session
   * @param store   the store
   */
  public CompletableFuture<Node> sendWithPrologue(@NonNull Session session, @NonNull Keys keys,
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
  public CompletableFuture<Node> send(@NonNull Session session, @NonNull Keys keys,
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
  public CompletableFuture<Void> sendWithNoResponse(@NonNull Session session, @NonNull Keys keys,
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
  public CompletableFuture<Node> send(@NonNull Session session, @NonNull Keys keys,
      @NonNull Store store,
      boolean prologue, boolean response) {
    try {
      var ciphered = encryptMessage(keys);
      var buffer = Bytes.of(prologue ?
              PROLOGUE :
              new byte[0])
          .appendInt(ciphered.length >> 16)
          .appendShort(65535 & ciphered.length)
          .append(ciphered)
          .toNioBuffer();
      session.getAsyncRemote()
          .sendBinary(buffer, result -> handleSendResult(store, result, response));
    } catch (Exception exception) {
      future.completeExceptionally(
          new IOException("Cannot send %s, an unknown exception occurred".formatted(this),
              exception));
    }
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
      future.completeExceptionally(
          new ErroneousNodeRequestException(
              "Cannot process request %s with %s".formatted(this, response), response, caller));
      return true;
    }
    if(filter != null && !filter.apply(response)){
      return false;
    }
    future.complete(response);
    return true;
  }

  private void handleSendResult(Store store, SendResult result, boolean response) {
    if (!result.isOK()) {
      future.completeExceptionally(
          new IOException("Cannot send request %s, erroneous send result".formatted(this),
              result.getException()));
      return;
    }
    if (!response) {
      future.complete(Node.of("stream-error"));
      return;
    }
    store.addRequest(this);
  }

  private byte[] encryptMessage(Keys keys) {
    var encodedBody = body();
    var body = switch (encodedBody) {
      case byte[] bytes -> bytes;
      case Node node -> ENCODER.encode(node);
      default -> throw new IllegalArgumentException(
          "Cannot create request, illegal body: %s".formatted(encodedBody));
    };
    if (keys.writeKey() == null) {
      return body;
    }
    return AesGmc.encrypt(keys.writeCounter(true), body, keys.writeKey().toByteArray());
  }
}
