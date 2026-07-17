package com.github.auties00.cobalt.telemetry.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import static java.util.logging.Logger.getLogger;

/**
 * Internal logging support for Cobalt.
 *
 * <p>Every Cobalt class logs through a {@link System.Logger} obtained from {@link #get(Class)}, so an
 * application that installs a {@link System.LoggerFinder} (an SLF4J, Log4j, or {@code java.util.logging}
 * bridge) receives Cobalt's records under the {@value #NAMESPACE} namespace and routes them through its own
 * configuration, the idiomatic behaviour for a library. On the plain JDK the records are printed to the
 * console by the compact {@link CobaltFormatter}.
 *
 * <p>The verbosity is fixed once, at class initialisation, from the {@value #LEVEL_PROPERTY} system property
 * or the {@value #LEVEL_ENV_VARIABLE} environment variable, defaulting to {@link Level#OFF}. It is
 * deliberately not reconfigurable at runtime: this package is not exported, so no consumer could reach a
 * setter, and freezing the level lets the emission guards below fold to compile-time constants.
 *
 * <p><strong>Zero-cost guards.</strong> The resolved level is published as the {@code static final} booleans
 * {@link #TRACE}, {@link #DEBUG}, {@link #INFO}, {@link #WARNING}, and {@link #ERROR}. Because they are
 * {@code static final} primitives assigned at class-init, HotSpot constant-folds them, so a guarded statement
 * {@snippet :
 * if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent to {0}", jid);
 * }
 * costs nothing when {@code DEBUG} is disabled: the JIT eliminates the branch, the argument evaluation, the
 * varargs array, and any redaction wrapper as dead code, and carries no residual branch when it is enabled.
 * Every log statement should be guarded this way. The guard is the emission gate, so a disabled guard drops
 * the record before it can reach a {@link System.LoggerFinder} bridge: a consumer's framework can filter
 * Cobalt's records further or redirect them, but cannot raise verbosity above {@value #LEVEL_PROPERTY}. Set
 * the property to {@link Level#TRACE} or {@link Level#ALL} to let a bridge observe everything and filter down.
 *
 * <p><strong>Redaction.</strong> Log lines are privacy-redacted by default: {@link #redact(Object)} runs every
 * message parameter that is a {@link LogRedactableProvider} through the {@link LogRedactable} it supplies, so a
 * {@code Jid}, a stanza, or a value wrapped at the call site never reaches the output in the clear, whether it
 * is passed on its own or inside a collection or array, whose elements are each redacted in turn. Sensitive
 * data that reaches a log statement through a generic carrier (a phone number as a {@code String} or
 * {@code long}, an access token, a one-time code, a raw {@code byte[]} key) is invisible to type dispatch and
 * must be wrapped at the call site in the {@link LogRedactable} record that classifies it, such as
 * {@link LogRedactable.Phone}, {@link LogRedactable.Token}, {@link LogRedactable.Code},
 * {@link LogRedactable.Email}, {@link LogRedactable.User}, {@link LogRedactable.Secret}, or
 * {@link LogRedactable.Bytes}. Redaction is on by default and disabled by setting {@link #REDACT_PROPERTY} (or
 * {@link #REDACT_ENV_VARIABLE}) to {@code false} for trusted local debugging.
 */
public final class Log {
    /**
     * The logger namespace every Cobalt logger sits under.
     *
     * <p>It matches the root package of the library, so a {@link System.LoggerFinder} bridge can select the
     * whole tree by this prefix.
     */
    public static final String NAMESPACE = "com.github.auties00.cobalt";

    /**
     * The system property that sets Cobalt's log {@link Level} at class initialisation, checked before
     * {@link #LEVEL_ENV_VARIABLE}.
     *
     * <p>Its value must name a {@link Level} constant, case-insensitively; an unset, blank, or unrecognised
     * value leaves the bootstrap to fall through to the environment variable and ultimately to
     * {@link Level#OFF}.
     */
    public static final String LEVEL_PROPERTY = "cobalt.log.level";

    /**
     * The environment variable that sets Cobalt's log {@link Level} when {@link #LEVEL_PROPERTY} is unset.
     *
     * <p>Its value must name a {@link Level} constant, case-insensitively; an unset, blank, or unrecognised
     * value leaves logging {@linkplain Level#OFF off}.
     */
    public static final String LEVEL_ENV_VARIABLE = "COBALT_LOG_LEVEL";

    /**
     * The system property that disables redaction when set to {@code false}, checked before
     * {@link #REDACT_ENV_VARIABLE}.
     */
    public static final String REDACT_PROPERTY = "cobalt.log.redact";

    /**
     * The environment variable that disables redaction when set to {@code false} and {@link #REDACT_PROPERTY}
     * is unset.
     */
    public static final String REDACT_ENV_VARIABLE = "COBALT_LOG_REDACT";

    /**
     * The system property that pins the fingerprint salt to a fixed value, enabling correlation of tokens
     * across separate runs; when unset, a fresh per-process random salt is used.
     */
    public static final String SALT_PROPERTY = "cobalt.log.redact.salt";

    /**
     * Whether redaction is currently active; initialised from {@link #REDACT_PROPERTY} and adjustable at
     * runtime through {@link #setEnabled(boolean)}.
     */
    private static volatile boolean enabled = computeEnabled();

    /**
     * The log level resolved once, at class initialisation, from {@link #LEVEL_PROPERTY} or
     * {@link #LEVEL_ENV_VARIABLE}; {@link Level#OFF} when neither is set. Fixed for the lifetime of the process.
     */
    private static final Level LEVEL = resolveLevel();

    /**
     * Whether {@link Level#TRACE} records are emitted. A {@code static final} guard HotSpot constant-folds, so
     * {@code if (Log.TRACE) ...} is eliminated as dead code when tracing is disabled.
     */
    public static final boolean TRACE = isEnabled(Level.TRACE);

    /**
     * Whether {@link Level#DEBUG} records are emitted. A {@code static final} guard HotSpot constant-folds, so
     * {@code if (Log.DEBUG) ...} is eliminated as dead code when debug logging is disabled.
     */
    public static final boolean DEBUG = isEnabled(Level.DEBUG);

    /**
     * Whether {@link Level#INFO} records are emitted. A {@code static final} guard HotSpot constant-folds, so
     * {@code if (Log.INFO) ...} is eliminated as dead code when info logging is disabled.
     */
    public static final boolean INFO = isEnabled(Level.INFO);

    /**
     * Whether {@link Level#WARNING} records are emitted. A {@code static final} guard HotSpot constant-folds,
     * so {@code if (Log.WARNING) ...} is eliminated as dead code when warning logging is disabled.
     */
    public static final boolean WARNING = isEnabled(Level.WARNING);

    /**
     * Whether {@link Level#ERROR} records are emitted. A {@code static final} guard HotSpot constant-folds, so
     * {@code if (Log.ERROR) ...} is eliminated as dead code when error logging is disabled.
     */
    public static final boolean ERROR = isEnabled(Level.ERROR);

    static {
        var backend = getLogger(NAMESPACE);
        if (LEVEL == Level.OFF) {
            // Off by default: on the JDK's own java.util.logging backend the root console handler sits at
            // INFO, so without this Cobalt's INFO and above would print unbidden. Only quiet the tree when the
            // application has not already given it an explicit level (and never when it routes System.Logger
            // through its own framework, where this java.util.logging logger is simply unused).
            if (backend.getLevel() == null) {
                backend.setLevel(java.util.logging.Level.OFF);
            }
        } else {
            backend.setLevel(toJul(LEVEL));
            var handler = new ConsoleHandler();
            handler.setLevel(java.util.logging.Level.ALL);
            handler.setFormatter(new CobaltFormatter());
            backend.addHandler(handler);
            backend.setUseParentHandlers(false);
        }
    }

    /**
     * Prevents instantiation of this static-only holder.
     *
     * @throws AssertionError always, since the class is never meant to be instantiated
     */
    private Log() {
        throw new AssertionError("No instances");
    }

    /**
     * Returns the {@link System.Logger} for {@code owner}. Referencing this method also bootstraps Cobalt's
     * logging from the environment, so every class should acquire its logger here rather than calling
     * {@link System#getLogger(String)} directly.
     *
     * @param owner the class the logger is named after; must not be {@code null}
     * @return the logger for {@code owner}; never {@code null}
     */
    public static Logger get(Class<?> owner) {
        return System.getLogger(owner.getName());
    }

    /**
     * Returns whether redaction is currently active.
     *
     * @return {@code true} when parameters are masked, {@code false} when they pass through raw
     */
    public static boolean enabled() {
        return enabled;
    }

    /**
     * Sets whether redaction is active, overriding the {@link #REDACT_PROPERTY} default.
     *
     * @param value {@code true} to mask parameters, {@code false} to pass them through raw
     */
    static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Returns a redacted surrogate for {@code value} suitable for message formatting, or {@code value} itself
     * when it needs no masking or when redaction is disabled.
     *
     * <p>Any {@link LogRedactableProvider} value renders through the {@link LogRedactable} it supplies. A
     * {@link Collection} or object array is redacted element by element and rendered as a list, so a parameter
     * that holds many addresses masks each of them rather than leaking every one through the collection's own
     * {@code toString()}. Every other value passes through unchanged, which is safe because Cobalt's other
     * loggable types render as an identity hash rather than exposing their fields; a {@code byte[]} is one of
     * them, so key material stays an identity hash unless it is wrapped. Sensitive data carried in a generic
     * type (including a raw {@code byte[]} key) must be wrapped at the call site in the {@link LogRedactable}
     * record that classifies it, for example {@link LogRedactable.Bytes}, since no dispatch can recognise a
     * bare {@code String} or {@code long} as sensitive.
     *
     * @param value the log parameter to redact, or {@code null}
     * @return the masked surrogate, or the original value when no masking applies
     */
    static Object redact(Object value) {
        if (!enabled || value == null) {
            return value;
        }
        return switch (value) {
            case LogRedactableProvider provider -> provider.toLogRedactable().toRedactedLog();
            case Collection<?> collection -> collection.stream().map(Log::redact).toList();
            case Object[] array -> Arrays.stream(array).map(Log::redact).toList();
            default -> value;
        };
    }

    /**
     * Reads the initial redaction state from {@link #REDACT_PROPERTY}, falling back to
     * {@link #REDACT_ENV_VARIABLE}.
     *
     * @return {@code false} only when one of the two sources is set to {@code false} (case-insensitively),
     * {@code true} otherwise
     */
    private static boolean computeEnabled() {
        var raw = System.getProperty(REDACT_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(REDACT_ENV_VARIABLE);
        }
        return raw == null || !raw.trim().equalsIgnoreCase("false");
    }

    /**
     * Returns the fixed log level resolved at class initialisation, or {@link Level#OFF} when logging is
     * disabled.
     *
     * @return the process-wide log level; never {@code null}
     */
    public static Level level() {
        return LEVEL;
    }

    /**
     * Resolves the boot-time level from the {@value #LEVEL_PROPERTY} system property, falling back to the
     * {@value #LEVEL_ENV_VARIABLE} environment variable and finally to {@link Level#OFF}.
     *
     * @return the resolved {@link Level}, never {@code null}
     */
    private static Level resolveLevel() {
        var raw = System.getProperty(LEVEL_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(LEVEL_ENV_VARIABLE);
        }
        if (raw == null || raw.isBlank()) {
            return Level.OFF;
        }
        try {
            return Level.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Level.OFF;
        }
    }

    /**
     * Returns whether records at {@code level} are emitted under the resolved {@link #LEVEL}, that is, whether
     * logging is not {@linkplain Level#OFF off} and {@code level} is at least as severe as {@link #LEVEL}.
     *
     * @param level the level to test; must not be {@code null}
     * @return {@code true} when a record at {@code level} would be emitted
     */
    private static boolean isEnabled(Level level) {
        return LEVEL != Level.OFF && level.getSeverity() >= LEVEL.getSeverity();
    }

    /**
     * Maps a {@link System.Logger.Level} to the {@code java.util.logging} level the default backend understands.
     *
     * @param level the {@link System.Logger} level to translate; must not be {@code null}
     * @return the equivalent {@link java.util.logging.Level}
     */
    private static java.util.logging.Level toJul(Level level) {
        return switch (level) {
            case ALL -> java.util.logging.Level.ALL;
            case TRACE -> java.util.logging.Level.FINER;
            case DEBUG -> java.util.logging.Level.FINE;
            case INFO -> java.util.logging.Level.INFO;
            case WARNING -> java.util.logging.Level.WARNING;
            case ERROR -> java.util.logging.Level.SEVERE;
            case OFF -> java.util.logging.Level.OFF;
        };
    }

    /**
     * A compact single-line formatter: {@code HH:mm:ss.SSS LEVEL [subsystem.Class] message}, with any thrown
     * exception's stack trace appended.
     */
    static final class CobaltFormatter extends Formatter {
        /**
         * Renders {@code record} as one line carrying the time, level, shortened logger name, and message,
         * followed by the thrown exception's stack trace when one is present.
         *
         * @param record the record to format; must not be {@code null}
         * @return the formatted, newline-terminated string
         */
        @Override
        public String format(LogRecord record) {
            var builder = new StringBuilder(String.format("%1$tH:%1$tM:%1$tS.%1$tL %2$-5s [%3$s] %4$s%n",
                    record.getMillis(),
                    displayLevel(record.getLevel()),
                    shorten(record.getLoggerName()),
                    formatMessage(record)));
            var thrown = record.getThrown();
            if (thrown != null) {
                var writer = new StringWriter();
                thrown.printStackTrace(new PrintWriter(writer));
                builder.append(writer);
            }
            return builder.toString();
        }

        /**
         * Substitutes the record's parameters into its message after passing each through {@link #redact(Object)},
         * so sensitive parameters are masked in the rendered line.
         *
         * <p>Each parameter is replaced by its redacted surrogate; when none needs masking, or when redaction
         * is disabled, the record is formatted exactly as the superclass would. The masked parameters are then
         * substituted through {@link MessageFormat} using the same {@code {0}}-through-{@code {3}} placeholder
         * detection the superclass applies, so the output shape is unchanged.
         *
         * @param record the record whose message and parameters are formatted; must not be {@code null}
         * @return the formatted message with sensitive parameters redacted
         * @implNote This implementation redacts at format time rather than requiring redacted call sites, so
         * ordinary {@code LOGGER.log(level, "... {0}", value)} calls are masked automatically. Resource-bundle
         * localisation is not applied, which Cobalt's internal loggers never use.
         */
        @Override
        public synchronized String formatMessage(LogRecord record) {
            var parameters = record.getParameters();
            if (parameters == null || parameters.length == 0 || !enabled()) {
                return super.formatMessage(record);
            }
            var redacted = new Object[parameters.length];
            var changed = false;
            for (var i = 0; i < parameters.length; i++) {
                redacted[i] = redact(parameters[i]);
                changed |= redacted[i] != parameters[i];
            }
            if (!changed) {
                return super.formatMessage(record);
            }
            var format = record.getMessage();
            if (!format.contains("{0") && !format.contains("{1") && !format.contains("{2") && !format.contains("{3")) {
                return format;
            }
            try {
                return MessageFormat.format(format, redacted);
            } catch (IllegalArgumentException ignored) {
                return format;
            }
        }

        /**
         * Maps a {@code java.util.logging} level back to the short {@link System.Logger} label shown in the
         * line, so the output reads in {@link System.Logger.Level} terms rather than the backend's names.
         *
         * @param level the backend level of the record; must not be {@code null}
         * @return the display label, such as {@code TRACE}, {@code DEBUG}, {@code ERROR}, or {@code WARN}
         */
        private static String displayLevel(java.util.logging.Level level) {
            if (level == java.util.logging.Level.FINER) {
                return "TRACE";
            }
            if (level == java.util.logging.Level.FINE) {
                return "DEBUG";
            }
            if (level == java.util.logging.Level.SEVERE) {
                return "ERROR";
            }
            if (level == java.util.logging.Level.WARNING) {
                return "WARN";
            }
            return level.getName();
        }

        /**
         * Strips the {@value #NAMESPACE} prefix from a logger name so the line shows only the subsystem-relative
         * class path.
         *
         * @param logger the fully qualified logger name, or {@code null}
         * @return the name with the Cobalt namespace prefix removed, the name unchanged when it sits outside the
         * namespace, or the empty string when {@code logger} is {@code null}
         */
        private static String shorten(String logger) {
            if (logger == null) {
                return "";
            }
            return logger.startsWith(NAMESPACE + ".") ? logger.substring(NAMESPACE.length() + 1) : logger;
        }
    }
}
