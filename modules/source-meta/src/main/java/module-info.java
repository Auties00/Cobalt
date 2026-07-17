module com.github.auties00.cobalt.meta {
    requires java.compiler;
    requires com.alibaba.fastjson2;

    exports com.github.auties00.cobalt.meta.annotation;
    exports com.github.auties00.cobalt.meta.processor;
    exports com.github.auties00.cobalt.meta.model;

    provides javax.annotation.processing.Processor
            with com.github.auties00.cobalt.meta.processor.WhatsAppSourceProcessor;
}
