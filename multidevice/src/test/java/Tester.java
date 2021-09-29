import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import it.auties.whatsapp4j.common.utils.Validate;
import lombok.SneakyThrows;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.interfaces.XECPrivateKey;
import java.util.Arrays;
import java.util.HexFormat;

import static java.math.BigInteger.*;

public class Tester {
    private static final HexFormat HEX = HexFormat.of();
    private static final BigInteger[] gf0 = createContainer(16);
    private static final BigInteger[] gf1 = new BigInteger[]{ONE, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO};
    private static final BigInteger[] D = new BigInteger[]{new BigInteger("78a3", 16), new BigInteger("1359", 16), new BigInteger("4dca", 16), new BigInteger("75eb", 16), new BigInteger("d8ab", 16), new BigInteger("4141", 16), new BigInteger("0a4d", 16), new BigInteger("0070", 16), new BigInteger("e898", 16), new BigInteger("7779", 16), new BigInteger("4079", 16), new BigInteger("8cc7", 16), new BigInteger("fe73", 16), new BigInteger("2b6f", 16), new BigInteger("6cee", 16), new BigInteger("5203", 16)};
    private static final BigInteger[] D2 = new BigInteger[]{new BigInteger("f159", 16), new BigInteger("26b2", 16), new BigInteger("9b94", 16), new BigInteger("ebd6", 16), new BigInteger("b156", 16), new BigInteger("8283", 16), new BigInteger("149a", 16), new BigInteger("00e0", 16), new BigInteger("d130", 16), new BigInteger("eef3", 16), new BigInteger("80f2", 16), new BigInteger("198e", 16), new BigInteger("fce7", 16), new BigInteger("56df", 16), new BigInteger("d9dc", 16), new BigInteger("2406", 16)};
    private static final BigInteger[] X = new BigInteger[]{new BigInteger("d51a", 16), new BigInteger("8f25", 16), new BigInteger("2d60", 16), new BigInteger("c956", 16), new BigInteger("a7b2", 16), new BigInteger("9525", 16), new BigInteger("c760", 16), new BigInteger("692c", 16), new BigInteger("dc5c", 16), new BigInteger("fdd6", 16), new BigInteger("e231", 16), new BigInteger("c0a4", 16), new BigInteger("53fe", 16), new BigInteger("cd6e", 16), new BigInteger("36d3", 16), new BigInteger("2169", 16)};
    private static final BigInteger[] Y = new BigInteger[]{new BigInteger("6658", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16), new BigInteger("6666", 16)};
    private static final BigInteger[] I = new BigInteger[]{new BigInteger("a0b0", 16), new BigInteger("4a0e", 16), new BigInteger("1b27", 16), new BigInteger("c4ee", 16), new BigInteger("e478", 16), new BigInteger("ad2f", 16), new BigInteger("1806", 16), new BigInteger("2f43", 16), new BigInteger("d7a7", 16), new BigInteger("3dfb", 16), new BigInteger("0099", 16), new BigInteger("2b4d", 16), new BigInteger("df0b", 16), new BigInteger("4fc1", 16), new BigInteger("2480", 16), new BigInteger("2b83", 16)};
    private static final BigInteger[] K = new BigInteger[]{new BigInteger("428a2f98", 16), new BigInteger("d728ae22", 16), new BigInteger("71374491", 16), new BigInteger("23ef65cd", 16), new BigInteger("b5c0fbcf", 16), new BigInteger("ec4d3b2f", 16), new BigInteger("e9b5dba5", 16), new BigInteger("8189dbbc", 16), new BigInteger("3956c25b", 16), new BigInteger("f348b538", 16), new BigInteger("59f111f1", 16), new BigInteger("b605d019", 16), new BigInteger("923f82a4", 16), new BigInteger("af194f9b", 16), new BigInteger("ab1c5ed5", 16), new BigInteger("da6d8118", 16), new BigInteger("d807aa98", 16), new BigInteger("a3030242", 16), new BigInteger("12835b01", 16), new BigInteger("45706fbe", 16), new BigInteger("243185be", 16), new BigInteger("4ee4b28c", 16), new BigInteger("550c7dc3", 16), new BigInteger("d5ffb4e2", 16), new BigInteger("72be5d74", 16), new BigInteger("f27b896f", 16), new BigInteger("80deb1fe", 16), new BigInteger("3b1696b1", 16), new BigInteger("9bdc06a7", 16), new BigInteger("25c71235", 16), new BigInteger("c19bf174", 16), new BigInteger("cf692694", 16), new BigInteger("e49b69c1", 16), new BigInteger("9ef14ad2", 16), new BigInteger("efbe4786", 16), new BigInteger("384f25e3", 16), new BigInteger("0fc19dc6", 16), new BigInteger("8b8cd5b5", 16), new BigInteger("240ca1cc", 16), new BigInteger("77ac9c65", 16), new BigInteger("2de92c6f", 16), new BigInteger("592b0275", 16), new BigInteger("4a7484aa", 16), new BigInteger("6ea6e483", 16), new BigInteger("5cb0a9dc", 16), new BigInteger("bd41fbd4", 16), new BigInteger("76f988da", 16), new BigInteger("831153b5", 16), new BigInteger("983e5152", 16), new BigInteger("ee66dfab", 16), new BigInteger("a831c66d", 16), new BigInteger("2db43210", 16), new BigInteger("b00327c8", 16), new BigInteger("98fb213f", 16), new BigInteger("bf597fc7", 16), new BigInteger("beef0ee4", 16), new BigInteger("c6e00bf3", 16), new BigInteger("3da88fc2", 16), new BigInteger("d5a79147", 16), new BigInteger("930aa725", 16), new BigInteger("06ca6351", 16), new BigInteger("e003826f", 16), new BigInteger("14292967", 16), new BigInteger("0a0e6e70", 16), new BigInteger("27b70a85", 16), new BigInteger("46d22ffc", 16), new BigInteger("2e1b2138", 16), new BigInteger("5c26c926", 16), new BigInteger("4d2c6dfc", 16), new BigInteger("5ac42aed", 16), new BigInteger("53380d13", 16), new BigInteger("9d95b3df", 16), new BigInteger("650a7354", 16), new BigInteger("8baf63de", 16), new BigInteger("766a0abb", 16), new BigInteger("3c77b2a8", 16), new BigInteger("81c2c92e", 16), new BigInteger("47edaee6", 16), new BigInteger("92722c85", 16), new BigInteger("1482353b", 16), new BigInteger("a2bfe8a1", 16), new BigInteger("4cf10364", 16), new BigInteger("a81a664b", 16), new BigInteger("bc423001", 16), new BigInteger("c24b8b70", 16), new BigInteger("d0f89791", 16), new BigInteger("c76c51a3", 16), new BigInteger("0654be30", 16), new BigInteger("d192e819", 16), new BigInteger("d6ef5218", 16), new BigInteger("d6990624", 16), new BigInteger("5565a910", 16), new BigInteger("f40e3585", 16), new BigInteger("5771202a", 16), new BigInteger("106aa070", 16), new BigInteger("32bbd1b8", 16), new BigInteger("19a4c116", 16), new BigInteger("b8d2d0c8", 16), new BigInteger("1e376c08", 16), new BigInteger("5141ab53", 16), new BigInteger("2748774c", 16), new BigInteger("df8eeb99", 16), new BigInteger("34b0bcb5", 16), new BigInteger("e19b48a8", 16), new BigInteger("391c0cb3", 16), new BigInteger("c5c95a63", 16), new BigInteger("4ed8aa4a", 16), new BigInteger("e3418acb", 16), new BigInteger("5b9cca4f", 16), new BigInteger("7763e373", 16), new BigInteger("682e6ff3", 16), new BigInteger("d6b2b8a3", 16), new BigInteger("748f82ee", 16), new BigInteger("5defb2fc", 16), new BigInteger("78a5636f", 16), new BigInteger("43172f60", 16), new BigInteger("84c87814", 16), new BigInteger("a1f0ab72", 16), new BigInteger("8cc70208", 16), new BigInteger("1a6439ec", 16), new BigInteger("90befffa", 16), new BigInteger("23631e28", 16), new BigInteger("a4506ceb", 16), new BigInteger("de82bde9", 16), new BigInteger("bef9a3f7", 16), new BigInteger("b2c67915", 16), new BigInteger("c67178f2", 16), new BigInteger("e372532b", 16), new BigInteger("ca273ece", 16), new BigInteger("ea26619c", 16), new BigInteger("d186b8c7", 16), new BigInteger("21c0c207", 16), new BigInteger("eada7dd6", 16), new BigInteger("cde0eb1e", 16), new BigInteger("f57d4f7f", 16), new BigInteger("ee6ed178", 16), new BigInteger("06f067aa", 16), new BigInteger("72176fba", 16), new BigInteger("0a637dc5", 16), new BigInteger("a2c898a6", 16), new BigInteger("113f9804", 16), new BigInteger("bef90dae", 16), new BigInteger("1b710b35", 16), new BigInteger("131c471b", 16), new BigInteger("28db77f5", 16), new BigInteger("23047d84", 16), new BigInteger("32caab7b", 16), new BigInteger("40c72493", 16), new BigInteger("3c9ebe0a", 16), new BigInteger("15c9bebc", 16), new BigInteger("431d67c4", 16), new BigInteger("9c100d4c", 16), new BigInteger("4cc5d4be", 16), new BigInteger("cb3e42b6", 16), new BigInteger("597f299c", 16), new BigInteger("fc657e2a", 16), new BigInteger("5fcb6fab", 16), new BigInteger("3ad6faec", 16), new BigInteger("6c44198c", 16), new BigInteger("4a475817", 16)};
    private static final BigInteger[] L = new BigInteger[]{new BigInteger("ed", 16), new BigInteger("d3", 16), new BigInteger("f5", 16), new BigInteger("5c", 16), new BigInteger("1a", 16), new BigInteger("63", 16), new BigInteger("12", 16), new BigInteger("58", 16), new BigInteger("d6", 16), new BigInteger("9c", 16), new BigInteger("f7", 16), new BigInteger("a2", 16), new BigInteger("de", 16), new BigInteger("f9", 16), new BigInteger("de", 16), new BigInteger("14", 16), ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, valueOf(10)};

    public static void main(String[] args) {
        var randomKey = CypherUtils.randomKeyPair().getPrivate();
        var randomMessage = BinaryArray.random(32).data();
        var signature = calculateSignature(randomKey, randomMessage);
        System.out.printf("Key: %s%nMessage: %s%nSignature: %s%n", HEX.formatHex(CypherUtils.raw(randomKey)), HEX.formatHex(randomMessage), HEX.formatHex(signature));
    }

    @SneakyThrows
    private static byte[] calculateSignature(PrivateKey key, byte[] data) {
        var rawPrivateKey = ((XECPrivateKey) key).getScalar().orElseThrow();
        return sign(rawPrivateKey, data);
    }

    @SneakyThrows
    private static byte[] sign(byte[] secretKey, byte[] msg) {
        Validate.isTrue(secretKey.length == 32, "Invalid key length");
        var buf = new byte[msg.length + 64];
        curve25519_sign(buf, msg, msg.length, secretKey);
        var signature = new byte[64];
        System.arraycopy(buf, 0, signature, 0, signature.length);
        return signature;
    }

    @SneakyThrows
    private static int curve25519_sign(byte[] sm, byte[] m, int n, byte[] sk) {
        var edsk = new byte[64];
        var p = createWrapper();
        System.arraycopy(sk, 0, edsk, 0, 32);

        edsk[0] &= 248;
        edsk[31] &= 127;
        edsk[31] |= 64;

        scalarbase(p, edsk);
        pack(edsk, p);

        // Remember sign bit.
        var signBit = edsk[63] & 128;
        var smlen = crypto_sign_direct(sm, m, n, edsk);

        // Copy sign bit from public key into signature.
        sm[63] |= signBit;
        return smlen;
    }

    private static int crypto_sign_direct(byte[] sm, byte[] m, int n, byte[] sk) {
        var h = new byte[64];
        var r = new byte[64];
        var i = 0;
        var j = 0;
        var x = createContainer(64);
        var p = createWrapper();

        for (i = 0; i < n; i++) sm[64 + i] = m[i];
        for (i = 0; i < 32; i++) sm[32 + i] = sk[i];

        crypto_hash(r, sm, n + 32);
        reduce(r);
        scalarbase(p, r);
        pack(sm, p);

        for (i = 0; i < 32; i++) sm[i + 32] = sk[32 + i];
        crypto_hash(h, sm, n + 64);
        reduce(h);

        for (i = 0; i < 64; i++) x[i] = ZERO;
        for (i = 0; i < 32; i++) x[i] = valueOf(r[i]);
        for (i = 0; i < 32; i++) {
            for (j = 0; j < 32; j++) {
                x[i + j] = x[i + j].add(valueOf(h[i] * sk[j]));
            }
        }

        modL(sm, x);
        return n + 64;
    }

    private static void reduce(byte[] r) {
        var x = createContainer(64);
        for (var i = 0; i < 64; i++) x[i] = valueOf(r[i]);
        for (var i = 0; i < 64; i++) r[i] = 0;
        modL(r, x);
    }

    private static void modL(byte[] r, BigInteger[] x) {
        BigInteger carry;
        int i, j, k;
        for (i = 63; i >= 32; --i) {
            carry = ZERO;
            for (j = i - 32, k = i - 12; j < k; ++j) {
                x[j] = x[j].add(carry.subtract(x[i]
                        .multiply(valueOf(16))
                        .multiply(L[j - (i - 32)])));
                carry = x[j].add(valueOf(128)).shiftRight(8);
                x[j] = x[j].subtract(carry.multiply(valueOf(256)));
            }

            x[j] = x[j].add(carry);
            x[i] = ZERO;
        }
        carry = ZERO;
        for (j = 0; j < 32; j++) {
            x[j] = x[j].add(carry.subtract(x[31].shiftRight(4)).multiply(L[j]));
            carry = x[j].shiftRight(8);
            x[j] = x[j].and(valueOf(255));
        }
        for (j = 0; j < 32; j++) x[j] = x[j].subtract(carry.multiply(L[j]));
        for (i = 0; i < 32; i++) {
            x[i+1] = x[i+1].add(x[i].shiftRight(8));
            r[i] = (byte) x[i].and(valueOf(255)).and(new BigInteger("ff", 16)).intValue();
        }
    }

    private static int crypto_hash(byte[] out, byte[] m, int n) {
        var hh = createContainer(8);
        var hl = createContainer(8);
        var x = new byte[256];
        var i = 0;
        var b = valueOf(n);

        hh[0] = new BigInteger("6a09e667", 16);
        hh[1] = new BigInteger("bb67ae85", 16);
        hh[2] = new BigInteger("3c6ef372", 16);
        hh[3] = new BigInteger("a54ff53a", 16);
        hh[4] = new BigInteger("510e527f", 16);
        hh[5] = new BigInteger("9b05688c", 16);
        hh[6] = new BigInteger("1f83d9ab", 16);
        hh[7] = new BigInteger("5be0cd19", 16);

        hl[0] = new BigInteger("f3bcc908", 16);
        hl[1] = new BigInteger("84caa73b", 16);
        hl[2] = new BigInteger("fe94f82b", 16);
        hl[3] = new BigInteger("5f1d36f1", 16);
        hl[4] = new BigInteger("ade682d1", 16);
        hl[5] = new BigInteger("2b3e6c1f", 16);
        hl[6] = new BigInteger("fb41bd6b", 16);
        hl[7] = new BigInteger("137e2179", 16);

        crypto_hashblocks_hl(hh, hl, m, n);
        n %= 128;

        for (i = 0; i < n; i++) x[i] = m[b.subtract(valueOf(n)).add(valueOf(i)).intValue()];
        x[n] = (byte) 128;

        n = 256 - 128 * (n < 112 ? 1 : 0);
        x[n - 9] = 0;
        ts64(x, n - 8, b.divide(new BigInteger("20000000", 16)), b.shiftLeft(3));
        crypto_hashblocks_hl(hh, hl, x, n);

        for (i = 0; i < 8; i++) ts64(out, 8 * i, hh[i], hl[i]);

        return 0;
    }

    private static void ts64(byte[] x, int i, BigInteger h, BigInteger l) {
        x[i] = (byte) h.shiftRight(24).and(new BigInteger("ff", 16)).intValue();
        x[i + 1] = (byte) h.shiftRight(16).and(new BigInteger("ff", 16)).intValue();
        x[i + 2] = (byte) h.shiftRight(8).and(new BigInteger("ff", 16)).intValue();
        x[i + 3] = (byte) h.and(new BigInteger("ff", 16)).intValue();
        x[i + 4] = (byte) l.shiftRight(24).and(new BigInteger("ff", 16)).intValue();
        x[i + 5] = (byte) l.shiftRight(16).and(new BigInteger("ff", 16)).intValue();
        x[i + 6] = (byte) l.shiftRight(8).and(new BigInteger("ff", 16)).intValue();
        x[i + 7] = (byte) l.and(new BigInteger("ff", 16)).intValue();
    }

    private static int crypto_hashblocks_hl(BigInteger[] hh, BigInteger[] hl, byte[] m, int n) {
        var wh = createContainer(16);
        var wl = createContainer(16);
        int i;
        var bh0 = ZERO;
        var bh1 = ZERO;
        var bh2 = ZERO;
        var bh3 = ZERO;
        var bh4 = ZERO;
        var bh5 = ZERO;
        var bh6 = ZERO;
        var bh7 = ZERO;
        var bl0 = ZERO;
        var bl1 = ZERO;
        var bl2 = ZERO;
        var bl3 = ZERO;
        var bl4 = ZERO;
        var bl5 = ZERO;
        var bl6 = ZERO;
        var bl7 = ZERO;
        var th = ZERO;
        var tl = ZERO;
        var a = ZERO;
        var b = ZERO;
        var c = ZERO;
        var d = ZERO;
        BigInteger l, h;
        int j;
        BigInteger ah0 = hh[0], ah1 = hh[1], ah2 = hh[2], ah3 = hh[3], ah4 = hh[4], ah5 = hh[5], ah6 = hh[6], ah7 = hh[7], al0 = hl[0], al1 = hl[1], al2 = hl[2], al3 = hl[3], al4 = hl[4], al5 = hl[5], al6 = hl[6], al7 = hl[7];

        var pos = 0;
        while (n >= 128) {
            for (i = 0; i < 16; i++) {
                j = 8 * i + pos;
                wh[i] = valueOf((m[j] << 24) | (m[j + 1] << 16) | (m[j + 2] << 8) | m[j + 3]);
                wl[i] = valueOf((m[j + 4] << 24) | (m[j + 5] << 16) | (m[j + 6] << 8) | m[j + 7]);
            }
            for (i = 0; i < 80; i++) {
                bh0 = ah0;
                bh1 = ah1;
                bh2 = ah2;
                bh3 = ah3;
                bh4 = ah4;
                bh5 = ah5;
                bh6 = ah6;
                bh7 = ah7;

                bl0 = al0;
                bl1 = al1;
                bl2 = al2;
                bl3 = al3;
                bl4 = al4;
                bl5 = al5;
                bl6 = al6;
                bl7 = al7;

                // add
                h = ah7;
                l = al7;

                a = l.and(new BigInteger("ffff", 16));
                b = lrsh(l, 16);
                c = h.and(new BigInteger("ffff", 16));
                d = lrsh(h, 16);

                // Sigma1
                h = lrsh(ah4, 14).or(al4.shiftLeft(32 - 14).xor(lrsh(ah4, 18).or(al4.shiftLeft(32 - 18))).xor(lrsh(al4, 41 - 32)).or(ah4.shiftLeft(32 - (41 - 32))));
                l = lrsh(al4, 14).or(ah4.shiftLeft(32 - 14).xor(lrsh(al4, 18).or(ah4.shiftLeft(32 - 18))).xor(lrsh(ah4, 41 - 32)).or(al4.shiftLeft(32 - (41 - 32))));
                a = a.add(l.and(new BigInteger("ffff", 16)));
                b = b.add(lrsh(l, 16));
                c = c.add(h.and(new BigInteger("ffff", 16)));
                d = d.add(lrsh(h, 16));

                // Ch
                h = ah4.and(ah5).xor(ah4.not().and(ah6));
                l = al4.and(al5).xor(al4.not().and(al6));

                a = a.add(l.and(new BigInteger("ffff", 16)));
                b = b.add(lrsh(l, 16));
                c = c.add(h.and(new BigInteger("ffff", 16)));
                d = d.add(lrsh(h, 16));

                // K
                h = K[i * 2];
                l = K[i * 2 + 1];

                a = a.add(l.and(new BigInteger("ffff", 16)));
                b = b.add(lrsh(l, 16));
                c = c.add(h.and(new BigInteger("ffff", 16)));
                d = d.add(lrsh(h, 16));

                // w
                h = wh[i % 16];
                l = wl[i % 16];

                a = a.add(l.and(new BigInteger("ffff", 16)));
                b = b.add(lrsh(l, 16));
                c = c.add(h.and(new BigInteger("ffff", 16)));
                d = d.add(lrsh(h, 16));

                b = b.add(lrsh(a, 16));
                c = c.add(lrsh(b, 16));
                d = d.add(lrsh(c, 16));

                th = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
                tl  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

                // add
                h = th;
                l = tl;

                a = l.and(new BigInteger("ffff", 16));
                b = lrsh(l, 16);
                c = h.and(new BigInteger("ffff", 16));
                d = lrsh(h, 16);

                // Sigma0
                h = lrsh(ah0, 28).or(al0.shiftLeft(32 - 28).xor(lrsh(al0, 34 - 32).or(ah0.shiftLeft(32 - (34 - 32)))).xor(lrsh(al0, 39 - 32)).or(ah0.shiftLeft(32 - (39 - 32))));
                l = lrsh(al0, 28).or(ah0.shiftLeft(32 - 28).xor(lrsh(ah0, 34 - 32).or(al0.shiftLeft(32 - (34 - 32)))).xor(lrsh(ah0, 39 - 32)).or(al0.shiftLeft(32 - (39 - 32))));

                a = a.add(l.and(new BigInteger("ffff", 16)));
                b = b.add(lrsh(l, 16));
                c = c.add(h.and(new BigInteger("ffff", 16)));
                d = d.add(lrsh(h, 16));

                // Maj
                h = ah0.and(ah1).xor(ah0).and(ah2).xor(ah1).and(ah2);
                l = al0.and(al1).xor(al0).and(al2).xor(al1).and(al2);

                a = a.add(l.and(new BigInteger("ffff", 16)));
                b = b.add(lrsh(l, 16));
                c = c.add(h.and(new BigInteger("ffff", 16)));
                d = d.add(lrsh(h, 16));

                b = b.add(lrsh(a, 16));
                c = c.add(lrsh(b, 16));
                d = d.add(lrsh(c, 16));

                bh7 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
                bl7  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

                // add
                h = bh3;
                l = bl3;

                a = l.and(new BigInteger("ffff", 16));
                b = lrsh(l, 16);
                c = h.and(new BigInteger("ffff", 16));
                d = lrsh(h, 16);

                h = th;
                l = tl;

                a = a.add(l.and(new BigInteger("ffff", 16)));
                b = b.add(lrsh(l, 16));
                c = c.add(h.and(new BigInteger("ffff", 16)));
                d = d.add(lrsh(h, 16));

                b = b.add(lrsh(a, 16));
                c = c.add(lrsh(b, 16));
                d = d.add(lrsh(c, 16));

                bh3 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
                bl3  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

                ah1 = bh0;
                ah2 = bh1;
                ah3 = bh2;
                ah4 = bh3;
                ah5 = bh4;
                ah6 = bh5;
                ah7 = bh6;
                ah0 = bh7;

                al1 = bl0;
                al2 = bl1;
                al3 = bl2;
                al4 = bl3;
                al5 = bl4;
                al6 = bl5;
                al7 = bl6;
                al0 = bl7;

                if (i % 16 == 15) {
                    for (j = 0; j < 16; j++) {
                        // add
                        h = wh[j];
                        l = wl[j];

                        a = a.add(l.and(new BigInteger("ffff", 16)));
                        b = b.add(lrsh(l, 16));
                        c = c.add(h.and(new BigInteger("ffff", 16)));
                        d = d.add(lrsh(h, 16));

                        h = wh[(j + 9) % 16];
                        l = wl[(j + 9) % 16];

                        a = a.add(l.and(new BigInteger("ffff", 16)));
                        b = b.add(lrsh(l, 16));
                        c = c.add(h.and(new BigInteger("ffff", 16)));
                        d = d.add(lrsh(h, 16));

                        // sigma0
                        th = wh[(j + 1) % 16];
                        tl = wl[(j + 1) % 16];
                        h = lrsh(th, 1).or(tl.shiftLeft(32 - 1)).xor(lrsh(th, 8)).or(tl.shiftLeft(32 - 8)).xor(lrsh(th, 7));
                        l = lrsh(tl, 1).or(th.shiftLeft(32 - 1)).xor(lrsh(tl, 8)).or(th.shiftLeft(32 - 8)).xor(lrsh(th, 7)).or(th.shiftLeft(32 - 7));

                        a = a.add(l.and(new BigInteger("ffff", 16)));
                        b = b.add(lrsh(l, 16));
                        c = c.add(h.and(new BigInteger("ffff", 16)));
                        d = d.add(lrsh(h, 16));

                        // sigma1
                        th = wh[(j + 14) % 16];
                        tl = wl[(j + 14) % 16];
                        h = lrsh(th, 19).or(tl.shiftLeft(32 - 19)).xor(lrsh(th, 61 - 32)).or(tl.shiftLeft(32 - (61 - 32))).xor(lrsh(th, 6));
                        l = lrsh(tl, 19).or(th.shiftLeft(32 - 19)).xor(lrsh(tl, 61 - 32)).or(th.shiftLeft(32 - (61 - 32))).xor(lrsh(th, 6)).or(th.shiftLeft(32 - 6));

                        a = a.add(l.and(new BigInteger("ffff", 16)));
                        b = b.add(lrsh(l, 16));
                        c = c.add(h.and(new BigInteger("ffff", 16)));
                        d = d.add(lrsh(h, 16));

                        b = b.add(lrsh(a, 16));
                        c = c.add(lrsh(b, 16));
                        d = d.add(lrsh(c, 16));

                        wh[j] = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
                        wl[j]  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));
                    }
                }
            }

            // add
            h = ah0;
            l = al0;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[0];
            l = hl[0];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[0] = ah0 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[0] = al0  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            h = ah1;
            l = al1;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[1];
            l = hl[1];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[1] = ah1 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[1] = al1  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            h = ah2;
            l = al2;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[2];
            l = hl[2];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[2] = ah2 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[2] = al2  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            h = ah3;
            l = al3;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[3];
            l = hl[3];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[3] = ah3 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[3] = al3  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            h = ah4;
            l = al4;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[4];
            l = hl[4];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[4] = ah4 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[4] = al4  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            h = ah5;
            l = al5;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[5];
            l = hl[5];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[5] = ah5 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[5] = al5  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            h = ah6;
            l = al6;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[6];
            l = hl[6];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[6] = ah6 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[6] = al6  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            h = ah7;
            l = al7;

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            h = hh[7];
            l = hl[7];

            a = a.add(l.and(new BigInteger("ffff", 16)));
            b = b.add(lrsh(l, 16));
            c = c.add(h.and(new BigInteger("ffff", 16)));
            d = d.add(lrsh(h, 16));

            b = b.add(lrsh(a, 16));
            c = c.add(lrsh(b, 16));
            d = d.add(lrsh(c, 16));

            hh[7] = ah7 = c.and(valueOf(0xffff)).or(d.shiftLeft(16));
            hl[7] = al7  = a.and(valueOf(0xffff)).or(b.shiftLeft(16));

            pos += 128;
            n -= 128;
        }
        return n;
    }


    @SneakyThrows
    private static void scalarbase(BigInteger[][] p, byte[] s) {
        var q = createWrapper();
        set25519(q[0], X);
        set25519(q[1], Y);
        set25519(q[2], gf1);
        M(q[3], X, Y);
        scalarmult(p, q, s);
    }

    private static void set25519(BigInteger[] r, BigInteger[] a) {
        System.arraycopy(a, 0, r, 0, 16);
    }

    private static void M(BigInteger[] o, BigInteger[] a, BigInteger[] b) {
        BigInteger t0 = BigInteger.ZERO, t1 = BigInteger.ZERO, t2 = BigInteger.ZERO, t3 = BigInteger.ZERO, t4 = BigInteger.ZERO, t5 = BigInteger.ZERO, t6 = BigInteger.ZERO, t7 = BigInteger.ZERO, t8 = BigInteger.ZERO, t9 = BigInteger.ZERO, t10 = BigInteger.ZERO, t11 = BigInteger.ZERO, t12 = BigInteger.ZERO, t13 = BigInteger.ZERO, t14 = BigInteger.ZERO, t15 = BigInteger.ZERO, t16 = BigInteger.ZERO, t17 = BigInteger.ZERO, t18 = BigInteger.ZERO, t19 = BigInteger.ZERO, t20 = BigInteger.ZERO, t21 = BigInteger.ZERO, t22 = BigInteger.ZERO, t23 = BigInteger.ZERO, t24 = BigInteger.ZERO, t25 = BigInteger.ZERO, t26 = BigInteger.ZERO, t27 = BigInteger.ZERO, t28 = BigInteger.ZERO, t29 = BigInteger.ZERO, t30 = BigInteger.ZERO;

        BigInteger b0 = b[0], b1 = b[1], b2 = b[2], b3 = b[3], b4 = b[4], b5 = b[5], b6 = b[6], b7 = b[7], b8 = b[8], b9 = b[9], b10 = b[10], b11 = b[11], b12 = b[12], b13 = b[13], b14 = b[14], b15 = b[15];

        BigInteger v = a[0];
        t0=t0.add(v.multiply(b0));
        t1=t1.add(v.multiply(b1));
        t2=t2.add(v.multiply(b2));
        t3=t3.add(v.multiply(b3));
        t4=t4.add(v.multiply(b4));
        t5=t5.add(v.multiply(b5));
        t6=t6.add(v.multiply(b6));
        t7=t7.add(v.multiply(b7));
        t8=t8.add(v.multiply(b8));
        t9=t9.add(v.multiply(b9));
        t10=t10.add(v.multiply(b10));
        t11=t11.add(v.multiply(b11));
        t12=t12.add(v.multiply(b12));
        t13=t13.add(v.multiply(b13));
        t14=t14.add(v.multiply(b14));
        t15=t15.add(v.multiply(b15));
        v = a[1];
        t1=t1.add(v.multiply(b0));
        t2=t2.add(v.multiply(b1));
        t3=t3.add(v.multiply(b2));
        t4=t4.add(v.multiply(b3));
        t5=t5.add(v.multiply(b4));
        t6=t6.add(v.multiply(b5));
        t7=t7.add(v.multiply(b6));
        t8=t8.add(v.multiply(b7));
        t9=t9.add(v.multiply(b8));
        t10=t10.add(v.multiply(b9));
        t11=t11.add(v.multiply(b10));
        t12=t12.add(v.multiply(b11));
        t13=t13.add(v.multiply(b12));
        t14=t14.add(v.multiply(b13));
        t15=t15.add(v.multiply(b14));
        t16=t16.add(v.multiply(b15));
        v = a[2];
        t2=t2.add(v.multiply(b0));
        t3=t3.add(v.multiply(b1));
        t4=t4.add(v.multiply(b2));
        t5=t5.add(v.multiply(b3));
        t6=t6.add(v.multiply(b4));
        t7=t7.add(v.multiply(b5));
        t8=t8.add(v.multiply(b6));
        t9=t9.add(v.multiply(b7));
        t10=t10.add(v.multiply(b8));
        t11=t11.add(v.multiply(b9));
        t12=t12.add(v.multiply(b10));
        t13=t13.add(v.multiply(b11));
        t14=t14.add(v.multiply(b12));
        t15=t15.add(v.multiply(b13));
        t16=t16.add(v.multiply(b14));
        t17=t17.add(v.multiply(b15));
        v = a[3];
        t3=t3.add(v.multiply(b0));
        t4=t4.add(v.multiply(b1));
        t5=t5.add(v.multiply(b2));
        t6=t6.add(v.multiply(b3));
        t7=t7.add(v.multiply(b4));
        t8=t8.add(v.multiply(b5));
        t9=t9.add(v.multiply(b6));
        t10=t10.add(v.multiply(b7));
        t11=t11.add(v.multiply(b8));
        t12=t12.add(v.multiply(b9));
        t13=t13.add(v.multiply(b10));
        t14=t14.add(v.multiply(b11));
        t15=t15.add(v.multiply(b12));
        t16=t16.add(v.multiply(b13));
        t17=t17.add(v.multiply(b14));
        t18=t18.add(v.multiply(b15));
        v = a[4];
        t4=t4.add(v.multiply(b0));
        t5=t5.add(v.multiply(b1));
        t6=t6.add(v.multiply(b2));
        t7=t7.add(v.multiply(b3));
        t8=t8.add(v.multiply(b4));
        t9=t9.add(v.multiply(b5));
        t10=t10.add(v.multiply(b6));
        t11=t11.add(v.multiply(b7));
        t12=t12.add(v.multiply(b8));
        t13=t13.add(v.multiply(b9));
        t14=t14.add(v.multiply(b10));
        t15=t15.add(v.multiply(b11));
        t16=t16.add(v.multiply(b12));
        t17=t17.add(v.multiply(b13));
        t18=t18.add(v.multiply(b14));
        t19=t19.add(v.multiply(b15));
        v = a[5];
        t5=t5.add(v.multiply(b0));
        t6=t6.add(v.multiply(b1));
        t7=t7.add(v.multiply(b2));
        t8=t8.add(v.multiply(b3));
        t9=t9.add(v.multiply(b4));
        t10=t10.add(v.multiply(b5));
        t11=t11.add(v.multiply(b6));
        t12=t12.add(v.multiply(b7));
        t13=t13.add(v.multiply(b8));
        t14=t14.add(v.multiply(b9));
        t15=t15.add(v.multiply(b10));
        t16=t16.add(v.multiply(b11));
        t17=t17.add(v.multiply(b12));
        t18=t18.add(v.multiply(b13));
        t19=t19.add(v.multiply(b14));
        t20=t20.add(v.multiply(b15));
        v = a[6];
        t6=t6.add(v.multiply(b0));
        t7=t7.add(v.multiply(b1));
        t8=t8.add(v.multiply(b2));
        t9=t9.add(v.multiply(b3));
        t10=t10.add(v.multiply(b4));
        t11=t11.add(v.multiply(b5));
        t12=t12.add(v.multiply(b6));
        t13=t13.add(v.multiply(b7));
        t14=t14.add(v.multiply(b8));
        t15=t15.add(v.multiply(b9));
        t16=t16.add(v.multiply(b10));
        t17=t17.add(v.multiply(b11));
        t18=t18.add(v.multiply(b12));
        t19=t19.add(v.multiply(b13));
        t20=t20.add(v.multiply(b14));
        t21=t21.add(v.multiply(b15));
        v = a[7];
        t7=t7.add(v.multiply(b0));
        t8=t8.add(v.multiply(b1));
        t9=t9.add(v.multiply(b2));
        t10=t10.add(v.multiply(b3));
        t11=t11.add(v.multiply(b4));
        t12=t12.add(v.multiply(b5));
        t13=t13.add(v.multiply(b6));
        t14=t14.add(v.multiply(b7));
        t15=t15.add(v.multiply(b8));
        t16=t16.add(v.multiply(b9));
        t17=t17.add(v.multiply(b10));
        t18=t18.add(v.multiply(b11));
        t19=t19.add(v.multiply(b12));
        t20=t20.add(v.multiply(b13));
        t21=t21.add(v.multiply(b14));
        t22=t22.add(v.multiply(b15));
        v = a[8];
        t8=t8.add(v.multiply(b0));
        t9=t9.add(v.multiply(b1));
        t10=t10.add(v.multiply(b2));
        t11=t11.add(v.multiply(b3));
        t12=t12.add(v.multiply(b4));
        t13=t13.add(v.multiply(b5));
        t14=t14.add(v.multiply(b6));
        t15=t15.add(v.multiply(b7));
        t16=t16.add(v.multiply(b8));
        t17=t17.add(v.multiply(b9));
        t18=t18.add(v.multiply(b10));
        t19=t19.add(v.multiply(b11));
        t20=t20.add(v.multiply(b12));
        t21=t21.add(v.multiply(b13));
        t22=t22.add(v.multiply(b14));
        t23=t23.add(v.multiply(b15));
        v = a[9];
        t9=t9.add(v.multiply(b0));
        t10=t10.add(v.multiply(b1));
        t11=t11.add(v.multiply(b2));
        t12=t12.add(v.multiply(b3));
        t13=t13.add(v.multiply(b4));
        t14=t14.add(v.multiply(b5));
        t15=t15.add(v.multiply(b6));
        t16=t16.add(v.multiply(b7));
        t17=t17.add(v.multiply(b8));
        t18=t18.add(v.multiply(b9));
        t19=t19.add(v.multiply(b10));
        t20=t20.add(v.multiply(b11));
        t21=t21.add(v.multiply(b12));
        t22=t22.add(v.multiply(b13));
        t23=t23.add(v.multiply(b14));
        t24=t24.add(v.multiply(b15));
        v = a[10];
        t10=t10.add(v.multiply(b0));
        t11=t11.add(v.multiply(b1));
        t12=t12.add(v.multiply(b2));
        t13=t13.add(v.multiply(b3));
        t14=t14.add(v.multiply(b4));
        t15=t15.add(v.multiply(b5));
        t16=t16.add(v.multiply(b6));
        t17=t17.add(v.multiply(b7));
        t18=t18.add(v.multiply(b8));
        t19=t19.add(v.multiply(b9));
        t20=t20.add(v.multiply(b10));
        t21=t21.add(v.multiply(b11));
        t22=t22.add(v.multiply(b12));
        t23=t23.add(v.multiply(b13));
        t24=t24.add(v.multiply(b14));
        t25=t25.add(v.multiply(b15));
        v = a[11];
        t11=t11.add(v.multiply(b0));
        t12=t12.add(v.multiply(b1));
        t13=t13.add(v.multiply(b2));
        t14=t14.add(v.multiply(b3));
        t15=t15.add(v.multiply(b4));
        t16=t16.add(v.multiply(b5));
        t17=t17.add(v.multiply(b6));
        t18=t18.add(v.multiply(b7));
        t19=t19.add(v.multiply(b8));
        t20=t20.add(v.multiply(b9));
        t21=t21.add(v.multiply(b10));
        t22=t22.add(v.multiply(b11));
        t23=t23.add(v.multiply(b12));
        t24=t24.add(v.multiply(b13));
        t25=t25.add(v.multiply(b14));
        t26=t26.add(v.multiply(b15));
        v = a[12];
        t12=t12.add(v.multiply(b0));
        t13=t13.add(v.multiply(b1));
        t14=t14.add(v.multiply(b2));
        t15=t15.add(v.multiply(b3));
        t16=t16.add(v.multiply(b4));
        t17=t17.add(v.multiply(b5));
        t18=t18.add(v.multiply(b6));
        t19=t19.add(v.multiply(b7));
        t20=t20.add(v.multiply(b8));
        t21=t21.add(v.multiply(b9));
        t22=t22.add(v.multiply(b10));
        t23=t23.add(v.multiply(b11));
        t24=t24.add(v.multiply(b12));
        t25=t25.add(v.multiply(b13));
        t26=t26.add(v.multiply(b14));
        t27=t27.add(v.multiply(b15));
        v = a[13];
        t13=t13.add(v.multiply(b0));
        t14=t14.add(v.multiply(b1));
        t15=t15.add(v.multiply(b2));
        t16=t16.add(v.multiply(b3));
        t17=t17.add(v.multiply(b4));
        t18=t18.add(v.multiply(b5));
        t19=t19.add(v.multiply(b6));
        t20=t20.add(v.multiply(b7));
        t21=t21.add(v.multiply(b8));
        t22=t22.add(v.multiply(b9));
        t23=t23.add(v.multiply(b10));
        t24=t24.add(v.multiply(b11));
        t25=t25.add(v.multiply(b12));
        t26=t26.add(v.multiply(b13));
        t27=t27.add(v.multiply(b14));
        t28=t28.add(v.multiply(b15));
        v = a[14];
        t14=t14.add(v.multiply(b0));
        t15=t15.add(v.multiply(b1));
        t16=t16.add(v.multiply(b2));
        t17=t17.add(v.multiply(b3));
        t18=t18.add(v.multiply(b4));
        t19=t19.add(v.multiply(b5));
        t20=t20.add(v.multiply(b6));
        t21=t21.add(v.multiply(b7));
        t22=t22.add(v.multiply(b8));
        t23=t23.add(v.multiply(b9));
        t24=t24.add(v.multiply(b10));
        t25=t25.add(v.multiply(b11));
        t26=t26.add(v.multiply(b12));
        t27=t27.add(v.multiply(b13));
        t28=t28.add(v.multiply(b14));
        t29=t29.add(v.multiply(b15));
        v = a[15];
        t15=t15.add(v.multiply(b0));
        t16=t16.add(v.multiply(b1));
        t17=t17.add(v.multiply(b2));
        t18=t18.add(v.multiply(b3));
        t19=t19.add(v.multiply(b4));
        t20=t20.add(v.multiply(b5));
        t21=t21.add(v.multiply(b6));
        t22=t22.add(v.multiply(b7));
        t23=t23.add(v.multiply(b8));
        t24=t24.add(v.multiply(b9));
        t25=t25.add(v.multiply(b10));
        t26=t26.add(v.multiply(b11));
        t27=t27.add(v.multiply(b12));
        t28=t28.add(v.multiply(b13));
        t29=t29.add(v.multiply(b14));
        t30=t30.add(v.multiply(b15));

        t0=t0.add(valueOf(38).multiply(t16));
        t1=t1.add(valueOf(38).multiply(t17));
        t2=t2.add(valueOf(38).multiply(t18));
        t3=t3.add(valueOf(38).multiply(t19));
        t4=t4.add(valueOf(38).multiply(t20));
        t5=t5.add(valueOf(38).multiply(t21));
        t6=t6.add(valueOf(38).multiply(t22));
        t7=t7.add(valueOf(38).multiply(t23));
        t8=t8.add(valueOf(38).multiply(t24));
        t9=t9.add(valueOf(38).multiply(t25));
        t10=t10.add(valueOf(38).multiply(t26));
        t11=t11.add(valueOf(38).multiply(t27));
        t12=t12.add(valueOf(38).multiply(t28));
        t13=t13.add(valueOf(38).multiply(t29));
        t14=t14.add(valueOf(38).multiply(t30));

        // first car
        var c = ONE;
        v=t0.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t0 = v.subtract(c.multiply(valueOf(65536)));
        v=t1.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t1 = v.subtract(c.multiply(valueOf(65536)));
        v=t2.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t2 = v.subtract(c.multiply(valueOf(65536)));
        v=t3.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t3 = v.subtract(c.multiply(valueOf(65536)));
        v=t4.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t4 = v.subtract(c.multiply(valueOf(65536)));
        v=t5.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t5 = v.subtract(c.multiply(valueOf(65536)));
        v=t6.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t6 = v.subtract(c.multiply(valueOf(65536)));
        v=t7.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t7 = v.subtract(c.multiply(valueOf(65536)));
        v=t8.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t8 = v.subtract(c.multiply(valueOf(65536)));
        v=t9.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t9 = v.subtract(c.multiply(valueOf(65536)));
        v=t10.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t10 = v.subtract(c.multiply(valueOf(65536)));
        v=t11.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t11 = v.subtract(c.multiply(valueOf(65536)));
        v=t12.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t12 = v.subtract(c.multiply(valueOf(65536)));
        v=t13.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t13 = v.subtract(c.multiply(valueOf(65536)));
        v=t14.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t14 = v.subtract(c.multiply(valueOf(65536)));
        v=t15.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t15 = v.subtract(c.multiply(valueOf(65536)));
        t0 = t0.add(c.subtract(ONE).add(valueOf(37).multiply(c.subtract(ONE))));

        // second car
        c = ONE;
        v=t0.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t0 = v.subtract(c.multiply(valueOf(65536)));
        v=t1.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t1 = v.subtract(c.multiply(valueOf(65536)));
        v=t2.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t2 = v.subtract(c.multiply(valueOf(65536)));
        v=t3.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t3 = v.subtract(c.multiply(valueOf(65536)));
        v=t4.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t4 = v.subtract(c.multiply(valueOf(65536)));
        v=t5.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t5 = v.subtract(c.multiply(valueOf(65536)));
        v=t6.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t6 = v.subtract(c.multiply(valueOf(65536)));
        v=t7.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t7 = v.subtract(c.multiply(valueOf(65536)));
        v=t8.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t8 = v.subtract(c.multiply(valueOf(65536)));
        v=t9.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t9 = v.subtract(c.multiply(valueOf(65536)));
        v=t10.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t10 = v.subtract(c.multiply(valueOf(65536)));
        v=t11.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t11 = v.subtract(c.multiply(valueOf(65536)));
        v=t12.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t12 = v.subtract(c.multiply(valueOf(65536)));
        v=t13.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t13 = v.subtract(c.multiply(valueOf(65536)));
        v=t14.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t14 = v.subtract(c.multiply(valueOf(65536)));
        v=t15.add(c).add(valueOf(65535));
        c = v.divide(valueOf(65536));
        t15 = v.subtract(c.multiply(valueOf(65536)));
        t0 = t0.add(c.subtract(ONE).add(valueOf(37).multiply(c.subtract(ONE))));

        o[0] = t0;
        o[1] = t1;
        o[2] = t2;
        o[3] = t3;
        o[4] = t4;
        o[5] = t5;
        o[6] = t6;
        o[7] = t7;
        o[8] = t8;
        o[9] = t9;
        o[10] = t10;
        o[11] = t11;
        o[12] = t12;
        o[13] = t13;
        o[14] = t14;
        o[15] = t15;
    }

    private static void scalarmult(BigInteger[][] p, BigInteger[][] q, byte[] s) {
        set25519(p[0], gf0);
        set25519(p[1], gf1);
        set25519(p[2], gf1);
        set25519(p[3], gf0);
        for (var i = 255; i >= 0; --i) {
            var b = (s[(i / 8)] >> (i & 7)) & 1;
            cswap(p, q, valueOf(b));
            add(q, p);
            add(p, p);
            cswap(p, q, valueOf(b));
        }
    }

    private static void cswap(BigInteger[][] p, BigInteger[][] q, BigInteger b) {
        for (var i = 0; i < 4; i++) {
            sel25519(p[i], q[i], b);
        }
    }

    private static void sel25519(BigInteger[] p, BigInteger[] q, BigInteger b) {
        var c = b.negate();
        for (var i = 0; i < 16; i++) {
            var t = c.and(p[i]).xor(q[i]);
            p[i] = p[i].xor(t);
            q[i] = p[i].xor(t);
        }
    }

    private static void add(BigInteger[][] p, BigInteger[][] q) {
        var a = createContainer(16);
        var b = createContainer(16);
        var c = createContainer(16);
        var d = createContainer(16);
        var e = createContainer(16);
        var f = createContainer(16);
        var g = createContainer(16);
        var h = createContainer(16);
        var t = createContainer(16);

        Z(a, p[1], p[0]);
        Z(t, q[1], q[0]);
        M(a, a, t);
        A(b, p[0], p[1]);
        A(t, q[0], q[1]);
        M(b, b, t);
        M(c, p[3], q[3]);
        M(c, c, D2);
        M(d, p[2], q[2]);
        A(d, d, d);
        Z(e, b, a);
        Z(f, d, c);
        A(g, d, c);
        A(h, b, a);

        M(p[0], e, f);
        M(p[1], h, g);
        M(p[2], g, f);
        M(p[3], e, h);
    }

    private static void A(BigInteger[] o, BigInteger[] a, BigInteger[] b) {
        for (var i = 0; i < 16; i++) {
            o[i] = (a[i].add(b[i]));
        }
    }

    private static void Z(BigInteger[] o, BigInteger[] a, BigInteger[] b) {
        for (var i = 0; i < 16; i++) {
            o[i] = (a[i].subtract(b[i]));
        }
    }

    private static void pack(byte[] r, BigInteger[][] p) {
        var tx = createContainer(16);
        var ty = createContainer(16);
        var zi = createContainer(16);
        inv25519(zi, p[2]);
        M(tx, p[0], zi);
        M(ty, p[1], zi);
        pack25519(r, ty);
        r[31] ^= par25519(tx) << 7;
    }

    private static void inv25519(BigInteger[] o, BigInteger[] i) {
        var c = createContainer(16);
        int a;
        for (a = 0; a < 16; a++) c[a] = i[a];
        for (a = 253; a >= 0; a--) {
            S(c, c);
            if (a != 2 && a != 4) M(c, c, i);
        }
        for (a = 0; a < 16; a++) o[a] = c[a];
    }

    private static void S(BigInteger[] o, BigInteger[] a) {
        M(o, a, a);
    }

    private static void pack25519(byte[] o, BigInteger[] n) {
        var m = createContainer(16);
        var t = createContainer(16);
        int i, j;
        for (i = 0; i < 16; i++) t[i] = n[i];
        car25519(t);
        car25519(t);
        car25519(t);
        for (j = 0; j < 2; j++) {
            m[0] = t[0].subtract(new BigInteger("ffed", 16));
            for (i = 1; i < 15; i++) {
                m[i] = t[i].subtract(new BigInteger("ffff", 16)).subtract(m[i - 1].shiftRight(16).and(valueOf(1)));
                m[i - 1] = m[i - 1].and(new BigInteger("ffff", 16));
            }
            m[15] = t[15].subtract(new BigInteger("7fff", 16)).subtract(m[14].shiftRight(16).and(valueOf(1)));
            var b = m[15].shiftRight(16).and(ONE);
            m[14] = m[14].and(new BigInteger("ffff", 16));
            sel25519(t, m, ONE.subtract(b));
        }
        for (i = 0; i < 16; i++) {
            o[2 * i] = (byte) t[i].and(new BigInteger("ff", 16)).intValue();
            o[2 * i + 1] = (byte) t[i].shiftRight(8).and(new BigInteger("ff", 16)).intValue();
        }
    }

    private static void car25519(BigInteger[] o) {
        var c = ONE;
        for (var i = 0; i < 16; i++) {
            var v = o[i].add(c).add(valueOf(65535));
            c = v.divide(valueOf(65536));
            o[i] = v.subtract(c.multiply(valueOf(65536)));
        }

        o[0] = o[0].add(c.subtract(ONE).add(valueOf(37).multiply(c.subtract(ONE))));
    }

    private static int par25519(BigInteger[] a) {
        var d = new byte[32];
        pack25519(d, a);
        return d[0] & 1;
    }

    private static BigInteger[][] createWrapper(){
        var a = new BigInteger[4][16];
        Arrays.stream(a).forEach(bigIntegers -> Arrays.fill(bigIntegers, ZERO));
        return a;
    }

    private static BigInteger[] createContainer(int length){
        var a = new BigInteger[length];
        Arrays.fill(a, ZERO);
        return a;
    }

    private static BigInteger lrsh(BigInteger num, int shiftBitCount) {
        var byteArray = num.toByteArray();
        var shiftMod = shiftBitCount % 8;
        var carryMask = (byte) (0xFF << (8 - shiftMod));
        var offsetBytes = (shiftBitCount / 8);

        int sourceIndex;
        for (var i = byteArray.length - 1; i >= 0; i--) {
            sourceIndex = i - offsetBytes;
            if (sourceIndex < 0) {
                byteArray[i] = 0;
            } else {
                var src = byteArray[sourceIndex];
                var dst = (byte) ((0xff & src) >>> shiftMod);
                if (sourceIndex - 1 >= 0) {
                    dst |= byteArray[sourceIndex - 1] << (8 - shiftMod) & carryMask;
                }
                byteArray[i] = dst;
            }
        }
        
        return new BigInteger(byteArray);
    }
}