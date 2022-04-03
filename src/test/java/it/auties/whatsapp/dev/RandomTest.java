package it.auties.whatsapp.dev;

import java.io.IOException;
import java.util.Arrays;

public class RandomTest {
    public static void main(String[] args) throws IOException {
        var a = new int[]{200, 86, 184, 26, 101, 28, 16, 19, 28, 86, 48, 138, 94, 245, 187, 149, 68, 107, 141, 53, 172, 47, 189, 69, 199, 58, 13, 184, 84, 131, 157, 243, 151, 148, 26, 47, 0, 244, 92, 242, 244, 28, 117, 243, 199, 242, 214, 187};
        var b = new byte[]{-56, 86, -72, 26, 101, 28, 16, 19, 28, 86, 48, -118, 94, -11, -69, -107, 68, 107, -115, 53, -84, 47, -67, 69, -57, 58, 13, -72, 84, -125, -99, -13, -105, -108, 26, 47, 0, -12, 92, -14, -12, 28, 117, -13, -57, -14, -42, -69};
        System.out.println(Arrays.toString(toBytes(a)));
        System.out.println(Arrays.toString(b));
    }

    private static byte[] toBytes(int[] a) {
        var b = new byte[a.length];
        for (var x = 0; x < a.length; x++) {
            b[x] = (byte) a[x];
        }

        return b;
    }
}
