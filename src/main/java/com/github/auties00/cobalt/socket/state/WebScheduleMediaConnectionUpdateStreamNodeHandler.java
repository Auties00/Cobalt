package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.media.MediaConnection;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.socket.SocketStream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.MEDIA_CONNECTION;

public final class WebScheduleMediaConnectionUpdateStreamNodeHandler extends SocketStream.Handler {
    private static final int DEFAULT_MEDIA_CONNECTION_TTL = 300;

    public WebScheduleMediaConnectionUpdateStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        scheduleMediaConnectionUpdate();
    }

    private void scheduleMediaConnectionUpdate() {
        MediaConnection mediaConnection = null;
        try {
            var queryRequestBody = new NodeBuilder()
                    .description("media_conn")
                    .build();
            var queryRequest = new NodeBuilder()
                    .description("iq")
                    .attribute("to", JidServer.user())
                    .attribute("type", "set")
                    .attribute("xmlns", "w:m")
                    .content(queryRequestBody);
            var queryResponse = whatsapp.sendNode(queryRequest);
            var mediaConn = queryResponse.getChild("media_conn")
                    .orElse(queryResponse);
            var auth = mediaConn.getRequiredAttributeAsString("auth");
            var ttl = Math.toIntExact(mediaConn.getRequiredAttributeAsLong("ttl"));
            var maxBuckets = Math.toIntExact(mediaConn.getRequiredAttributeAsLong("max_buckets"));
            var timestamp = System.currentTimeMillis();
            var hosts = mediaConn.streamChildren("host")
                    .map(attributes -> attributes.getRequiredAttributeAsString("hostname"))
                    .toList();
            mediaConnection = new MediaConnection(auth, ttl, maxBuckets, timestamp, hosts);
            whatsapp.store()
                    .setMediaConnection(mediaConnection);
        } catch (Exception throwable) {
            whatsapp.store().setMediaConnection(null);
            whatsapp.handleFailure(MEDIA_CONNECTION, throwable);
        }finally {
            var mediaConnectionTtl = mediaConnection != null ? mediaConnection.ttl() : DEFAULT_MEDIA_CONNECTION_TTL;
            var executor = CompletableFuture.delayedExecutor(mediaConnectionTtl, TimeUnit.SECONDS);
            executor.execute(this::scheduleMediaConnectionUpdate);
        }
    }
}
