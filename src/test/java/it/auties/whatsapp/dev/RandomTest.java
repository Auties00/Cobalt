package it.auties.whatsapp.dev;

import it.auties.bytes.Bytes;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.binary.BinarySync;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.LTHash;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.sync.LTHashState;
import it.auties.whatsapp.util.BytesHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HexFormat;

public class RandomTest {
    public static void main(String[] args) throws IOException {
        var a = new byte[]{51, 10, 33, 5, 44, 63, 18, 91, 16, 109, 71, 0, 75, 24, 55, 41, 114, -123, 41, -79, 88, -92, 67, 91, -120, -114, 10, -64, -71, 4, -103, 48, 76, 73, -20, 107, 34, 48, 84, 90, 81, -77, 122, -118, 6, -73, -42, -93, -3, 63, -21, -57, -44, 65, -60, -96, -1, -9, 30, -1, -10, 29, 107, -67, 97, 44, 21, 92, -68, 73, 31, -124, 47, -88, 15, -22, 17, 70, 105, -28, 26, -109, 69, 48, -31, 64, -80, 50, 57, -76, 42, -5, -67, 112};
        var b = new int[]{51, 10, 33, 5, 44, 63, 18, 91, 16, 109, 71, 0, 75, 24, 55, 41, 114, 133, 41, 177, 88, 164, 67, 91, 136, 142, 10, 192, 185, 4, 153, 48, 76, 73, 236, 107, 16, 8, 24, 0, 34, 48, 28, 5, 27, 75, 0, 59, 86, 198, 68, 93, 230, 209, 141, 176, 145, 8, 25, 67, 213, 50, 228, 174, 227, 151, 120, 37, 171, 161, 246, 190, 149, 141, 227, 151, 103, 48, 17, 54, 159, 39, 175, 200, 211, 232, 119, 39, 248, 70, 74, 235, 131, 255, 2, 69, 13, 166};
        System.out.println(SignalMessage.ofSerialized(a));
        System.out.println(SignalMessage.ofSerialized(toBytes(b)));
    }

    private static byte[] toBytes(int[] a){
        var b = new byte[a.length];
        for(var x = 0; x < a.length; x++){
            b[x] = (byte) a[x];
        }

        return b;
    }
}
