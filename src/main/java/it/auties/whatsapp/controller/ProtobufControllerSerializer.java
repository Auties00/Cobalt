package it.auties.whatsapp.controller;

import it.auties.protobuf.model.ProtobufString;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatSpec;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class ProtobufControllerSerializer extends FileControllerSerializer {
    ProtobufControllerSerializer() {
        super();
    }

    ProtobufControllerSerializer(Path baseDirectory) {
        super(baseDirectory);
    }

    @Override
    String fileExtension() {
        return ".proto";
    }

    @Override
    void encodeKeys(Keys keys, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                KeysSpec.encode(keys, ProtobufOutputStream.toStream(stream));
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    void encodeStore(Store store, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                StoreSpec.encode(store, ProtobufOutputStream.toStream(stream));
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    void encodeChat(Chat chat, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                ChatSpec.encode(chat, ProtobufOutputStream.toStream(stream));
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    void encodeNewsletter(Newsletter newsletter, Path path) {
        try {
            var tempFile = Files.createTempFile(path.getFileName().toString(), ".tmp");
            try(var stream = Files.newOutputStream(tempFile)) {
                NewsletterSpec.encode(newsletter, ProtobufOutputStream.toStream(stream));
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    Keys decodeKeys(Path keys) throws IOException {
        try(var stream = Files.newInputStream(keys)) {
            return KeysSpec.decode(new OptimizedProtobufInputStream(stream));
        }
    }

    @Override
    Store decodeStore(Path store) throws IOException {
        try(var stream = Files.newInputStream(store)) {
            return StoreSpec.decode(new OptimizedProtobufInputStream(stream));
        }
    }

    @Override
    Chat decodeChat(Path chat) throws IOException {
        try(var stream = Files.newInputStream(chat)) {
            return ChatSpec.decode(new OptimizedProtobufInputStream(stream));
        }
    }

    @Override
    Newsletter decodeNewsletter(Path newsletter) throws IOException {
        try(var stream = Files.newInputStream(newsletter)) {
            return NewsletterSpec.decode(new OptimizedProtobufInputStream(stream));
        }
    }

    // TODO: Remove me when I finish updating ModernProtobuf
    private static final class OptimizedProtobufInputStream extends ProtobufInputStream {
        private static final int MAX_VAR_INT_SIZE = 10;

        private final InputStream inputStream;
        private final boolean autoclose;
        private final long length;
        private long position;

        private final byte[] buffer;
        private int bufferReadPosition;
        private int bufferWritePosition;
        private int bufferLength;

        private OptimizedProtobufInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            this.autoclose = true;
            this.length = -1;
            this.buffer = new byte[MAX_VAR_INT_SIZE];
        }

        private OptimizedProtobufInputStream(InputStream inputStream, long length, byte[] buffer, int bufferReadPosition, int bufferWritePosition, int bufferLength) {
            this.inputStream = inputStream;
            this.autoclose = false;
            this.length = length;
            this.buffer = buffer;
            this.bufferReadPosition = bufferReadPosition;
            this.bufferWritePosition = bufferWritePosition;
            this.bufferLength = bufferLength;
        }

        @Override
        public byte readByte() {
            try {
                if(length != -1) {
                    position++;
                }

                if(bufferLength > 0) {
                    bufferLength--;
                    return buffer[bufferReadPosition++];
                }

                var result = (byte) inputStream.read();
                buffer[bufferWritePosition++ % buffer.length] = result;
                return result;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        @Override
        public ByteBuffer readBytes(int size) {
            try {
                return ByteBuffer.wrap(readStreamBytes(size));
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        @Override
        public ProtobufString.Lazy readString(int size) {
            try {
                return ProtobufString.lazy(readStreamBytes(size), 0, size);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        
        private byte[] readStreamBytes(int size) throws IOException {
            if (size < 0) {
                throw new IllegalArgumentException("Size must be non-negative.");
            }

            if (size == 0) {
                return new byte[0];
            }

            if (length != -1) {
                position += size;
            }

            byte[] result = new byte[size];
            int totalBytesRead = 0;

            int bytesFromBuffer = Math.min(size, bufferLength);
            if (bytesFromBuffer > 0) {
                System.arraycopy(buffer, bufferReadPosition, result, 0, bytesFromBuffer);
                totalBytesRead += bytesFromBuffer;
                bufferReadPosition += bytesFromBuffer;
                bufferLength -= bytesFromBuffer;
            }

            while (totalBytesRead < size) {
                int bytesReadFromStream = inputStream.read(result, totalBytesRead, size - totalBytesRead);
                if (bytesReadFromStream == -1) {
                    throw new IOException("Unexpected end of stream. Read " + totalBytesRead + ", expected " + size);
                }

                for (int i = 0; i < bytesReadFromStream; i++) {
                    buffer[bufferWritePosition % buffer.length] = result[totalBytesRead + i];
                    bufferWritePosition++;
                }
                totalBytesRead += bytesReadFromStream;
            }

            return result;
        }

        @Override
        public void mark() {
            this.bufferReadPosition = 0;
            this.bufferWritePosition = 0;
        }

        @Override
        public void rewind() {
            this.bufferReadPosition = 0;
            this.bufferLength = bufferWritePosition - bufferReadPosition;
            if(length != -1) {
                this.position -= bufferLength;
            }
        }

        @Override
        public boolean isFinished() {
            if (length != -1) {
                return position >= length;
            }

            mark();
            var result = readByte() == -1;
            rewind();
            return result;
        }

        @Override
        public OptimizedProtobufInputStream subStream(int size) {
            var result = new OptimizedProtobufInputStream(inputStream, size, buffer, bufferReadPosition, bufferWritePosition, bufferLength);
            if(length != -1) {
                position += size;
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            if(autoclose) {
                inputStream.close();
            }
        }
    }
}
