package it.auties.github.dev;

import it.auties.whatsapp.crypto.AesCbc;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

@Log
public class CryptoTest {
    @Test
    public void test() {
        var hex = HexFormat.of();
        var iv = hex.parseHex("85bd9a25db9a91c00f94a026cc26d72b");
        var data = hex.parseHex("e0c17823fce3214ee57599e362b738e48f97f428225f0899d715f7b6ae2e2cad");
        var key = hex.parseHex("532f86b1bf88b0ba1be9aac708862266682252a2a2c0f8cf690dda6b4a5f9f51");

        var encrypted = AesCbc.encrypt(iv, data, key);
        System.out.println(hex.formatHex(encrypted));

        var decrypted = AesCbc.decrypt(iv, encrypted, key);
        System.out.println(hex.formatHex(decrypted));
    }
}
