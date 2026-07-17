package com.github.auties00.cobalt.util;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Compares {@link AesGcmStreamCipher} against the JDK's own {@code AES/GCM/NoPadding} provider and the
 * BouncyCastle provider (the streaming implementation the datagram streams previously used) for AES-256-GCM
 * across datagram-representative payload sizes. Each provider is driven two ways so the comparison separates
 * call pattern from internal behaviour: a "whole" one-shot {@code doFinal} and a "streaming" sequence of chunked
 * {@code update} calls followed by {@code doFinal}, the same chunking the streaming cipher uses. All variants
 * re-key per operation (mirroring the per-datagram nonce of the socket transport) and write into preallocated
 * output buffers, so the throughput numbers compare raw compute and the allocation profiler isolates the memory
 * difference.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class AesGcmStreamCipherBenchmark {
    /**
     * The per-operation payload size in bytes, swept from a single datagram header up to a megabyte.
     */
    @Param({"64", "1024", "8192", "65536", "1048576"})
    private int size;

    /**
     * The fixed scratch-buffer chunk the streaming ciphers feed the payload through, matching the datagram
     * stream's 8 KiB working buffer.
     */
    private static final int CHUNK = 8192;

    private final byte[] key = new byte[32];
    private final byte[] nonce = new byte[12];
    private final byte[] encryptNonce = new byte[12];
    private long encryptCounter;
    private byte[] plaintext;
    private byte[] sealed;
    private byte[] scratch;
    private byte[] wholeOut;
    private SecretKeySpec secret;
    private AesGcmStreamCipher streaming;
    private Cipher jdk;
    private Cipher bc;

    /**
     * Returns a fresh nonce for the next encryption operation. The JDK provider forbids reusing an IV for GCM
     * encryption, and reusing one is unsafe regardless, so each encrypt op advances a counter into the nonce;
     * the decrypt benchmarks keep the fixed {@link #nonce} the {@link #sealed} ciphertext was produced under.
     */
    private byte[] nextEncryptNonce() {
        DataUtils.putLong(encryptNonce, 4, encryptCounter++, ByteOrder.BIG_ENDIAN);
        return encryptNonce;
    }

    @Setup(Level.Trial)
    public void setup() throws Exception {
        var random = new SecureRandom();
        random.nextBytes(key);
        random.nextBytes(nonce);
        secret = new SecretKeySpec(key, "AES");
        plaintext = new byte[size];
        random.nextBytes(plaintext);
        scratch = new byte[CHUNK + AesGcmStreamCipher.TAG_LENGTH];
        wholeOut = new byte[size + 64];
        streaming = new AesGcmStreamCipher();
        jdk = Cipher.getInstance("AES/GCM/NoPadding");
        bc = Cipher.getInstance("AES/GCM/NoPadding", new BouncyCastleProvider());
        jdk.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(128, nonce));
        sealed = jdk.doFinal(plaintext);
    }

    @Benchmark
    public void streamingEncrypt(Blackhole blackhole) throws Exception {
        streaming.init(true, secret, nextEncryptNonce());
        for (var offset = 0; offset < size; offset += CHUNK) {
            var length = Math.min(CHUNK, size - offset);
            blackhole.consume(streaming.update(plaintext, offset, length, scratch, 0));
            blackhole.consume(scratch);
        }
        blackhole.consume(streaming.doFinal(scratch, 0));
    }

    @Benchmark
    public void jdkWholeEncrypt(Blackhole blackhole) throws Exception {
        jdk.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(128, nextEncryptNonce()));
        blackhole.consume(jdk.doFinal(plaintext, 0, size, wholeOut, 0));
        blackhole.consume(wholeOut);
    }

    @Benchmark
    public void jdkStreamingEncrypt(Blackhole blackhole) throws Exception {
        jdk.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(128, nextEncryptNonce()));
        var outOffset = 0;
        for (var offset = 0; offset < size; offset += CHUNK) {
            var length = Math.min(CHUNK, size - offset);
            outOffset += jdk.update(plaintext, offset, length, wholeOut, outOffset);
        }
        blackhole.consume(jdk.doFinal(wholeOut, outOffset));
        blackhole.consume(wholeOut);
    }

    @Benchmark
    public void streamingDecrypt(Blackhole blackhole) throws Exception {
        streaming.init(false, secret, nonce);
        for (var offset = 0; offset < sealed.length; offset += CHUNK) {
            var length = Math.min(CHUNK, sealed.length - offset);
            blackhole.consume(streaming.update(sealed, offset, length, scratch, 0));
            blackhole.consume(scratch);
        }
        blackhole.consume(streaming.doFinal(scratch, 0));
    }

    @Benchmark
    public void jdkWholeDecrypt(Blackhole blackhole) throws Exception {
        jdk.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(128, nonce));
        blackhole.consume(jdk.doFinal(sealed, 0, sealed.length, wholeOut, 0));
        blackhole.consume(wholeOut);
    }

    @Benchmark
    public void jdkStreamingDecrypt(Blackhole blackhole) throws Exception {
        jdk.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(128, nonce));
        var outOffset = 0;
        for (var offset = 0; offset < sealed.length; offset += CHUNK) {
            var length = Math.min(CHUNK, sealed.length - offset);
            outOffset += jdk.update(sealed, offset, length, wholeOut, outOffset);
        }
        blackhole.consume(jdk.doFinal(wholeOut, outOffset));
        blackhole.consume(wholeOut);
    }

    @Benchmark
    public void bcWholeEncrypt(Blackhole blackhole) throws Exception {
        bc.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(128, nextEncryptNonce()));
        blackhole.consume(bc.doFinal(plaintext, 0, size, wholeOut, 0));
        blackhole.consume(wholeOut);
    }

    @Benchmark
    public void bcStreamingEncrypt(Blackhole blackhole) throws Exception {
        bc.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(128, nextEncryptNonce()));
        var outOffset = 0;
        for (var offset = 0; offset < size; offset += CHUNK) {
            var length = Math.min(CHUNK, size - offset);
            outOffset += bc.update(plaintext, offset, length, wholeOut, outOffset);
        }
        blackhole.consume(bc.doFinal(wholeOut, outOffset));
        blackhole.consume(wholeOut);
    }

    @Benchmark
    public void bcWholeDecrypt(Blackhole blackhole) throws Exception {
        bc.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(128, nonce));
        blackhole.consume(bc.doFinal(sealed, 0, sealed.length, wholeOut, 0));
        blackhole.consume(wholeOut);
    }

    @Benchmark
    public void bcStreamingDecrypt(Blackhole blackhole) throws Exception {
        bc.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(128, nonce));
        var outOffset = 0;
        for (var offset = 0; offset < sealed.length; offset += CHUNK) {
            var length = Math.min(CHUNK, sealed.length - offset);
            outOffset += bc.update(sealed, offset, length, wholeOut, outOffset);
        }
        blackhole.consume(bc.doFinal(wholeOut, outOffset));
        blackhole.consume(wholeOut);
    }

    public static void main(String[] args) throws RunnerException {
        var options = new OptionsBuilder()
                .include(AesGcmStreamCipherBenchmark.class.getSimpleName())
                .addProfiler("gc")
                .build();
        new Runner(options).run();
    }
}
