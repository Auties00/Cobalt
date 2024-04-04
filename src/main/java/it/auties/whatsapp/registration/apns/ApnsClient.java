package it.auties.whatsapp.registration.apns;

import com.dd.plist.NSDictionary;
import it.auties.whatsapp.crypto.Sha1;
import it.auties.whatsapp.registration.http.HttpClient;
import it.auties.whatsapp.util.Specification;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ApnsClient {
    private static final long PING_INTERVAL = 5;
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("<Protocol>([^*]+)</Protocol>", Pattern.MULTILINE);
    private static final byte[] FAIRPLAY_CERT_CHAIN = Base64.getDecoder().decode("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");
    private static final int PORT = 5223;

    private final HttpClient httpClient;
    private final Proxy proxy;
    private final KeyPair keyPair;
    private final Set<ApnsListener> listeners;
    private final List<ApnsPacket> unhandledPackets;
    private final CompletableFuture<Void> loginFuture;
    private final ScheduledExecutorService pingExecutor;
    private SSLSocket socket;
    private byte[] certificate;
    private byte[] authToken;
    private boolean disconnected;

    public ApnsClient(HttpClient httpClient, Proxy proxy) {
        this.httpClient = httpClient;
        this.proxy = proxy;
        this.keyPair = initRSAKeyPair();
        this.listeners = ConcurrentHashMap.newKeySet();
        this.unhandledPackets = new CopyOnWriteArrayList<>();
        this.pingExecutor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());
        this.loginFuture = login();
    }

    private KeyPair initRSAKeyPair() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        }catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Missing algorithm: RSA", exception);
        }
    }

    private CompletableFuture<Void> login() {
        return generatePushCert()
                .thenComposeAsync(ignored -> getAPNSBag())
                .thenComposeAsync(this::authenticate)
                .thenRunAsync(this::setFilters);
    }

    private CompletableFuture<Void> generatePushCert() {
        return getActivationBody().thenComposeAsync(activationBody -> {
            var endpoint = URI.create("https://albert.apple.com/WebObjects/ALUnbrick.woa/wa/deviceActivation");
            var body = "device=Windows&activation-info=" + URLEncoder.encode(activationBody, StandardCharsets.UTF_8);
            var headers = Map.of("Content-Type", "application/x-www-form-urlencoded");
            return httpClient.post(endpoint, proxy,  headers, body.getBytes()).thenAcceptAsync(response -> {
                var protocol = PROTOCOL_PATTERN.matcher(new String(response))
                        .results()
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("Missing result"))
                        .group(1)
                        .getBytes(StandardCharsets.UTF_8);
                var deviceInfo = DeviceActivationInfo.ofPlist(protocol);
                this.certificate = deviceInfo.activationRecord().deviceCertificate();
            });
        });
    }

    private CompletableFuture<String> getActivationBody() {
        return CompletableFuture.supplyAsync(() -> {
            var csr = ApnsCrypto.generateCSR(keyPair);
            var activationInfoDictionary = new NSDictionary();
            activationInfoDictionary.put("ActivationRandomness", UUID.randomUUID().toString());
            activationInfoDictionary.put("ActivationState", "Unactivated");
            activationInfoDictionary.put("BuildVersion", "10.6.4");
            activationInfoDictionary.put("DeviceCertRequest", csr);
            activationInfoDictionary.put("DeviceClass", "Windows");
            activationInfoDictionary.put("ProductType", "windows1,1");
            activationInfoDictionary.put("ProductVersion", "10.6.4");
            activationInfoDictionary.put("SerialNumber", "WindowSerial");
            activationInfoDictionary.put("UniqueDeviceID", UUID.randomUUID().toString());
            var activationInfo = activationInfoDictionary.toXMLPropertyList()
                    .getBytes(StandardCharsets.UTF_8);
            var activationSignature = ApnsCrypto.getActivationSignature(activationInfo);
            var activationInfoBody = new NSDictionary();
            activationInfoBody.put("ActivationInfoComplete", true);
            activationInfoBody.put("ActivationInfoXML", activationInfo);
            activationInfoBody.put("FairPlayCertChain", FAIRPLAY_CERT_CHAIN);
            activationInfoBody.put("FairPlaySignature", activationSignature);
            return activationInfoBody.toXMLPropertyList();
        });
    }

    private CompletableFuture<ApnsBag> getAPNSBag() {
        return httpClient.get(URI.create("http://init-p01st.push.apple.com/bag"), proxy)
                    .thenApplyAsync(ApnsBag::ofPlist);
    }

    private void setFilters() {
        var hashes = Arrays.stream(Specification.Whatsapp.DEFAULT_APNS_FILTERS)
                .map(Sha1::calculate)
                .toArray(byte[][]::new);
        send(ApnsPayloadTag.FILTER, Map.of(
                1, authToken,
                2, hashes
        ));
    }

    private CompletableFuture<Void> authenticate(ApnsBag bag) {
        var nonce = ApnsCrypto.createNonce();
        var signature = ApnsCrypto.createNonceSignature(keyPair, nonce);
        createSocketConnection(bag);
        var certificateBytes = ApnsCrypto.getCertificateBytes(certificate);
        readIncomingMessages();
        send(ApnsPayloadTag.CONNECT, Map.of(
                0x2, new byte[]{0x01},
                0x5, new byte[]{0, 0, 0, 65},
                0xc, certificateBytes,
                0xd, nonce,
                0xe, signature
        ));
        return waitForPacketDirect(packet -> packet.tag() == ApnsPayloadTag.READY).thenAccept(packet -> {
            var statusBuffer = ByteBuffer.wrap(packet.fields().get(0x1));
            var statusCode = Byte.toUnsignedInt(statusBuffer.get());
            if(statusCode != 0) {
                throw new IllegalStateException("Connection failed: " + statusCode);
            }

            onLoggedIn(packet);
            schedulePing();
        });
    }

    private void onLoggedIn(ApnsPacket packet) {
        this.authToken = packet.fields().get(0x3);
        send(ApnsPayloadTag.STATE, Map.of(
                1, new byte[]{1},
                2, new byte[]{127, -1, -1, -1}
        ));
    }

    private void schedulePing() {
        pingExecutor.scheduleAtFixedRate(
                () -> send(ApnsPayloadTag.KEEP_ALIVE_SEND, Map.of()),
                PING_INTERVAL,
                PING_INTERVAL,
                TimeUnit.SECONDS
        );
    }

    private void createSocketConnection(ApnsBag bag) {
        try {
            var sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, new TrustManager[]{new AppleTrustManager()}, null);
            var sslParameters = sslContext.getDefaultSSLParameters();
            sslParameters.setApplicationProtocols(new String[]{"apns-security-v3"});
            var sslSocketFactory = sslContext.getSocketFactory();
            var underlyingSocket = proxy == null ? new Socket() : new Socket(proxy);
            var endpoint = ThreadLocalRandom.current().nextInt(1, bag.hostCount()) + "-" + bag.hostname();
            underlyingSocket.connect(new InetSocketAddress(endpoint, PORT));
            this.socket = (SSLSocket) sslSocketFactory.createSocket(underlyingSocket, endpoint, PORT, true);
            socket.setSoTimeout((int) Duration.ofMinutes(5).toMillis());
            socket.setSSLParameters(sslParameters);
            socket.startHandshake();
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }

    public CompletableFuture<ApnsPacket> waitForPacket(Function<ApnsPacket, Boolean> listener) {
        return loginFuture.thenComposeAsync(ignored -> waitForPacketDirect(listener));
    }

    private CompletableFuture<ApnsPacket> waitForPacketDirect(Function<ApnsPacket, Boolean> listener) {
        if(disconnected) {
            return CompletableFuture.failedFuture(new RuntimeException("APNS connection lost"));
        }

        var listenerWithFuture = new ApnsListener(listener);
        var result = unhandledPackets.removeIf(entry -> {
            var entryResult = listener.apply(entry);
            if(!entryResult) {
                return false;
            }

            listenerWithFuture.future().complete(entry);
            return true;
        });
        if(!result) {
            listeners.add(listenerWithFuture);
        }

        return listenerWithFuture.future();
    }

    public CompletableFuture<String> getAppToken(boolean business) {
        return loginFuture.thenComposeAsync(ignored -> {
            var topicHash = Sha1.calculate(business ? Specification.Whatsapp.APNS_WHATSAPP_BUSINESS_NAME : Specification.Whatsapp.APNS_WHATSAPP_NAME);
            send(ApnsPayloadTag.GET_TOKEN, Map.of(
                    1, authToken,
                    2, topicHash,
                    3, new byte[]{0x00, 0x00}
            ));
            return waitForPacketDirect(packet -> packet.tag() == ApnsPayloadTag.TOKEN_RESPONSE && Arrays.equals(packet.fields().get(0x3), topicHash))
                    .thenApplyAsync(packet -> HexFormat.of().formatHex(packet.fields().get(0x2)));
        });
    }

    private void send(ApnsPayloadTag payloadType, Map<Integer, ?> fields) {
        if(disconnected) {
            throw new RuntimeException("APNS connection lost");
        }

        var payloadLength = getPayloadLength(fields);
        var byteArrayOutputStream = new ByteArrayOutputStream(payloadLength);
        try (var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeByte(payloadType.value());
            dataOutputStream.writeInt(payloadLength);
            for (var entry : fields.entrySet()) {
                if(entry.getValue() == null) {
                    continue;
                }

                dataOutputStream.writeByte(entry.getKey().byteValue());
                switch (entry.getValue()) {
                    case byte[] bytes -> {
                        dataOutputStream.writeShort(bytes.length);
                        dataOutputStream.write(bytes);
                    }
                    case byte[][] bytes -> {
                        for (var payloadEntry : bytes) {
                            dataOutputStream.writeShort(payloadEntry.length);
                            dataOutputStream.write(payloadEntry);
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value");
                }
            }
            socket.getOutputStream().write(byteArrayOutputStream.toByteArray());
        } catch (SocketException ignored) {
            // Ignored
        }catch (IOException exception) {
            throw new UncheckedIOException("Cannot send message", exception);
        }
    }

    private int getPayloadLength(Map<Integer, ?> fields) {
        return fields.values()
                .stream()
                .mapToInt(this::getPayloadLength)
                .sum();
    }

    private int getPayloadLength(Object entry) {
        return switch (entry) {
            case byte[] bytes -> bytes.length + 3;
            case byte[][] bytes -> Arrays.stream(bytes).mapToInt(value -> value.length + 2).sum() + 1;
            case null -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + entry.getClass().getName());
        };
    }

    private void readIncomingMessages() {
        Thread.ofPlatform().start(() -> {
            try(var dataInputStream = new DataInputStream(socket.getInputStream())) {
                while (socket.isConnected()) {
                    var id = dataInputStream.readUnsignedByte();
                    var length = dataInputStream.readInt();
                    if (length <= 0) {
                        continue;
                    }

                    var payload = new byte[length];
                    dataInputStream.readFully(payload);
                    var fields = readFields(payload);
                    var packetType = ApnsPayloadTag.of(id);
                    if(packetType == ApnsPayloadTag.NOTIFICATION) {
                        sendAck(fields);
                    }

                    var packet = new ApnsPacket(packetType, fields);
                    onPacket(packet);
                }
            }catch (IOException exception) {
                if(!socket.isClosed()) {
                    throw new UncheckedIOException(exception);
                }
            }finally {
                this.disconnected = true;
                var apnsConnectionLost = new RuntimeException("APNS connection lost");
                listeners.forEach(listener -> listener.future().completeExceptionally(apnsConnectionLost));
                if(!loginFuture.isDone()) {
                    loginFuture.completeExceptionally(apnsConnectionLost);
                }
            }
        });
    }

    private void onPacket(ApnsPacket packet) {
        var result = listeners.removeIf(listener -> {
            var remove = listener.filter().apply(packet);
            if(remove) {
                listener.future().complete(packet);
            }

            return remove;
        });
        if (result) {
            return;
        }

        unhandledPackets.add(packet);
    }

    private void sendAck(HashMap<Integer, byte[]> fields) {
        send(ApnsPayloadTag.ACK, Map.of(
                1, authToken,
                4, fields.get(0x4),
                8, new byte[]{0x00}
        ));
    }

    private static HashMap<Integer, byte[]> readFields(byte[] payload) throws IOException {
        var fields = new HashMap<Integer, byte[]>();
        try(var payloadDataInputStream = new DataInputStream(new ByteArrayInputStream(payload))) {
            int fieldId;
            while ((fieldId = payloadDataInputStream.read()) >= 0) {
                var fieldLength = payloadDataInputStream.readUnsignedShort();
                var value = new byte[fieldLength];
                payloadDataInputStream.readFully(value);
                fields.put(fieldId, value);
            }
        }
        return fields;
    }

    public void close() {
        try {
            listeners.clear();
            unhandledPackets.clear();
            pingExecutor.shutdownNow();
            if(loginFuture != null && !loginFuture.isDone()) {
                loginFuture.cancel(true);
            }

            if(socket != null) {
                socket.close();
            }
        }catch (Throwable ignored) {
            // Ignored
        }
    }
}
