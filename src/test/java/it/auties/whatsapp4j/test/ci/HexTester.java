package it.auties.whatsapp4j.test.ci;

import org.bouncycastle.util.encoders.Hex;

import java.lang.reflect.Array;
import java.util.Arrays;

public class HexTester {
    public static void main(String[] args) {
        var hex = Hex.decode("57410502");
        System.out.println(hex.length);
        System.out.println(Arrays.toString(hex));
        System.out.println(new String(hex));
        System.out.println(Arrays.toString("WA\5\2".getBytes()));
    }
}
