package it.auties.whatsapp.net;

import it.auties.whatsapp.crypto.Sha1;
import it.auties.whatsapp.util.Bytes;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class WebSocketClient implements AutoCloseable {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    private static final String DEFAULT_PATH = "/";
    private static final int KEY_LENGTH = 16;
    private static final int VERSION = 13;
    private static final int DEFAULT_RCV_BUF = 8192;
    private static final int SWITCHING_PROTOCOLS_CODE = 101;
    private static final String SERVER_KEY_HEADER = "sec-websocket-accept";
    private static final int MAX_HEADER_SIZE_BYTES = 2 + 8 + 4;
    private static final int MAX_CONTROL_FRAME_PAYLOAD_LENGTH = 125;
    private static final int NORMAL_CLOSURE = 1000;
    private static final int NO_STATUS_CODE = 1005;
    private static final int CLOSED_ABNORMALLY = 1006;
    private static final String MAGIC_VALUE = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final ByteBuffer EMPTY_BYTEBUFFER = ByteBuffer.allocate(0);

    private final SocketClient underlyingSocket;
    private final String clientKey;
    private final MessageDecoder messageDecoder;
    private final MessageEncoder messageEncoder;
    private final AtomicBoolean listening;
    private String serverKey;

    private WebSocketClient(SocketClient underlyingSocket) {
        this.underlyingSocket = underlyingSocket;
        this.clientKey = Base64.getEncoder().encodeToString(Bytes.random(KEY_LENGTH));
        this.messageDecoder = new MessageDecoder();
        this.messageEncoder = new MessageEncoder();
        this.listening = new AtomicBoolean(false);
    }

    public static WebSocketClient newPlainClient(URI proxy) throws IOException {
        var underlyingSocket = SocketClient.newPlainClient(proxy);
        return new WebSocketClient(underlyingSocket);
    }


    public static WebSocketClient newSecureClient(SSLEngine sslEngine, URI proxy) throws IOException {
        var underlyingSocket = SocketClient.newSecureClient(sslEngine, proxy);
        return new WebSocketClient(underlyingSocket);
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address, String path) {
        return connectAsync(address, path, DEFAULT_CONNECTION_TIMEOUT);
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address, String path, int timeout) {
        return underlyingSocket.connectAsync(address, timeout)
                .thenComposeAsync(ignored -> handshake(path))
                .orTimeout(timeout, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> handshake(String path) {
        var payload = generateWebSocketUpgradePayload(path);
        System.out.println(payload);
        return underlyingSocket.writeAsync(ByteBuffer.wrap(payload.getBytes()))
                .thenComposeAsync(writeResult -> readWebSocketUpgradeResponse())
                .thenComposeAsync(this::parseWebSocketUpgradeResponse);
    }

    private CompletableFuture<Void> parseWebSocketUpgradeResponse(String result) {
        if (result.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Malformed HTTP response"));
        }

        var lines = result.split("\r\n");
        var responseParts = lines[0].split(" ", 3);
        try {
            var statusCode = Integer.parseUnsignedInt(responseParts[1]);
            if (statusCode != SWITCHING_PROTOCOLS_CODE) {
                return CompletableFuture.failedFuture(new SocketException("Cannot upgrade protocol to WebSocket, status code " + statusCode));
            }

            for (var i = 1; i < lines.length; i++) {
                var header = lines[i].split(": ", 2);
                if (header.length != 2) {
                    continue;
                }

                var headerKey = header[0];
                if (!headerKey.equalsIgnoreCase(SERVER_KEY_HEADER)) {
                    continue;
                }

                serverKey = header[1];
                break;
            }

            if (serverKey == null) {
                return CompletableFuture.failedFuture(new SocketException("Missing Sec-WebSocket-Accept header"));
            }

            var hash = Sha1.calculate(Bytes.concat(clientKey.getBytes(), MAGIC_VALUE.getBytes()));
            if (!Objects.equals(serverKey, Base64.getEncoder().encodeToString(hash))) {
                return CompletableFuture.failedFuture(new SocketException("Invalid Sec-WebSocket-Accept header"));
            }

            return CompletableFuture.completedFuture(null);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(new SocketException("Malformed HTTP response: " + responseParts[0]));
        }
    }

    private String generateWebSocketUpgradePayload(String path) {
        var socketAddress = getSocketAddress();
        var urlHost = socketAddress.getHostName();
        var urlPort = socketAddress.getPort();
        var urlPath = Objects.requireNonNullElse(path, DEFAULT_PATH);
        return """
                GET %s HTTP/1.1\r
                Host: %s:%d\r
                Connection: Upgrade\r
                Upgrade: websocket\r
                Sec-WebSocket-Version: %d\r
                Sec-WebSocket-Key: %s\r
                \r
                """.formatted(urlPath, urlHost, urlPort, VERSION, clientKey);
    }

    private CompletableFuture<String> readWebSocketUpgradeResponse() {
        var buffer = ByteBuffer.allocate(readReceiveBufferSize());
        return underlyingSocket.readAsync(buffer).thenApplyAsync(bytesRead -> {
            var data = new byte[buffer.limit()];
            buffer.get(data);
            return new String(data);
        });
    }

    private int readReceiveBufferSize() {
        try {
            return underlyingSocket.getOption(StandardSocketOptions.SO_RCVBUF);
        } catch (IOException exception) {
            return DEFAULT_RCV_BUF;
        }
    }

    private InetSocketAddress getSocketAddress() {
        var address = underlyingSocket.getRemoteSocketAddress();
        if (!(address instanceof InetSocketAddress inetSocketAddress)) {
            throw new IllegalStateException("Cannot query socket address: unsupported address type");
        }

        return inetSocketAddress;
    }

    public CompletableFuture<Void> sendBinary(ByteBuffer buffer) {
        try {
            var dst = ByteBuffer.allocate(buffer.remaining() * 2);
            messageEncoder.encodeBinary(buffer, true, dst);
            messageEncoder.reset();
            dst.flip();
            return underlyingSocket.writeAsync(dst);
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public CompletableFuture<Void> sendText(String text) {
        try {
            var buffer = CharBuffer.wrap(text);
            var dst = ByteBuffer.allocate(buffer.remaining() * 2);
            messageEncoder.encodeText(buffer, true, dst);
            messageEncoder.reset();
            dst.flip();
            return underlyingSocket.writeAsync(dst);
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public void listen(Listener listener) {
        if (listening.get()) {
            throw new IllegalStateException("Web socket was already listened");
        }

        listening.set(true);
        var buffer = ByteBuffer.allocate(readReceiveBufferSize());
        readAndDecodeFrame(buffer, listener);
    }

    private void readAndDecodeFrame(ByteBuffer buffer, Listener listener) {
        buffer.clear();
        buffer.position(0);
        buffer.limit(buffer.capacity());
        underlyingSocket.readAsync(buffer, (bytesRead, error) -> {
            if (error != null) {
                error.printStackTrace();
                return;
            }

            System.out.println("Read buf" + bytesRead);
            try {
                messageDecoder.readFrame(buffer, listener);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            readAndDecodeFrame(buffer, listener);
        });
    }

    @Override
    public void close() throws IOException {
        try {
            var dst = ByteBuffer.allocate(16384);
            messageEncoder.encodeClose(NORMAL_CLOSURE, CharBuffer.allocate(0), dst);
            dst.flip();
            var future = underlyingSocket.writeAsync(dst);
            future.join();
        } finally {
            underlyingSocket.close();
        }
    }

    public interface Listener {
        default void onText(CharSequence data) {

        }

        default void onBinary(ByteBuffer data) {

        }

        default void onClose(int statusCode, String reason) {

        }
    }

    private static final class MessageDecoder {
        private static final int AWAITING_FIRST_BYTE = 1;
        private static final int AWAITING_SECOND_BYTE = 2;
        private static final int READING_16_LENGTH = 4;
        private static final int READING_64_LENGTH = 8;
        private static final int READING_PAYLOAD = 32;

        private final ByteBufferOutputStream binaryOutputStream;
        private final CharBufferOutputStream textOutputStream;
        private final CharsetDecoder textDecoder;
        private final ByteBuffer accumulator;
        private boolean fin;
        private MessageOpcode opcode;
        private MessageOpcode originatingOpcode;
        private long payloadLen;
        private long unconsumedPayloadLen;
        private ByteBuffer binaryData;
        private int state;
        private long remainingPayloadLength;
        private ByteBuffer textLeftovers;

        public MessageDecoder() {
            this.binaryOutputStream = new ByteBufferOutputStream();
            this.textOutputStream = new CharBufferOutputStream();
            this.textDecoder = UTF_8.newDecoder();
            textDecoder.onMalformedInput(CodingErrorAction.REPORT);
            textDecoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            this.accumulator = ByteBuffer.allocate(8);
            this.state = AWAITING_FIRST_BYTE;
            this.textLeftovers = EMPTY_BYTEBUFFER;
        }

        public void readFrame(ByteBuffer input, Listener listener) {
            while (true) {
                var result = handleFrame(input, listener);
                if (result) {
                    break;
                }
            }
        }

        private boolean handleFrame(ByteBuffer input, Listener listener) {
            return switch (state) {
                case AWAITING_FIRST_BYTE -> handleOpcode(input);
                case AWAITING_SECOND_BYTE -> handlePayloadByte(input);
                case READING_16_LENGTH -> handlePayloadLength(input);
                case READING_64_LENGTH -> handlePayloadLengthExtended(input);
                case READING_PAYLOAD -> handlePayloadData(input, listener);
                default -> throw new InternalError(String.valueOf(state));
            };
        }

        private boolean handlePayloadData(ByteBuffer input, Listener listener) {
            var deliverable = (int) Math.min(remainingPayloadLength, input.remaining());
            var oldLimit = input.limit();
            input.limit(input.position() + deliverable);
            if (deliverable != 0 || remainingPayloadLength == 0) {
                payloadData(input, listener);
            }
            var consumed = deliverable - input.remaining();
            if (consumed < 0) {
                throw new InternalError();
            }
            input.limit(oldLimit);
            remainingPayloadLength -= consumed;
            if (remainingPayloadLength == 0) {
                endFrame(listener);
                state = AWAITING_FIRST_BYTE;
            }
            return false;
        }

        private boolean handlePayloadLengthExtended(ByteBuffer input) {
            if (!input.hasRemaining()) {
                return true;
            }

            var b = input.get();
            if (accumulator.put(b).position() < 8) {
                return false;
            }

            remainingPayloadLength = accumulator.flip().getLong();
            if (remainingPayloadLength < 0) {
                throw new IllegalArgumentException("Negative payload length: " + remainingPayloadLength);
            } else if (remainingPayloadLength < 65536) {
                throw new IllegalArgumentException("Not minimally-encoded payload length:" + remainingPayloadLength);
            }
            payloadLen(remainingPayloadLength);
            accumulator.clear();
            state = READING_PAYLOAD;
            return false;
        }

        private boolean handlePayloadLength(ByteBuffer input) {
            if (!input.hasRemaining()) {
                return true;
            }

            var b = input.get();
            if (accumulator.put(b).position() < 2) {
                return false;
            }

            remainingPayloadLength = accumulator.flip().getChar();
            if (remainingPayloadLength < 126) {
                throw new IllegalArgumentException("Not minimally-encoded payload length:" + remainingPayloadLength);
            }

            payloadLen(remainingPayloadLength);
            accumulator.clear();
            state = READING_PAYLOAD;
            return false;
        }

        private boolean handleOpcode(ByteBuffer input) {
            if (!input.hasRemaining()) {
                return true;
            }

            var b = input.get();
            fin = (b & 0b10000000) != 0;
            if ((b & 0b01000000) != 0) {
                throw new IllegalArgumentException("Unexpected rsv1 bit");
            }

            if ((b & 0b00100000) != 0) {
                throw new IllegalArgumentException("Unexpected rsv2 bit");
            }

            if ((b & 0b00010000) != 0) {
                throw new IllegalArgumentException("Unexpected rsv3 bit");
            }

            var v = MessageOpcode.ofCode(b);
            switch (v) {
                case PING, PONG, CLOSE -> {
                    if (!fin) {
                        throw new IllegalArgumentException("Fragmented control frame  " + v);
                    }
                    opcode = v;
                }
                case TEXT, BINARY -> {
                    if (originatingOpcode != null) {
                        throw new IllegalArgumentException(format("Unexpected frame %s (fin=%s)", v, fin));
                    }
                    opcode = v;
                    if (!fin) {
                        originatingOpcode = v;
                    }
                }
                case CONTINUATION -> {
                    if (originatingOpcode == null) {
                        throw new IllegalArgumentException(format("Unexpected frame %s (fin=%s)", v, fin));
                    }

                    opcode = v;
                }
                case null, default -> throw new IllegalArgumentException("Unexpected opcode " + v);
            }

            state = AWAITING_SECOND_BYTE;
            return false;
        }

        private boolean handlePayloadByte(ByteBuffer input) {
            if (!input.hasRemaining()) {
                return true;
            }
            var b = input.get();
            if ((b & 0b10000000) != 0) {
                throw new IllegalArgumentException("Masked frame received");
            }

            var p1 = (byte) (b & 0b01111111);
            if (p1 < 126) {
                payloadLen(remainingPayloadLength = p1);
                state = READING_PAYLOAD;
            } else if (p1 < 127) {
                state = READING_16_LENGTH;
            } else {
                state = READING_64_LENGTH;
            }
            return false;
        }

        private void payloadLen(long value) {
            if (opcode.isControl()) {
                if (value > MAX_CONTROL_FRAME_PAYLOAD_LENGTH) {
                    throw new IllegalArgumentException(
                            format("%s's payload length %s", opcode, value));
                }
                assert MessageOpcode.CLOSE.isControl();
                if (opcode == MessageOpcode.CLOSE && value == 1) {
                    throw new IllegalArgumentException("Incomplete status code");
                }
            }
            payloadLen = value;
            unconsumedPayloadLen = value;
        }

        private void payloadData(ByteBuffer data, Listener listener) {
            unconsumedPayloadLen -= data.remaining();
            boolean lastPayloadChunk = unconsumedPayloadLen == 0;
            if (opcode.isControl()) {
                if (binaryData != null) { // An intermediate or the last chunk
                    binaryData.put(data);
                } else if (!lastPayloadChunk) { // The first chunk
                    binaryData = ByteBuffer.allocate(MAX_CONTROL_FRAME_PAYLOAD_LENGTH).put(data);
                } else { // The only chunk
                    binaryData = ByteBuffer.allocate(data.remaining()).put(data);
                }
            } else {
                boolean last = fin && lastPayloadChunk;
                boolean text = opcode == MessageOpcode.TEXT || originatingOpcode == MessageOpcode.TEXT;
                if (!text) {
                    binaryOutputStream.writeBuffer(data, data.remaining());
                    if (last) {
                        listener.onBinary(binaryOutputStream.toByteBuffer());
                        binaryOutputStream.reset();
                    }
                    data.position(data.limit()); // Consume
                } else {
                    boolean binaryNonEmpty = data.hasRemaining();
                    CharBuffer textData;
                    try {
                        textData = decode(data, last);
                    } catch (CharacterCodingException e) {
                        throw new IllegalArgumentException("Invalid UTF-8 in frame " + opcode, e);
                    }
                    if (!(binaryNonEmpty && !textData.hasRemaining())) {
                        textOutputStream.writeBuffer(textData, textData.remaining());
                        if (last) {
                            listener.onText(textOutputStream.toString());
                            textOutputStream.reset();
                        }
                    }
                }
            }
        }

        private void endFrame(Listener listener) {
            if (opcode.isControl()) {
                binaryData.flip();
            }
            switch (opcode) {
                case CLOSE -> {
                    char statusCode = NO_STATUS_CODE;
                    String reason = "";
                    if (payloadLen != 0) {
                        statusCode = binaryData.getChar();
                        if (!isLegalToReceiveFromServer(statusCode)) {
                            throw new IllegalArgumentException(
                                    "Illegal status code: " + statusCode);
                        }
                        try {
                            reason = UTF_8.newDecoder().decode(binaryData).toString();
                        } catch (CharacterCodingException e) {
                            throw new IllegalArgumentException("Illegal close reason", e);
                        }
                    }
                    listener.onClose(statusCode, reason);
                }
                case PING, PONG -> binaryData = null;
                default -> {
                    if (fin) {
                        originatingOpcode = null;
                    }
                }
            }
            payloadLen = 0;
            opcode = null;
        }

        private static boolean isLegalToReceiveFromServer(int code) {
            if (code < 1000 || code > 65535) {
                return false;
            }

            if ((code >= 1016 && code <= 2999) || code == 1004) {
                return false;
            }

            return code != NO_STATUS_CODE && code != CLOSED_ABNORMALLY && code != 1015 && code != 1010;
        }

        private CharBuffer decode(ByteBuffer in, boolean endOfInput) throws CharacterCodingException {
            ByteBuffer b;
            int rem = textLeftovers.remaining();
            if (rem != 0) {
                b = ByteBuffer.allocate(rem + in.remaining());
                b.put(textLeftovers).put(in).flip();
            } else {
                b = in;
            }
            CharBuffer out = CharBuffer.allocate(b.remaining());
            CoderResult r = textDecoder.decode(b, out, endOfInput);
            if (r.isError()) {
                r.throwException();
            }
            if (b.hasRemaining()) {
                textLeftovers = ByteBuffer.allocate(b.remaining()).put(b).flip();
            } else {
                textLeftovers = EMPTY_BYTEBUFFER;
            }
            b.position(b.limit()); // As if we always read to the end
            if (endOfInput) {
                r = textDecoder.flush(out);
                textDecoder.reset();
                if (r.isOverflow()) {
                    throw new InternalError("Not yet implemented");
                }
            }
            return out.flip();
        }
    }

    private static final class MessageEncoder {
        private final SecureRandom maskingKeySource;
        private final CharsetEncoder charsetEncoder;
        private final ByteBuffer intermediateBuffer;
        private final ByteBuffer headerBuffer;
        private final ByteBuffer acc;
        private final int[] maskBytes;
        private int offset;
        private long maskLong;
        private boolean started;
        private boolean flushing;
        private boolean moreText = true;
        private long headerCount;
        private boolean previousFin = true;
        private boolean previousText;
        private boolean closed;
        private int actualLen;
        private int expectedLen;
        private char firstChar;
        private long payloadLen;
        private int maskingKey;
        private boolean mask;

        public MessageEncoder() {
            this.maskingKeySource = new SecureRandom();
            this.charsetEncoder = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            this.intermediateBuffer = ByteBuffer.allocate(16384);
            this.headerBuffer = ByteBuffer.allocate(MAX_HEADER_SIZE_BYTES);
            this.acc = ByteBuffer.allocate(8);
            this.maskBytes = new int[4];
        }

        public void reset() {
            started = false;
            flushing = false;
            moreText = true;
            headerCount = 0;
            actualLen = 0;
        }

        public boolean encodeText(CharBuffer src, boolean last, ByteBuffer dst) throws IOException {
            if (closed) {
                throw new IOException("Output closed");
            }
            if (!started) {
                if (!previousText && !previousFin) {
                    // Previous data message was a partial binary message
                    throw new IllegalStateException("Unexpected text message");
                }
                started = true;
                headerBuffer.position(0).limit(0);
                intermediateBuffer.position(0).limit(0);
                charsetEncoder.reset();
            }
            while (true) {
                if (putAvailable(headerBuffer, dst)) {
                    return false;
                }

                if (maskAvailable(intermediateBuffer, dst) < 0) {
                    return false;
                }
                if (!moreText) {
                    previousFin = last;
                    previousText = true;
                    return true;
                }
                intermediateBuffer.clear();
                CoderResult r = null;
                if (!flushing) {
                    r = charsetEncoder.encode(src, intermediateBuffer, true);
                    if (r.isUnderflow()) {
                        flushing = true;
                    }
                }
                if (flushing) {
                    r = charsetEncoder.flush(intermediateBuffer);
                    if (r.isUnderflow()) {
                        moreText = false;
                    }
                }
                if (r.isError()) {
                    try {
                        r.throwException();
                    } catch (CharacterCodingException e) {
                        throw new IOException("Malformed text message", e);
                    }
                }
                intermediateBuffer.flip();
                MessageOpcode opcode = previousFin && headerCount == 0 ? MessageOpcode.TEXT : MessageOpcode.CONTINUATION;
                boolean fin = last && !moreText;
                setupHeader(opcode, fin, intermediateBuffer.remaining());
                headerCount++;
            }
        }

        private boolean putAvailable(ByteBuffer src, ByteBuffer dst) {
            int available = dst.remaining();
            if (available >= src.remaining()) {
                dst.put(src);
                return false;
            } else {
                int lim = src.limit();                   // save the limit
                src.limit(src.position() + available);
                dst.put(src);
                src.limit(lim);                          // restore the limit
                return true;
            }
        }

        public boolean encodeBinary(ByteBuffer src, boolean last, ByteBuffer dst) throws IOException {
            if (closed) {
                throw new IOException("Output closed");
            }
            if (!started) {
                if (previousText && !previousFin) {
                    // Previous data message was a partial text message
                    throw new IllegalStateException("Unexpected binary message");
                }
                expectedLen = src.remaining();
                MessageOpcode opcode = previousFin ? MessageOpcode.BINARY : MessageOpcode.CONTINUATION;
                setupHeader(opcode, last, expectedLen);
                previousFin = last;
                previousText = false;
                started = true;
            }
            if (putAvailable(headerBuffer, dst)) {
                return false;
            }
            int count = maskAvailable(src, dst);
            actualLen += Math.abs(count);
            if (count >= 0 && actualLen != expectedLen) {
                throw new IOException("Concurrent message modification");
            }
            return count >= 0;
        }

        private int maskAvailable(ByteBuffer src, ByteBuffer dst) {
            int r0 = dst.remaining();
            transferMasking(src, dst);
            int masked = r0 - dst.remaining();
            return src.hasRemaining() ? -masked : masked;
        }

        public boolean encodePing(ByteBuffer src, ByteBuffer dst) throws IOException {
            if (closed) {
                throw new IOException("Output closed");
            }
            if (!started) {
                expectedLen = src.remaining();
                if (expectedLen > MAX_CONTROL_FRAME_PAYLOAD_LENGTH) {
                    throw new IllegalArgumentException("Long message: " + expectedLen);
                }
                setupHeader(MessageOpcode.PING, true, expectedLen);
                started = true;
            }
            if (putAvailable(headerBuffer, dst)) {
                return false;
            }
            int count = maskAvailable(src, dst);
            actualLen += Math.abs(count);
            if (count >= 0 && actualLen != expectedLen) {
                throw new IOException("Concurrent message modification");
            }
            return count >= 0;
        }

        public boolean encodePong(ByteBuffer src, ByteBuffer dst) throws IOException {
            if (closed) {
                throw new IOException("Output closed");
            }
            if (!started) {
                expectedLen = src.remaining();
                if (expectedLen > MAX_CONTROL_FRAME_PAYLOAD_LENGTH) {
                    throw new IllegalArgumentException("Long message: " + expectedLen);
                }
                setupHeader(MessageOpcode.PONG, true, expectedLen);
                started = true;
            }
            if (putAvailable(headerBuffer, dst)) {
                return false;
            }
            int count = maskAvailable(src, dst);
            actualLen += Math.abs(count);
            if (count >= 0 && actualLen != expectedLen) {
                throw new IOException("Concurrent message modification");
            }
            return count >= 0;
        }

        public boolean encodeClose(int statusCode, CharBuffer reason, ByteBuffer dst) throws IOException {
            if (closed) {
                throw new IOException("Output closed");
            }
            if (!started) {
                intermediateBuffer.position(0).limit(MAX_CONTROL_FRAME_PAYLOAD_LENGTH);
                intermediateBuffer.putChar((char) statusCode);
                CoderResult r = charsetEncoder.reset().encode(reason, intermediateBuffer, true);
                if (r.isUnderflow()) {
                    r = charsetEncoder.flush(intermediateBuffer);
                }
                if (r.isError()) {
                    try {
                        r.throwException();
                    } catch (CharacterCodingException e) {
                        throw new IOException("Malformed reason", e);
                    }
                } else if (r.isOverflow()) {
                    throw new IOException("Long reason");
                } else if (!r.isUnderflow()) {
                    throw new InternalError(); // assertion
                }
                intermediateBuffer.flip();
                setupHeader(MessageOpcode.CLOSE, true, intermediateBuffer.remaining());
                started = true;
                closed = true;
            }
            if (putAvailable(headerBuffer, dst)) {
                return false;
            }
            return maskAvailable(intermediateBuffer, dst) >= 0;
        }

        private void setupHeader(MessageOpcode opcode, boolean fin, long payloadLen) {
            headerBuffer.clear();
            int mask = maskingKeySource.nextInt();
            if (mask == 0) {
                fin(fin).opcode(opcode).payloadLen(payloadLen).write(headerBuffer);
            } else {
                fin(fin).opcode(opcode).payloadLen(payloadLen).mask(mask).write(headerBuffer);
            }
            headerBuffer.flip();
            acc.clear().putInt(mask).putInt(mask).flip();
            for (int i = 0; i < maskBytes.length; i++) {
                maskBytes[i] = acc.get(i);
            }
            offset = 0;
            maskLong = acc.getLong(0);
        }

        private void transferMasking(ByteBuffer src, ByteBuffer dst) {
            begin(src, dst);
            loop(src, dst);
            end(src, dst);
        }

        private void begin(ByteBuffer src, ByteBuffer dst) {
            if (offset == 0) {
                return;
            }
            int i = src.position(), j = dst.position();
            final int srcLim = src.limit(), dstLim = dst.limit();
            for (; offset < 4 && i < srcLim && j < dstLim; i++, j++, offset++) {
                dst.put(j, (byte) (src.get(i) ^ maskBytes[offset]));
            }
            offset &= 3;
            src.position(i);
            dst.position(j);
        }

        private void loop(ByteBuffer src, ByteBuffer dst) {
            int i = src.position();
            int j = dst.position();
            final int srcLongLim = src.limit() - 7, dstLongLim = dst.limit() - 7;
            for (; i < srcLongLim && j < dstLongLim; i += 8, j += 8) {
                dst.putLong(j, src.getLong(i) ^ maskLong);
            }
            if (i > src.limit()) {
                src.position(i - 8);
            } else {
                src.position(i);
            }
            if (j > dst.limit()) {
                dst.position(j - 8);
            } else {
                dst.position(j);
            }
        }

        private void end(ByteBuffer src, ByteBuffer dst) {
            final int srcLim = src.limit(), dstLim = dst.limit();
            int i = src.position(), j = dst.position();
            for (; i < srcLim && j < dstLim; i++, j++, offset = (offset + 1) & 3) {
                dst.put(j, (byte) (src.get(i) ^ maskBytes[offset]));
            }
            src.position(i);
            dst.position(j);
        }

        MessageEncoder fin(boolean value) {
            if (value) {
                firstChar |= 0b10000000_00000000;
            } else {
                // Explicit cast required:
                // The negation "~" sets the high order bits
                // so the value becomes more than 16 bits and the
                // compiler will emit a warning if not cast
                firstChar &= (char) ~0b10000000_00000000;
            }
            return this;
        }

        MessageEncoder rsv1(boolean value) {
            if (value) {
                firstChar |= 0b01000000_00000000;
            } else {
                // Explicit cast required - see fin() above
                firstChar &= (char) ~0b01000000_00000000;
            }
            return this;
        }

        MessageEncoder rsv2(boolean value) {
            if (value) {
                firstChar |= 0b00100000_00000000;
            } else {
                // Explicit cast required - see fin() above
                firstChar &= (char) ~0b00100000_00000000;
            }
            return this;
        }

        MessageEncoder rsv3(boolean value) {
            if (value) {
                firstChar |= 0b00010000_00000000;
            } else {
                // Explicit cast required - see fin() above
                firstChar &= (char) ~0b00010000_00000000;
            }
            return this;
        }

        MessageEncoder opcode(MessageOpcode value) {
            firstChar = (char) ((firstChar & 0xF0FF) | (value.code << 8));
            return this;
        }

        MessageEncoder payloadLen(long value) {
            if (value < 0) {
                throw new IllegalArgumentException("Negative: " + value);
            }
            payloadLen = value;
            firstChar &= 0b11111111_10000000; // Clear previous payload length leftovers
            if (payloadLen < 126) {
                firstChar |= (char) payloadLen;
            } else if (payloadLen < 65536) {
                firstChar |= 126;
            } else {
                firstChar |= 127;
            }
            return this;
        }

        MessageEncoder mask(int value) {
            firstChar |= 0b00000000_10000000;
            maskingKey = value;
            mask = true;
            return this;
        }

        MessageEncoder noMask() {
            firstChar &= (char) ~0b00000000_10000000;
            mask = false;
            return this;
        }

        private void write(ByteBuffer buffer) {
            buffer.putChar(firstChar);
            if (payloadLen >= 126) {
                if (payloadLen < 65536) {
                    buffer.putChar((char) payloadLen);
                } else {
                    buffer.putLong(payloadLen);
                }
            }
            if (mask) {
                buffer.putInt(maskingKey);
            }
        }
    }

    private enum MessageOpcode {
        CONTINUATION(0x0),
        TEXT(0x1),
        BINARY(0x2),
        NON_CONTROL_0x3(0x3),
        NON_CONTROL_0x4(0x4),
        NON_CONTROL_0x5(0x5),
        NON_CONTROL_0x6(0x6),
        NON_CONTROL_0x7(0x7),
        CLOSE(0x8),
        PING(0x9),
        PONG(0xA),
        CONTROL_0xB(0xB),
        CONTROL_0xC(0xC),
        CONTROL_0xD(0xD),
        CONTROL_0xE(0xE),
        CONTROL_0xF(0xF);

        private static final MessageOpcode[] opcodes;

        static {
            var values = values();
            opcodes = new MessageOpcode[values.length];
            for (var value : values) {
                opcodes[value.code] = value;
            }
        }

        private final byte code;

        MessageOpcode(int code) {
            this.code = (byte) code;
        }

        boolean isControl() {
            return (code & 0x8) != 0;
        }

        static MessageOpcode ofCode(int code) {
            return opcodes[code & 0xF];
        }
    }

    private static final class ByteBufferOutputStream {
        private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

        private byte[] buf;
        private int count;

        public ByteBufferOutputStream() {
            this.buf = new byte[32];
        }

        public void writeBuffer(ByteBuffer buffer, int length) {
            ensureCapacity(count + length);
            for (var i = 0; i < length; i++) {
                buf[count++] = buffer.get();
            }
        }

        private void ensureCapacity(int minCapacity) {
            var oldCapacity = buf.length;
            var minGrowth = minCapacity - oldCapacity;
            if (minGrowth > 0) {
                buf = Arrays.copyOf(buf, newLength(oldCapacity, minGrowth, oldCapacity));
            }
        }

        private int newLength(int oldLength, int minGrowth, int prefGrowth) {
            var prefLength = oldLength + Math.max(minGrowth, prefGrowth);
            if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
                return prefLength;
            } else {
                return hugeLength(oldLength, minGrowth);
            }
        }

        private int hugeLength(int oldLength, int minGrowth) {
            var minLength = oldLength + minGrowth;
            if (minLength < 0) {
                throw new OutOfMemoryError("Required array length " + oldLength + " + " + minGrowth + " is too large");
            }

            return Math.max(minLength, SOFT_MAX_ARRAY_LENGTH);
        }

        public ByteBuffer toByteBuffer() {
            return ByteBuffer.wrap(buf, 0, count);
        }

        public void reset() {
            count = 0;
        }
    }

    private static final class CharBufferOutputStream {
        private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

        private char[] buf;
        private int count;

        public CharBufferOutputStream() {
            this.buf = new char[32];
        }

        public void writeBuffer(CharBuffer buffer, int length) {
            ensureCapacity(count + length);
            for (var i = 0; i < length; i++) {
                buf[count++] = buffer.get();
            }
        }

        private void ensureCapacity(int minCapacity) {
            var oldCapacity = buf.length;
            var minGrowth = minCapacity - oldCapacity;
            if (minGrowth > 0) {
                buf = Arrays.copyOf(buf, newLength(oldCapacity, minGrowth, oldCapacity));
            }
        }

        private int newLength(int oldLength, int minGrowth, int prefGrowth) {
            var prefLength = oldLength + Math.max(minGrowth, prefGrowth);
            if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
                return prefLength;
            } else {
                return hugeLength(oldLength, minGrowth);
            }
        }

        private int hugeLength(int oldLength, int minGrowth) {
            var minLength = oldLength + minGrowth;
            if (minLength < 0) {
                throw new OutOfMemoryError("Required array length " + oldLength + " + " + minGrowth + " is too large");
            }

            return Math.max(minLength, SOFT_MAX_ARRAY_LENGTH);
        }

        @Override
        public String toString() {
            return String.valueOf(buf, 0, count);
        }

        public void reset() {
            count = 0;
        }
    }
}
