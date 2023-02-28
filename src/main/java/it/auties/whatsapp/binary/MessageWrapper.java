package it.auties.whatsapp.binary;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.model.request.Node;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;

@Value
@Accessors(fluent = true)
public class MessageWrapper {
    @NonNull Bytes raw;

    @NonNull LinkedList<Bytes> decoded;

    public MessageWrapper(byte @NonNull [] array) {
        this(Bytes.of(array));
    }

    public MessageWrapper(@NonNull Bytes raw) {
        this.raw = raw;
        var decoded = new LinkedList<Bytes>();
        while (raw.readableBytes() >= 3) {
            var length = decodeLength(raw);
            if (length < 0) {
                continue;
            }
            decoded.add(raw.readBuffer(length));
        }
        this.decoded = decoded;
    }

    private int decodeLength(Bytes buffer) {
        return (buffer.readByte() << 16) | buffer.readUnsignedShort();
    }

    public List<Node> toNodes(@NonNull Keys keys) {
        return decoded.stream().map(encoded -> toNode(encoded, keys)).toList();
    }

    private Node toNode(Bytes encoded, Keys keys) {
        var plainText = AesGmc.decrypt(keys.readCounter(true), encoded.toByteArray(), keys.readKey().toByteArray());
        var decoder = new Decoder(plainText);
        return decoder.readNode();
    }
}
