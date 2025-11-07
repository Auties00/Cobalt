package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.media.MediaConnection;
import com.github.auties00.cobalt.media.MediaHost;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.media.MediaPath;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.socket.SocketStream;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
                    .map(this::parseHost)
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

    private MediaHost parseHost(Node host) {
        var type = host.getRequiredAttributeAsString("type");
        return switch (type) {
            case "primary" -> parsePrimaryHost(host);
            case "fallback" -> parseFallbackHost(host);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private MediaHost.Primary parsePrimaryHost(Node host) {
        var hostname = host.getRequiredAttributeAsString("hostname");
        var fallbackHostname = host.getAttributeAsString("fallback_hostname");
        var ip4 = host.getRequiredAttributeAsString("ip4");
        var fallbackIp4 = host.getRequiredAttributeAsString("fallback_ip4");
        var ip6 = host.getRequiredAttributeAsString("ip6");
        var fallbackIp6 = host.getRequiredAttributeAsString("fallback_ip6");
        var downloads = host.streamChild("download")
                .flatMap(Node::streamChildren)
                .flatMap(download -> MediaPath.ofId(download.description()).stream())
                .collect(Collectors.toUnmodifiableSet());
        var uploads = host.hasChild("upload") ? MediaPath.known() : Set.<MediaPath>of();
        return new MediaHost.Primary(hostname, fallbackHostname, ip4, fallbackIp4, ip6, fallbackIp6, downloads, uploads);
    }

    private MediaHost.Fallback parseFallbackHost(Node host) {
        var hostname = host.getRequiredAttributeAsString("hostname");
        return new MediaHost.Fallback(hostname);
    }
}
