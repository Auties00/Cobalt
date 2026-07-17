/**
 * Defines Cobalt's metrics telemetry, the second telemetry pillar alongside the logging facade in
 * {@code com.github.auties00.cobalt.telemetry.log}.
 *
 * <p>Metrics are surfaced as JFR (JDK Flight Recorder) events, so an application can record and analyse
 * Cobalt's runtime behaviour with the standard JDK tooling without Cobalt shipping its own metrics pipeline.
 *
 * <p>TODO: this package is a placeholder for the metrics half of {@code cobalt-telemetry-core}; the JFR event
 * definitions are not yet implemented.
 */
package com.github.auties00.cobalt.telemetry.metrics;
