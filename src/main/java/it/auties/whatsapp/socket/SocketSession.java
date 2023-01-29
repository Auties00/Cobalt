package it.auties.whatsapp.socket;

import static it.auties.whatsapp.util.Specification.Whatsapp.APP_ENDPOINT_HOST;
import static it.auties.whatsapp.util.Specification.Whatsapp.APP_ENDPOINT_PORT;
import static it.auties.whatsapp.util.Specification.Whatsapp.WEB_ENDPOINT;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.socket.SocketSession.AppSocketSession;
import it.auties.whatsapp.socket.SocketSession.WebSocketSession;
import it.auties.whatsapp.socket.SocketSession.WebSocketSession.OriginPatcher;
import it.auties.whatsapp.util.Specification;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.NonNull;

public sealed interface SocketSession permits WebSocketSession, AppSocketSession {
  CompletableFuture<Void> connect(SocketListener listener);
  CompletableFuture<Void> close();
  CompletableFuture<Void> sendBinary(byte[] bytes);
  boolean isOpen();

  static SocketSession of(ClientType type){
    return switch (type){
      case WEB_CLIENT -> new WebSocketSession();
      case APP_CLIENT -> new AppSocketSession();
    };
  }

  @ClientEndpoint(configurator = OriginPatcher.class)
  final class WebSocketSession implements SocketSession {
    private Session session;
    private SocketListener listener;

    @Override
    public CompletableFuture<Void> connect(SocketListener listener) {
      return CompletableFuture.runAsync(() ->{
        try {
          this.listener = listener;
          ContainerProvider.getWebSocketContainer()
              .connectToServer(this, WEB_ENDPOINT);
        }catch (IOException exception){
          throw new UncheckedIOException("Cannot connect to host", exception);
        }catch (DeploymentException exception){
          throw new RuntimeException(exception);
        }
      });
    }

    @Override
    public CompletableFuture<Void> close() {
      return CompletableFuture.runAsync(() -> {
        try {
          session.close();
        }catch (IOException exception){
          throw new UncheckedIOException("Cannot close connection to host", exception);
        }
      });
    }

    @Override
    public CompletableFuture<Void> sendBinary(byte[] bytes) {
      var future = new CompletableFuture<Void>();
      try {
        session.getAsyncRemote().sendBinary(ByteBuffer.wrap(bytes), result -> {
          if(result.isOK()){
            future.complete(null);
            return;
          }

          future.completeExceptionally(result.getException());
        });
      }catch (Throwable throwable){
        future.completeExceptionally(throwable);
      }

      return future;
    }

    @Override
    public boolean isOpen() {
      return session == null || session.isOpen();
    }

    @OnOpen
    @SuppressWarnings("unused")
    public void onOpen(Session session) {
      this.session = session;
      listener.onOpen(this);
    }

    @OnClose
    @SuppressWarnings("unused")
    public void onClose() {
      listener.onClose();
    }

    @OnError
    @SuppressWarnings("unused")
    public void onError(Throwable throwable) {
      listener.onError(throwable);
    }

    @OnMessage
    @SuppressWarnings("unused")
    public void onBinary(byte[] message){
      listener.onMessage(message);
    }

    public static class OriginPatcher extends Configurator {
      @Override
      public void beforeRequest(@NonNull Map<String, List<String>> headers) {
        headers.put("Origin", List.of(Specification.Whatsapp.WEB_ORIGIN));
        headers.put("Host", List.of(Specification.Whatsapp.WEB_HOST));
      }
    }
  }

  final class AppSocketSession implements SocketSession{
    private static final int MAX_READ_SIZE = 65535;

    private Socket socket;
    private ExecutorService service;
    private SocketListener listener;

    @Override
    public CompletableFuture<Void> connect(SocketListener listener) {
      return CompletableFuture.runAsync(() -> {
        try {
          this.listener = listener;
          this.socket = new Socket();
          socket.setKeepAlive(true);
          socket.connect(new InetSocketAddress(APP_ENDPOINT_HOST, APP_ENDPOINT_PORT));
          this.service = Executors.newSingleThreadScheduledExecutor();
          service.execute(this::readMessages);
          listener.onOpen(this);
        }catch (IOException exception){
          throw new UncheckedIOException("Cannot connect to host", exception);
        }
      });
    }

    @Override
    public CompletableFuture<Void> close() {
      return CompletableFuture.runAsync(() -> {
        try {
          socket.close();
          service.shutdownNow();
          listener.onClose();
        }catch (IOException exception){
          throw new UncheckedIOException("Cannot close connection to host", exception);
        }
      });
    }

    @Override
    public CompletableFuture<Void> sendBinary(byte[] bytes) {
      return CompletableFuture.runAsync(() -> {
        try {
          var stream = socket.getOutputStream();
          stream.write(bytes);
          stream.flush();
        }catch (IOException exception){
          throw new UncheckedIOException("Cannot send message", exception);
        }
      });
    }

    @Override
    public boolean isOpen() {
      return !socket.isClosed();
    }

    private void readMessages(){
      var bytes = new byte[MAX_READ_SIZE];
      while (isOpen()){
        try {
          var stream = socket.getInputStream();
          var size = stream.read(bytes);
          listener.onMessage(Arrays.copyOf(bytes, size));
        }catch (IOException exception){
          listener.onError(exception);
        }
      }

      listener.onClose();
    }
  }
}
