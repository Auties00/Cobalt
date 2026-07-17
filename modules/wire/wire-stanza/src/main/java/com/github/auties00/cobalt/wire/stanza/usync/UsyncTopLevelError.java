package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;

/**
 * Error envelope returned when the USync IQ failed wholesale.
 *
 * <p>Surfaced through {@link UsyncResult#topLevelError()} when the relay's IQ
 * response carries a {@code type} attribute other than {@code "result"}.
 * Per-protocol errors that apply to every user are exposed separately via
 * {@link UsyncResult#getProtocolError(UsyncProtocol)}.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public final class UsyncTopLevelError {
    /**
     * The numeric {@code code} attribute on the IQ's {@code <error>} child.
     */
    private final int errorCode;

    /**
     * The {@code text} attribute on the IQ's {@code <error>} child, coerced to
     * the empty string when absent.
     */
    private final String errorText;

    /**
     * The {@code type} attribute on the IQ's {@code <error>} child, coerced to
     * the empty string when absent.
     */
    private final String errorType;

    /**
     * Builds a new envelope from the parsed attributes.
     *
     * <p>Constructed exclusively by {@link UsyncQuery#parseResponse(Stanza)}.
     *
     * @param errorCode the numeric error code
     * @param errorText the human-readable text, coerced to the empty string
     *                  when {@code null}
     * @param errorType the error category, coerced to the empty string when
     *                  {@code null}
     */
    public UsyncTopLevelError(int errorCode, String errorText, String errorType) {
        this.errorCode = errorCode;
        this.errorText = Objects.requireNonNullElse(errorText, "");
        this.errorType = Objects.requireNonNullElse(errorType, "");
    }

    /**
     * Returns the numeric error code.
     *
     * @return the {@code code} attribute value
     */
    public int errorCode() {
        return errorCode;
    }

    /**
     * Returns the human-readable error text.
     *
     * @return the {@code text} attribute value, never {@code null}
     */
    public String errorText() {
        return errorText;
    }

    /**
     * Returns the error category.
     *
     * @return the {@code type} attribute value, never {@code null}
     */
    public String errorType() {
        return errorType;
    }
}
