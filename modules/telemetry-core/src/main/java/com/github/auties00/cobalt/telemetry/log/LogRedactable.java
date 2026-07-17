package com.github.auties00.cobalt.telemetry.log;

import java.nio.charset.StandardCharsets;

/**
 * A type that supplies its own redacted rendering for the privacy-aware logging pipeline.
 *
 * <p>{@link Log} renders any value that implements this interface through {@link #toRedactedLog()} instead of
 * its {@code toString()}, so a type whose natural string form would leak sensitive content can expose a
 * structural, non-reversible summary. This inverts the dependency: rather than the logger knowing every domain
 * type that needs masking, each type declares how to redact itself, keeping the telemetry facade free of any
 * dependency on the domain and transport modules.
 *
 * <p>The nested records cover the sensitive values that reach a log statement through a generic carrier (a
 * phone number as a {@code String} or {@code long}, an access token, a one-time code, an email, a JID held as
 * a {@code String}, a raw {@code byte[]} key, or an unclassified secret), which type dispatch cannot recognise
 * as sensitive on its own. Wrap such a value at the call site in the record that classifies it, for example
 * {@snippet :
 * LOGGER.log(Level.DEBUG, "sent to {0} with code {1}", new LogRedactable.User(jid), new LogRedactable.Code(code));
 * }
 * so it is masked at format time. Each record renders a keyed, non-reversible token, and its {@code toString()}
 * yields that token too, or the raw value when redaction is {@linkplain Log#enabled() disabled}, so a wrapped
 * value stays masked in plain string concatenation and third-party logging bridges as well as in Cobalt's own
 * formatter. These records are also the only supported way to fingerprint a value: the engine behind them is
 * package-private, so a caller redacts by choosing the record that describes what it holds.
 *
 * <p>A type that holds a sensitive value but has no redacted form of its own implements
 * {@link LogRedactableProvider} instead and returns the record that classifies what it carries.
 */
public interface LogRedactable extends LogRedactableProvider {
    /**
     * Returns a redacted, non-reversible rendering of this value, safe to emit to logs.
     *
     * @return the redacted rendering; never {@code null}
     */
    String toRedactedLog();

    /**
     * Returns this value, which is already its own redactable view.
     *
     * @return {@code this}; never {@code null}
     */
    @Override
    default LogRedactable toLogRedactable() {
        return this;
    }

    /**
     * A phone number carried as a {@code String} or {@code long}, rendered as {@code phone(#fingerprint)} with
     * the {@code E.164} and bare-digit forms fingerprinting identically.
     *
     * @param value the phone number, or {@code null}
     */
    record Phone(Object value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return value == null ? "null" : "phone(#" + LogRedactor.fingerprint(phoneBytes(value)) + ")";
        }

        /**
         * Returns the redacted token, or the raw value when redaction is disabled.
         *
         * @return the rendered string; never {@code null}
         */
        @Override
        public String toString() {
            return Log.enabled() ? toRedactedLog() : String.valueOf(value);
        }

        /**
         * Returns the UTF-8 bytes of a phone number, dropping a single leading {@code +} so the {@code E.164}
         * and bare-digit forms of the same number fingerprint identically.
         *
         * @param value the phone number value; must not be {@code null}
         * @return the normalised UTF-8 bytes
         */
        private static byte[] phoneBytes(Object value) {
            var text = String.valueOf(value);
            if (!text.isEmpty() && text.charAt(0) == '+') {
                text = text.substring(1);
            }
            return text.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * An email address, rendered as {@code email(#fingerprint)}.
     *
     * @param value the email address, or {@code null}
     */
    record Email(String value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return value == null ? "null" : "email(#" + LogRedactor.fingerprint(value.getBytes(StandardCharsets.UTF_8)) + ")";
        }

        /**
         * Returns the redacted token, or the raw value when redaction is disabled.
         *
         * @return the rendered string; never {@code null}
         */
        @Override
        public String toString() {
            return Log.enabled() ? toRedactedLog() : String.valueOf(value);
        }
    }

    /**
     * A one-time verification, registration, or pairing code, rendered as {@code code(#fingerprint)}.
     *
     * @param value the code, or {@code null}
     */
    record Code(String value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return value == null ? "null" : "code(#" + LogRedactor.fingerprint(value.getBytes(StandardCharsets.UTF_8)) + ")";
        }

        /**
         * Returns the redacted token, or the raw value when redaction is disabled.
         *
         * @return the rendered string; never {@code null}
         */
        @Override
        public String toString() {
            return Log.enabled() ? toRedactedLog() : String.valueOf(value);
        }
    }

    /**
     * An opaque credential (access token, session token, invite code), rendered as
     * {@code token(len=N,#fingerprint)} so its length survives while its content does not.
     *
     * @param value the credential, or {@code null}
     */
    record Token(String value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return value == null
                    ? "null"
                    : "token(len=" + value.length() + ",#" + LogRedactor.fingerprint(value.getBytes(StandardCharsets.UTF_8)) + ")";
        }

        /**
         * Returns the redacted token, or the raw value when redaction is disabled.
         *
         * @return the rendered string; never {@code null}
         */
        @Override
        public String toString() {
            return Log.enabled() ? toRedactedLog() : String.valueOf(value);
        }
    }

    /**
     * A user address carried as a JID {@code String}, rendered with its user part fingerprinted and its
     * routing structure (agent, device, server) preserved.
     *
     * @param value the JID string, or {@code null}
     */
    record User(String value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return value == null ? "null" : redact(value);
        }

        /**
         * Returns the redacted JID string, or the raw value when redaction is disabled.
         *
         * @return the rendered string; never {@code null}
         */
        @Override
        public String toString() {
            return Log.enabled() ? toRedactedLog() : String.valueOf(value);
        }

        /**
         * Redacts a JID carried as a {@code String}, hashing only the user component while preserving the
         * routing structure, without parsing it into a domain {@code Jid} (this module is a dependency-free
         * leaf).
         *
         * <p>The user is the run of characters up to the first agent ({@code _}) or device ({@code :})
         * separator, or the server ({@code @}) separator when neither appears; it is replaced by a keyed
         * fingerprint while the agent, device, and server parts are kept verbatim. A string with no {@code @}
         * is fingerprinted whole as a generic {@code jid(#...)} token, and one whose user part is empty carries
         * no user data and is returned verbatim.
         *
         * @param jid the JID string to redact; must not be {@code null}
         * @return the redacted JID string
         */
        private static String redact(String jid) {
            var at = jid.lastIndexOf('@');
            if (at < 0) {
                return "jid(#" + LogRedactor.fingerprint(jid.getBytes(StandardCharsets.UTF_8)) + ")";
            }
            var local = jid.substring(0, at);
            var server = jid.substring(at);
            var userEnd = local.length();
            for (var i = 0; i < local.length(); i++) {
                var c = local.charAt(i);
                if (c == '_' || c == ':') {
                    userEnd = i;
                    break;
                }
            }
            if (userEnd == 0) {
                return jid;
            }
            var user = local.substring(0, userEnd);
            var suffix = local.substring(userEnd);
            return "#" + LogRedactor.fingerprint(user.getBytes(StandardCharsets.UTF_8)) + suffix + server;
        }
    }

    /**
     * An address with no user component, such as a bare server or a device-only address, rendered verbatim.
     *
     * <p>Every part of such an address is protocol routing information rather than user data, so there is
     * nothing to mask. This record exists so that a type whose address may or may not carry a user, such as a
     * JID, can classify the user-less case explicitly: passing it to {@link User} instead would leave no
     * server separator to split on and fingerprint the whole address, hiding routing information that is
     * public and useful.
     *
     * @param value the server address; never {@code null}
     */
    record Server(String value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return value;
        }

        /**
         * Returns the address, which carries no user data to mask.
         *
         * @return the rendered string; never {@code null}
         */
        @Override
        public String toString() {
            return toRedactedLog();
        }
    }

    /**
     * Any other secret with no more specific classification, rendered as {@code secret(#fingerprint)}; a
     * {@code byte[]} value is fingerprinted over its bytes, any other value over its string form.
     *
     * @param value the secret value, or {@code null}
     */
    record Secret(Object value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return value == null ? "null" : "secret(#" + LogRedactor.fingerprint(secretBytes(value)) + ")";
        }

        /**
         * Returns the redacted token, or the raw value when redaction is disabled.
         *
         * @return the rendered string; never {@code null}
         */
        @Override
        public String toString() {
            return Log.enabled() ? toRedactedLog() : String.valueOf(value);
        }

        /**
         * Returns the bytes to fingerprint for a generic secret, using the array itself when the value is a
         * {@code byte[]} and its string form otherwise.
         *
         * @param value the secret value; must not be {@code null}
         * @return the bytes to fingerprint
         */
        private static byte[] secretBytes(Object value) {
            return value instanceof byte[] bytes ? bytes : String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * A raw byte array (typically key material), rendered as {@code bytes(len=N,#fingerprint)} so its length
     * survives but never its content. Wrap a {@code byte[]} at the call site in this record; it is the one
     * sensitive type that cannot implement this interface directly.
     *
     * @param value the byte array; never {@code null}
     */
    record Bytes(byte[] value) implements LogRedactable {
        /**
         * {@inheritDoc}
         */
        @Override
        public String toRedactedLog() {
            return "bytes(len=" + value.length + ",#" + LogRedactor.fingerprint(value) + ")";
        }
    }
}
