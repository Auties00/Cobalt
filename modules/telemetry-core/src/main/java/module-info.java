/**
 * Defines the telemetry facade for Cobalt.
 *
 * <p>This module is used by every other module to emit privacy-redacted diagnostics: sensitive values (phone
 * numbers, JIDs, tokens, codes, and key material) are masked to non-reversible, still-correlatable tokens
 * before a record reaches any sink, so they never appear in the clear, while ordinary values pass through
 * untouched. It is a dependency-free leaf that everything else can depend on, and redaction can be turned off
 * for trusted local debugging.
 */
module com.github.auties00.cobalt.telemetry {
    // JUL is the backing logging framework
    requires java.logging;

    // Logs: the Log facade, the redaction engine, and the LogRedactable contract
    exports com.github.auties00.cobalt.telemetry.log;

    // TODO: metrics -- the com.github.auties00.cobalt.telemetry.metrics package will hold the JFR event
    //       definitions (the second telemetry pillar), not yet implemented, so it is not exported yet
}
