package it.auties.whatsapp.binary;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.util.Buffers;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static it.auties.whatsapp.binary.BinaryArray.of;

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
    BinaryArray raw;

    /**
     * The raw buffer array sliced at [3, {@code length})
     */
    @NonNull
    LinkedList<BinaryArray> decoded;

    /**
     * Constructs a new instance of this wrapper from a buffer array
     *
     * @param array the non-null buffer array
     */
    public BinaryMessage(@NonNull BinaryArray array) {
        this.raw = array;
        var buffer = Buffers.newBuffer(array);
        var decoded = new LinkedList<BinaryArray>();
        while (buffer.readableBytes() >= 3) {
            var size = decodeLength(buffer);
            var frame = Buffers.readBinary(buffer, size);
            decoded.add(frame);
        }

        Validate.isTrue(buffer.readableBytes() == 0,
                "Incomplete message with a delta of %s bytes",
                buffer.readableBytes());
        this.decoded = decoded;
    }

    private int decodeLength(ByteBuf buffer) {
        return (buffer.readByte() << 16) | buffer.readUnsignedShort();
    }

    /**
     * Constructs a new instance of this wrapper from an array of bytes
     *
     * @param array the non-null array of bytes
     */
    public BinaryMessage(byte @NonNull [] array) {
        this(of(array));
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

    private Node toNode(BinaryArray encoded, WhatsappKeys keys) {
        var plainText = AesGmc.with(keys.readKey(), keys.readCounter(true), false)
                .process(encoded.data());
        return decoder.decode(plainText);
    }
}
