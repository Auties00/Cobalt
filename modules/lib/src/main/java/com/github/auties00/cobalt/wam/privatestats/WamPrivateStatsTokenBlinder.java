package com.github.auties00.cobalt.wam.privatestats;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.privatestats.ed25519.Ed25519HashToPoint;
import com.github.auties00.cobalt.wam.privatestats.ed25519.Ed25519Point;

import java.util.Objects;

/**
 * Performs the client-side blind and unblind steps of the WhatsApp private-stats token VOPRF on the Ed25519
 * curve.
 *
 * <p>The protocol is a single-use blinded-token verifiable oblivious pseudorandom function (VOPRF). The client
 * picks a random 32-byte scalar {@code k}, hashes the message {@code m} to a curve point {@code H(m)}, and ships
 * {@code blinded = H(m) + k*B} to the server, where {@code B} is the Ed25519 base point. The server replies with
 * {@code signed = sk * blinded = sk*H(m) + k*pk}, where {@code sk} is the server private key and
 * {@code pk = sk*B} is its public key. The client recovers {@code sk*H(m)} as {@code signed - k*pk}, the
 * unblinded VOPRF output.
 *
 * <p>The same scalar must be supplied to {@link #unblind} that was supplied to {@link #blind}, and the scalar
 * must be uniformly random and used at most once per message; reusing a scalar across messages leaks the
 * unblinded outputs of prior messages.
 */
@WhatsAppWebModule(moduleName = "WAWamPrivateStatsToken")
@WhatsAppWebModule(moduleName = "WAWebIssuePrivateStatsToken")
public final class WamPrivateStatsTokenBlinder {
    /**
     * The shared byte length of a token, a scalar, and a compressed Edwards point.
     *
     * @implNote
     * This implementation matches the {@code new Uint8Array(32)} sized buffers used everywhere in the
     * {@code WAWamPrivateStatsToken} and {@code WACryptoEd25519} modules.
     */
    public static final int TOKEN_BYTES = 32;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private WamPrivateStatsTokenBlinder() {
        throw new AssertionError("PrivateStatsTokenBlinder is a utility class and must not be instantiated");
    }

    /**
     * Computes the blinded curve point {@code blind(message, scalar) = H(message) + scalar*B} and returns its
     * 32-byte compressed Edwards encoding.
     *
     * <p>The result is the {@code blinded_credential} payload that {@link WamPrivateStatsTokenIssuer} sends
     * inside the {@code sign_credential} IQ. The caller's {@code scalar} array is cloned before clamping, so it
     * survives the call intact.
     *
     * @implNote
     * This implementation mirrors the {@code blindToken} export step for step: clone-and-clamp the scalar per
     * the X25519/Ed25519 convention (clear bits 0, 1, and 2 of byte 0, clear bit 7 of byte 31, set bit 6 of byte
     * 31), compute {@code scalar*B} via {@link Ed25519Point#scalarMultBase(long[][], byte[])}, hash the message
     * via {@link Ed25519HashToPoint#compute(byte[])}, add the two points in place via
     * {@link Ed25519Point#add(long[][], long[][])}, and emit the 32-byte compressed encoding via
     * {@link Ed25519Point#pack(byte[], long[][])}.
     *
     * @param message the message to blind, of any length
     * @param scalar  the client blinding scalar, exactly {@value #TOKEN_BYTES} bytes
     * @return a freshly allocated 32-byte compressed encoding of the blinded point
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException if {@code scalar} is not exactly {@value #TOKEN_BYTES} bytes
     * @see Ed25519HashToPoint#compute(byte[])
     * @see Ed25519Point#scalarMultBase(long[][], byte[])
     */
    @WhatsAppWebExport(
            moduleName = "WAWamPrivateStatsToken",
            exports = "blindToken",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    public static byte[] blind(byte[] message, byte[] scalar) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(scalar, "scalar must not be null");
        if (scalar.length != TOKEN_BYTES) {
            throw new IllegalArgumentException(
                    "scalar must be " + TOKEN_BYTES + " bytes, was " + scalar.length);
        }
        var clampedScalar = scalar.clone();
        clamp(clampedScalar);

        var scalarPoint = Ed25519Point.p3();
        Ed25519Point.scalarMultBase(scalarPoint, clampedScalar);

        var hashPoint = Ed25519HashToPoint.compute(message);
        Ed25519Point.add(hashPoint, scalarPoint);

        var out = new byte[TOKEN_BYTES];
        Ed25519Point.pack(out, hashPoint);
        return out;
    }

    /**
     * Recovers the unblinded VOPRF output from the server-signed blinded token by computing
     * {@code unblind(signed, scalar, serverPubKey) = signed - scalar*serverPubKey}.
     *
     * <p>Invoked by {@link WamPrivateStatsTokenIssuer} on the {@code signed_credential} bytes returned by the
     * server, with the same {@code scalar} that was passed to {@link #blind} and the {@code acs_public_key}
     * carried alongside the signed credential. The output is the {@code unblindedSignedToken} later fed into
     * {@code SHA-512(token || unblindedSignedToken)} to derive the upload shared secret.
     *
     * @implNote
     * This implementation diverges from the {@code unblindToken} export in its error model: the JS routine
     * returns {@code null} when {@code signed} or {@code serverPubKey} fails to decode, while Cobalt throws
     * {@link IllegalArgumentException} so the issuer sees the failure synchronously. The math is otherwise
     * identical: decode {@code signed} via {@link Ed25519Point#unpack(long[][], byte[])}, decode the negation of
     * the server key via {@link Ed25519Point#unpackNeg(long[][], byte[])}, scalar-multiply the negated key by the
     * clamped scalar via {@link Ed25519Point#scalarMult(long[][], long[][], byte[])}, add the product to the
     * signed point in place via {@link Ed25519Point#add(long[][], long[][])}, and emit the 32-byte compressed
     * encoding via {@link Ed25519Point#pack(byte[], long[][])}.
     *
     * @param signed       the server-signed blinded token, exactly {@value #TOKEN_BYTES} bytes
     * @param scalar       the same client scalar passed to {@link #blind}, exactly {@value #TOKEN_BYTES} bytes
     * @param serverPubKey the server public key as a compressed Ed25519 point, exactly {@value #TOKEN_BYTES}
     *                     bytes
     * @return a freshly allocated 32-byte compressed encoding of the unblinded point
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if any argument is not exactly {@value #TOKEN_BYTES} bytes, or if
     *                                  {@code signed} or {@code serverPubKey} is not a valid Edwards point
     * @see Ed25519Point#unpack(long[][], byte[])
     * @see Ed25519Point#unpackNeg(long[][], byte[])
     */
    @WhatsAppWebExport(
            moduleName = "WAWamPrivateStatsToken",
            exports = "unblindToken",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static byte[] unblind(byte[] signed, byte[] scalar, byte[] serverPubKey) {
        Objects.requireNonNull(signed, "signed must not be null");
        Objects.requireNonNull(scalar, "scalar must not be null");
        Objects.requireNonNull(serverPubKey, "serverPubKey must not be null");
        if (signed.length != TOKEN_BYTES) {
            throw new IllegalArgumentException(
                    "signed must be " + TOKEN_BYTES + " bytes, was " + signed.length);
        }
        if (scalar.length != TOKEN_BYTES) {
            throw new IllegalArgumentException(
                    "scalar must be " + TOKEN_BYTES + " bytes, was " + scalar.length);
        }
        if (serverPubKey.length != TOKEN_BYTES) {
            throw new IllegalArgumentException(
                    "serverPubKey must be " + TOKEN_BYTES + " bytes, was " + serverPubKey.length);
        }

        var blindedPoint = Ed25519Point.p3();
        if (Ed25519Point.unpack(blindedPoint, signed) != 0) {
            throw new IllegalArgumentException("signed is not a valid Edwards point");
        }
        var negatedPubKey = Ed25519Point.p3();
        if (Ed25519Point.unpackNeg(negatedPubKey, serverPubKey) != 0) {
            throw new IllegalArgumentException("serverPubKey is not a valid Edwards point");
        }

        var clampedScalar = scalar.clone();
        clamp(clampedScalar);

        var negProduct = Ed25519Point.p3();
        Ed25519Point.scalarMult(negProduct, negatedPubKey, clampedScalar);
        Ed25519Point.add(blindedPoint, negProduct);

        var out = new byte[TOKEN_BYTES];
        Ed25519Point.pack(out, blindedPoint);
        return out;
    }

    /**
     * Applies the standard X25519/Ed25519 scalar clamp in place.
     *
     * <p>Required before any scalar multiplication on Curve25519/Ed25519. The clamp bounds the scalar to the
     * safe range {@code [2^254, 2^255)} with low-order bits zeroed so the Montgomery ladder cannot leak
     * information about the subgroup component of an attacker-supplied point.
     *
     * @implNote
     * This implementation matches the private clamp helper inside the {@code WAWamPrivateStatsToken} module:
     * clear bits 0, 1, and 2 of byte 0 (mask {@code 0xF8}), clear bit 7 of byte 31 (mask {@code 0x7F}), and set
     * bit 6 of byte 31 (mask {@code 0x40}).
     *
     * @param scalar the scalar bytes to clamp in place, exactly {@value #TOKEN_BYTES} bytes
     */
    private static void clamp(byte[] scalar) {
        scalar[0] &= (byte) 0xF8;
        scalar[31] &= (byte) 0x7F;
        scalar[31] |= (byte) 0x40;
    }
}
