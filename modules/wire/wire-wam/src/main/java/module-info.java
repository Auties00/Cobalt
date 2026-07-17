/**
 * Defines the WAM (WhatsApp Metrics) telemetry event and enum schema for Cobalt.
 *
 * <p>This module is consumed by the {@code cobalt-wam-core} annotation processor, which reads its
 * {@code @WamEvent}/{@code @WamEnum} declarations to generate the concrete {@code *Impl}/{@code *Builder} types
 * and the {@code WamEventRegistry}. It lives apart from {@code cobalt-wam-core} precisely because that module is
 * the processor itself, and a processor cannot be placed on its own annotation-processor path to process its own
 * sources. It depends only on {@code cobalt-wam-core} and references no domain or transport model; the WAM
 * runtime services (beaconing, sampling, daily stats) that consume the generated registry live in the client
 * library.
 */
module com.github.auties00.cobalt.wire.wam {
    // transitive: the WAM annotations, WamEventSpec/WamType model and binary codec the generated code targets
    requires transitive com.github.auties00.cobalt.wam;
    // source-provenance annotations on the WAM declarations; SOURCE retention, so static
    requires static com.github.auties00.cobalt.meta;

    // WAM event and enum declaration schema
    exports com.github.auties00.cobalt.wire.wam.event;
    exports com.github.auties00.cobalt.wire.wam.type;
}
