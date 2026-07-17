package com.github.auties00.cobalt.wam.processor;

import com.github.auties00.cobalt.wam.processor.element.WamEventElement;
import com.github.auties00.cobalt.wam.processor.generator.WamBuilderGenerator;
import com.github.auties00.cobalt.wam.processor.generator.WamImplGenerator;
import com.github.auties00.cobalt.wam.processor.generator.WamRegistryGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An annotation processor that generates companion {@code *Impl} and
 * {@code *Builder} classes for every {@code @WamEvent}-annotated
 * interface, plus a single {@code WamEventRegistry} dispatch table
 * shared across the whole event hierarchy.
 *
 * <p>The generated {@code *Impl} class implements the event interface
 * and contains zero-reflection {@code sizeOf}, {@code encode}, and
 * static {@code decode} methods that talk to {@code WamEventEncoder}
 * and {@code WamEventDecoder}.
 *
 * <p>The {@code WamEventRegistry} is emitted once at the end of the
 * compilation, in the same package as the {@code *Impl} classes so
 * that its {@code decode} switch can reference each impl directly
 * even though the impls themselves are package-private.
 *
 * @see com.github.auties00.cobalt.wam.annotation.WamEvent
 */
@SupportedAnnotationTypes("com.github.auties00.cobalt.wam.annotation.WamEvent")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class WamEventProcessor extends AbstractProcessor {
    private final List<WamEventElement> collectedEvents = new ArrayList<>();
    private final Set<String> seenQualifiedNames = new HashSet<>();
    private boolean registryWritten = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (var annotation : annotations) {
            for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
                try {
                    var eventElement = WamEventElement.create(element, processingEnv);
                    var qualifiedName = eventElement.typeElement().getQualifiedName().toString();
                    if (seenQualifiedNames.add(qualifiedName)) {
                        collectedEvents.add(eventElement);
                    }
                    var fieldCount = eventElement.properties().size();
                    if (fieldCount > WamImplGenerator.MAP_THRESHOLD) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.WARNING,
                                "WAM event '" + eventElement.className().simpleName() + "' has " + fieldCount
                                        + " fields (over the " + WamImplGenerator.MAP_THRESHOLD + "-field threshold); "
                                        + "generating a map-backed implementation instead of the efficient per-field "
                                        + "one to stay within the JVM 255-parameter and 64KB method-size limits.",
                                element
                        );
                    }
                    WamImplGenerator.generate(eventElement)
                            .writeTo(processingEnv.getFiler());
                    WamBuilderGenerator.generate(eventElement)
                            .writeTo(processingEnv.getFiler());
                } catch (Throwable e) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "Failed to generate WAM sources: " + e.getMessage(),
                            element
                    );
                }
            }
        }

        if (!registryWritten && !collectedEvents.isEmpty()) {
            try {
                writeRegistry();
                registryWritten = true;
            } catch (Throwable e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate WamEventRegistry: " + e.getMessage()
                );
            }
        }

        return true;
    }

    private void writeRegistry() throws IOException {
        var packageGroups = new HashMap<String, List<WamEventElement>>();
        for (var event : collectedEvents) {
            packageGroups.computeIfAbsent(event.packageName(), _ -> new ArrayList<>()).add(event);
        }
        if (packageGroups.size() != 1) {
            throw new IllegalStateException(
                    "@WamEvent classes must all live in a single package, found: "
                            + packageGroups.keySet());
        }
        var entry = packageGroups.entrySet().iterator().next();
        var sorted = new ArrayList<>(entry.getValue());
        sorted.sort(Comparator.comparingInt(WamEventElement::eventId));
        WamRegistryGenerator.generate(entry.getKey(), sorted)
                .writeTo(processingEnv.getFiler());
    }
}
