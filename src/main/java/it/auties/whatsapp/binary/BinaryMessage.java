package it.auties.whatsapp.binary;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.socket.Node;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;

/**
 * A wrapper object used to represent messages received by Whatsapp
 */
@Value
@Accessors(fluent = true)
public class BinaryMessage {
    /**
     * The node decoder
     */
    private static BinaryDecoder decoder = new BinaryDecoder();

    /**
     * The raw buffer array used to construct this object
     */
    @NonNull
    Bytes raw;

    /**
     * The raw buffer array sliced at [3, {@code length})
     */
    @NonNull
    LinkedList<Bytes> decoded;

    /**
     * Constructs a new instance of this wrapper from a buffer array
     *
     * @param raw the non-null buffer array
     */
    public BinaryMessage(@NonNull Bytes raw) {
        this.raw = raw;
        var decoded = new LinkedList<Bytes>();
        while (raw.readableBytes() >= 3) {
            var size = decodeLength(raw);
            if(size < 0){
                continue;
            }

            decoded.add(raw.readBuffer(size));
        }

        this.decoded = decoded;
    }

    private int decodeLength(Bytes buffer) {
        return (buffer.readByte() << 16) | buffer.readUnsignedShort();
    }

    /**
     * Constructs a new instance of this wrapper from an array of bytes
     *
     * @param array the non-null array of bytes
     */
    public BinaryMessage(byte @NonNull [] array) {
        this(Bytes.of(array));
    }

    /**
     * Converts {@link BinaryMessage#decoded()} to a list of nodes
     *
     * @param keys the keys used to decipher each entry
     * @return a non-null list of nodes
     */
    public List<Node> toNodes(@NonNull WhatsappKeys keys) {
        return decoded.stream()
                .map(encoded -> toNode(encoded, keys))
                .toList();
    }

    private Node toNode(Bytes encoded, WhatsappKeys keys) {
        var plainText = AesGmc.with(keys.readKey(), keys.readCounter(true), false)
                .process(encoded.toByteArray());
        return decoder.decode(plainText);
    }
}
