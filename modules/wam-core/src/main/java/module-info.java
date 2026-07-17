module com.github.auties00.cobalt.wam {
    requires java.compiler;
    requires static com.palantir.javapoet;

    exports com.github.auties00.cobalt.wam.annotation;
    exports com.github.auties00.cobalt.wam.binary;
    exports com.github.auties00.cobalt.wam.model;

    provides javax.annotation.processing.Processor
        with com.github.auties00.cobalt.wam.processor.WamEventProcessor;
}
