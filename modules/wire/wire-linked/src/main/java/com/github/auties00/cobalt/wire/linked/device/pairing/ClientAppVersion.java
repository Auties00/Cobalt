package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.OptionalInt;

import static java.lang.Integer.parseInt;

/**
 * Represents the version of the WhatsApp client application that is opening a connection.
 *
 * <p>Whenever a client negotiates a session with WhatsApp's servers it must declare which
 * build of the app it is, so that the server can enable or disable features, format
 * responses appropriately, and in some cases outright refuse obsolete clients. This type
 * models that version as a sequence of numeric components in the familiar dotted form
 * {@code primary.secondary.tertiary.quaternary.quinary}. Only {@code primary} is
 * conceptually required; trailing components are optional and are omitted from the string
 * form when not set.
 *
 * <p>A convenience {@link #toHash()} method computes the MD5 digest of the string form,
 * which matches the hash that WhatsApp's handshake layer expects as part of the client
 * payload {@code buildHash} field on the wire.
 *
 * <p>Instances are mutable so that callers building a {@link ClientPayload} can adjust
 * individual components at runtime, for example to masquerade as a specific client type
 * during the pairing handshake.
 */
@ProtobufMessage(name = "DeviceProps.AppVersion")
public final class ClientAppVersion {
    /**
     * Major version component, typically bumped for releases that change protocol
     * behaviour in a backward incompatible way. Serialised as wire index {@code 1}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer primary;

    /**
     * Minor version component, typically bumped for feature additions. Serialised as
     * wire index {@code 2}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer secondary;

    /**
     * Patch version component, typically bumped for bugfixes within a minor release.
     * Serialised as wire index {@code 3}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer tertiary;

    /**
     * Build number or hotfix component. Used by the Windows desktop client to carry its
     * store build number alongside the usual three part version. Serialised as wire
     * index {@code 4}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer quaternary;

    /**
     * Additional revision component reserved for rare builds that require a fifth number.
     * Almost never populated in practice. Serialised as wire index {@code 5}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer quinary;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * <p>All five components are nullable because the underlying protobuf schema marks
     * every field as optional; this constructor is package private so that client code is
     * funnelled through the generated {@code ClientAppVersionBuilder} or the convenience
     * constructors below.
     *
     * @param primary    the major component, or {@code null} if unknown
     * @param secondary  the minor component, or {@code null} if unknown
     * @param tertiary   the patch component, or {@code null} if unknown
     * @param quaternary the build number component, or {@code null} if unknown
     * @param quinary    the revision component, or {@code null} if unknown
     */
    ClientAppVersion(Integer primary, Integer secondary, Integer tertiary, Integer quaternary, Integer quinary) {
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
        this.quaternary = quaternary;
        this.quinary = quinary;
    }

    /**
     * Convenience constructor for a version that carries only the major component.
     *
     * <p>Useful when the caller wants to express, for example, {@code "2"} as a version
     * string or when minor and patch components are genuinely unknown.
     *
     * @param primary the major component
     */
    public ClientAppVersion(int primary) {
        this(primary, null, null, null, null);
    }

    /**
     * Convenience constructor for the common three component version
     * {@code primary.secondary.tertiary}. The build and revision fields are left unset.
     *
     * @param primary   the major component
     * @param secondary the minor component
     * @param tertiary  the patch component
     */
    public ClientAppVersion(int primary, int secondary, int tertiary) {
        this(primary, secondary, tertiary, null, null);
    }

    /**
     * Parses a dotted decimal version string into a {@link ClientAppVersion}.
     *
     * <p>Accepts between one and five numeric components separated by dots, for example
     * {@code "2"}, {@code "2.3000"}, {@code "2.3000.1017"} or
     * {@code "2.3000.1017.17.0"}. Missing trailing components are left unset.
     *
     * @param version the version string to parse
     * @return the parsed {@code ClientAppVersion}
     * @throws IllegalArgumentException if the string contains more than five components
     * @throws NumberFormatException    if any component is not a valid integer
     */
    public static ClientAppVersion of(String version) {
        var tokens = version.split("\\.", 5);
        if (tokens.length > 5) {
            throw new IllegalArgumentException("Invalid value of tokens for version %s: %s".formatted(version, tokens));
        }

        var primary = tokens.length > 0 ? parseInt(tokens[0]) : null;
        var secondary = tokens.length > 1 ? parseInt(tokens[1]) : null;
        var tertiary = tokens.length > 2 ? parseInt(tokens[2]) : null;
        var quaternary = tokens.length > 3 ? parseInt(tokens[3]) : null;
        var quinary = tokens.length > 4 ? parseInt(tokens[4]) : null;
        return new ClientAppVersion(primary, secondary, tertiary, quaternary, quinary);
    }

    /**
     * Computes the MD5 digest of this version's dotted string form.
     *
     * <p>WhatsApp's handshake requires the client to advertise a {@code buildHash} as an
     * MD5 hash of the version string, and this helper produces the exact bytes expected
     * on the wire. The result is a fresh sixteen byte array that the caller may modify
     * freely.
     *
     * @return the sixteen byte MD5 digest of {@link #toString()}
     * @throws InternalError if the current JRE does not provide an MD5 implementation,
     *                       which should never happen on a standards compliant platform
     */
    public byte[] toHash() {
        try {
            var digest = MessageDigest.getInstance("MD5");
            digest.update(toString().getBytes());
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new InternalError("Missing md5 implementation", exception);
        }
    }

    /**
     * Returns the dotted decimal form of this version.
     *
     * <p>Components that are {@code null} are omitted entirely and do not contribute a
     * separator. For example, a version with only {@code primary=2} and
     * {@code tertiary=5} set is rendered as {@code "2.5"} rather than {@code "2..5"}.
     *
     * @return the version string, possibly empty if no component is set
     */
    @Override
    public String toString() {
        var result = new StringBuilder();
        if(primary != null) {
            result.append(primary);
        }
        if(secondary != null) {
            if(!result.isEmpty()) {
                result.append('.');
            }
            result.append(secondary);
        }
        if(tertiary != null) {
            if(!result.isEmpty()) {
                result.append('.');
            }
            result.append(tertiary);
        }
        if(quaternary != null) {
            if(!result.isEmpty()) {
                result.append('.');
            }
            result.append(quaternary);
        }
        if(quinary != null) {
            if(!result.isEmpty()) {
                result.append('.');
            }
            result.append(quinary);
        }
        return result.toString();
    }

    /**
     * Returns the major version component.
     *
     * @return the major component, or {@link OptionalInt#empty()} when not set
     */
    public OptionalInt primary() {
        return primary == null ? OptionalInt.empty() : OptionalInt.of(primary);
    }

    /**
     * Returns the minor version component.
     *
     * @return the minor component, or {@link OptionalInt#empty()} when not set
     */
    public OptionalInt secondary() {
        return secondary == null ? OptionalInt.empty() : OptionalInt.of(secondary);
    }

    /**
     * Returns the patch version component.
     *
     * @return the patch component, or {@link OptionalInt#empty()} when not set
     */
    public OptionalInt tertiary() {
        return tertiary == null ? OptionalInt.empty() : OptionalInt.of(tertiary);
    }

    /**
     * Returns the build number component, populated for example on Windows desktop
     * builds that carry a store build number.
     *
     * @return the build number component, or {@link OptionalInt#empty()} when not set
     */
    public OptionalInt quaternary() {
        return quaternary == null ? OptionalInt.empty() : OptionalInt.of(quaternary);
    }

    /**
     * Returns the revision component. Almost always empty in practice.
     *
     * @return the revision component, or {@link OptionalInt#empty()} when not set
     */
    public OptionalInt quinary() {
        return quinary == null ? OptionalInt.empty() : OptionalInt.of(quinary);
    }

    /**
     * Replaces the major version component.
     *
     * @param primary the new major component, or {@code null} to clear it
     */
    public void setPrimary(Integer primary) {
        this.primary = primary;
    }

    /**
     * Replaces the minor version component.
     *
     * @param secondary the new minor component, or {@code null} to clear it
     */
    public void setSecondary(Integer secondary) {
        this.secondary = secondary;
    }

    /**
     * Replaces the patch version component.
     *
     * @param tertiary the new patch component, or {@code null} to clear it
     */
    public void setTertiary(Integer tertiary) {
        this.tertiary = tertiary;
    }

    /**
     * Replaces the build number component.
     *
     * @param quaternary the new build number component, or {@code null} to clear it
     */
    public void setQuaternary(Integer quaternary) {
        this.quaternary = quaternary;
    }

    /**
     * Replaces the revision component.
     *
     * @param quinary the new revision component, or {@code null} to clear it
     */
    public void setQuinary(Integer quinary) {
        this.quinary = quinary;
    }
}
