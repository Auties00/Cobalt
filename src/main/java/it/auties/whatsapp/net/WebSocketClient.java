package it.auties.whatsapp.net;

import it.auties.whatsapp.crypto.Sha1;
import it.auties.whatsapp.util.Bytes;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

// The message encoding/decoding is a minimal copy from the OpenJDK source
// No text IO / no partial message output
// IO is not synchronized as it's assumed that SocketHandler will do that
public class WebSocketClient implements AutoCloseable {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 300;
    private static final String DEFAULT_PATH = "/";
    private static final int KEY_LENGTH = 16;
    private static final int VERSION = 13;
    private static final int SWITCHING_PROTOCOLS_CODE = 101;
    private static final String SERVER_KEY_HEADER = "sec-websocket-accept";
    private static final int MAX_HEADER_SIZE_BYTES = 2 + 8 + 4;
    private static final int MAX_CONTROL_FRAME_PAYLOAD_LENGTH = 125;
    private static final int NORMAL_CLOSURE = 1000;
    private static final int NO_STATUS_CODE = 1005;
    private static final int CLOSED_ABNORMALLY = 1006;
    private static final String MAGIC_VALUE = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final SocketClient underlyingSocket;
    private final String clientKey;
    private final MessageDecoder messageDecoder;
    private final MessageEncoder messageEncoder;
    private final Listener listener;
    private String serverKey;

    private WebSocketClient(SocketClient underlyingSocket, Listener listener) {
        this.underlyingSocket = underlyingSocket;
        this.clientKey = Base64.getEncoder().encodeToString(Bytes.random(KEY_LENGTH));
        this.messageDecoder = new MessageDecoder();
        this.messageEncoder = new MessageEncoder(underlyingSocket.getSendBufferSize());
        this.listener = listener;
    }

    public static WebSocketClient newPlainClient(URI proxy, Listener listener) throws IOException {
        var underlyingSocket = SocketClient.newPlainClient(proxy);
        return new WebSocketClient(underlyingSocket, listener);
    }


    public static WebSocketClient newSecureClient(SSLEngine sslEngine, URI proxy, Listener listener) throws IOException {
        var underlyingSocket = SocketClient.newSecureClient(sslEngine, proxy);
        return new WebSocketClient(underlyingSocket, listener);
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address, String path) {
        return connectAsync(address, path, DEFAULT_CONNECTION_TIMEOUT);
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address, String path, int timeout) {
        return underlyingSocket.connectAsync(address, timeout)
                .thenComposeAsync(ignored -> handshake(path))
                .thenRunAsync(this::listen)
                .orTimeout(timeout, TimeUnit.SECONDS);
    }

    private void listen() {
        var buffer = ByteBuffer.allocate(underlyingSocket.getReceiveBufferSize());
        listen(buffer, listener);
    }

    private void listen(ByteBuffer buffer, Listener listener) {
        buffer.clear();
        underlyingSocket.readAsync(buffer, (bytesRead, error) -> {
            if (error != null) {
                close();
                return;
            }

            messageDecoder.readFrame(buffer, listener);
            listen(buffer, listener);
        });
    }


    private CompletableFuture<Void> handshake(String path) {
        var payload = generateWebSocketUpgradePayload(path);
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
        var buffer = ByteBuffer.allocate(underlyingSocket.getReceiveBufferSize());
        return underlyingSocket.readAsync(buffer).thenApplyAsync(bytesRead -> {
            var data = new byte[buffer.limit()];
            buffer.get(data);
            return new String(data);
        });
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
            var dst = messageEncoder.encodeBinary(buffer, true);
            return underlyingSocket.writeAsync(dst);
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public void close() {
        if(underlyingSocket.isClosed()) {
            return;
        }

        closeWebSocket();
        closeUnderlyingSocket();
        listener.onClose(NORMAL_CLOSURE, "");
    }

    private void closeWebSocket() {
        try {
            var dst = messageEncoder.encodeClose(NORMAL_CLOSURE, CharBuffer.allocate(0));
            var future = underlyingSocket.writeAsync(dst);
            future.join();
        }catch(Throwable ignored) {

        }
    }

    private void closeUnderlyingSocket() {
        try {
            underlyingSocket.close();
        }catch (Throwable ignored1) {

        }
    }

    public interface Listener {
        default void onMessage(ByteBuffer data, boolean last) {

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

        private final ByteBuffer accumulator;
        private boolean fin;
        private MessageOpcode opcode;
        private MessageOpcode originatingOpcode;
        private long payloadLen;
        private long unconsumedPayloadLen;
        private ByteBuffer binaryData;
        private int state;
        private long remainingPayloadLength;

        public MessageDecoder() {
            this.accumulator = ByteBuffer.allocate(8);
            this.state = AWAITING_FIRST_BYTE;
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
            if (!input.hasRemaining()) {
                return true;
            }

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
            var lastPayloadChunk = unconsumedPayloadLen == 0;
            if (opcode.isControl()) {
                if (binaryData != null) {
                    binaryData.put(data);
                } else if (!lastPayloadChunk) {
                    binaryData = ByteBuffer.allocate(MAX_CONTROL_FRAME_PAYLOAD_LENGTH).put(data);
                } else {
                    binaryData = ByteBuffer.allocate(data.remaining()).put(data);
                }
            } else {
                var last = fin && lastPayloadChunk;
                var text = opcode == MessageOpcode.TEXT || originatingOpcode == MessageOpcode.TEXT;
                if (!text) {
                    listener.onMessage(data, last);
                    data.position(data.limit());
                }
            }
        }

        private void endFrame(Listener listener) {
            if (opcode.isControl()) {
                binaryData.flip();
            }
            switch (opcode) {
                case CLOSE -> {
                    var statusCode = NO_STATUS_CODE;
                    var reason = "";
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
    }

    private static final class MessageEncoder {
        private final SecureRandom maskingKeySource;
        private final CharsetEncoder charsetEncoder;
        private final ByteBuffer intermediateBuffer;
        private final ByteBuffer outputBuffer;
        private final ByteBuffer headerBuffer;
        private final ByteBuffer acc;
        private final int[] maskBytes;
        private int offset;
        private long maskLong;
        private boolean previousFin = true;
        private boolean previousText;
        private boolean closed;
        private char firstChar;
        private long payloadLen;
        private int maskingKey;
        private boolean mask;

        public MessageEncoder(int maxPayloadSize) {
            this.maskingKeySource = new SecureRandom();
            this.charsetEncoder = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            this.intermediateBuffer = ByteBuffer.allocate(maxPayloadSize);
            this.outputBuffer = ByteBuffer.allocate(maxPayloadSize);
            this.headerBuffer = ByteBuffer.allocate(MAX_HEADER_SIZE_BYTES);
            this.acc = ByteBuffer.allocate(8);
            this.maskBytes = new int[4];
        }

        public ByteBuffer encodeBinary(ByteBuffer src, boolean last) throws IOException {
            if (closed) {
                throw new IOException("Output closed");
            }

            if (previousText && !previousFin) {
                throw new IllegalStateException("Unexpected binary message");
            }

            var opcode = previousFin ? MessageOpcode.BINARY : MessageOpcode.CONTINUATION;
            setupHeader(opcode, last, src.remaining());
            previousFin = last;
            previousText = false;
            outputBuffer.clear();
            outputBuffer.put(headerBuffer);
            transferMasking(src, outputBuffer);
            outputBuffer.flip();
            return outputBuffer;
        }

        public ByteBuffer encodeClose(int statusCode, CharBuffer reason) throws IOException {
            if (closed) {
                throw new IOException("Output closed");
            }

            intermediateBuffer.position(0).limit(MAX_CONTROL_FRAME_PAYLOAD_LENGTH);
            intermediateBuffer.putChar((char) statusCode);
            var r = charsetEncoder.reset().encode(reason, intermediateBuffer, true);
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
                throw new InternalError();
            }

            intermediateBuffer.flip();
            setupHeader(MessageOpcode.CLOSE, true, intermediateBuffer.remaining());
            closed = true;
            outputBuffer.clear();
            outputBuffer.put(headerBuffer);
            transferMasking(intermediateBuffer, outputBuffer);
            outputBuffer.flip();
            return outputBuffer;
        }

        private void setupHeader(MessageOpcode opcode, boolean fin, long payloadLen) {
            headerBuffer.clear();
            var mask = maskingKeySource.nextInt();
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
            var i = src.position();
            var j = dst.position();
            var srcLim = src.limit();
            var dstLim = dst.limit();
            for (; offset < 4 && i < srcLim && j < dstLim; i++, j++, offset++) {
                dst.put(j, (byte) (src.get(i) ^ maskBytes[offset]));
            }
            offset &= 3;
            src.position(i);
            dst.position(j);
        }

        private void loop(ByteBuffer src, ByteBuffer dst) {
            var i = src.position();
            var j = dst.position();
            var srcLongLim = src.limit() - 7;
            var dstLongLim = dst.limit() - 7;
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
            var srcLim = src.limit();
            var dstLim = dst.limit();
            var i = src.position();
            var j = dst.position();
            for (; i < srcLim && j < dstLim; i++, j++, offset = (offset + 1) & 3) {
                dst.put(j, (byte) (src.get(i) ^ maskBytes[offset]));
            }
            src.position(i);
            dst.position(j);
        }

        private MessageEncoder fin(boolean value) {
            if (value) {
                firstChar |= 0b10000000_00000000;
            } else {
                firstChar &= (char) ~0b10000000_00000000;
            }
            return this;
        }

        private MessageEncoder opcode(MessageOpcode value) {
            firstChar = (char) ((firstChar & 0xF0FF) | (value.code << 8));
            return this;
        }

        private MessageEncoder payloadLen(long value) {
            if (value < 0) {
                throw new IllegalArgumentException("Negative: " + value);
            }
            payloadLen = value;
            firstChar &= 0b11111111_10000000;
            if (payloadLen < 126) {
                firstChar |= (char) payloadLen;
            } else if (payloadLen < 65536) {
                firstChar |= 126;
            } else {
                firstChar |= 127;
            }
            return this;
        }

        private MessageEncoder mask(int value) {
            firstChar |= 0b00000000_10000000;
            maskingKey = value;
            mask = true;
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
}
