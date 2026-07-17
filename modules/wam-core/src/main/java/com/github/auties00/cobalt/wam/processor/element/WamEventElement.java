package com.github.auties00.cobalt.wam.processor.element;

import com.github.auties00.cobalt.wam.processor.WamAnnotations;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.palantir.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the compile-time metadata extracted from a {@code @WamEvent}-annotated
 * interface, including all its annotated property methods.
 *
 * @param typeElement    the annotated interface element
 * @param className      the fully-qualified JavaPoet class name
 * @param packageName    the package containing the event interface
 * @param eventId        the numeric event identifier from {@code @WamEvent.id()}
 * @param channel        the transport channel
 * @param alphaWeight    the sampling weight for alpha builds
 * @param betaWeight     the sampling weight for beta builds
 * @param releaseWeight  the sampling weight for release builds
 * @param privateStatsId the private statistics identifier, or {@code -1}
 * @param properties     the annotated methods in declaration order
 */
public record WamEventElement(
        TypeElement typeElement,
        ClassName className,
        String packageName,
        int eventId,
        WamChannel channel,
        int alphaWeight,
        int betaWeight,
        int releaseWeight,
        int privateStatsId,
        List<WamPropertyElement> properties
) {
    /**
     * Creates a {@code WamEventElement} by reading the {@code @WamEvent}
     * annotation and all {@code @WamProperty}-annotated methods from the
     * given type element.
     *
     * @param element the annotated interface element
     * @param env     the processing environment
     * @return a new event element
     * @throws IllegalStateException if the element is not a valid event interface
     */
    public static WamEventElement create(Element element, ProcessingEnvironment env) {
        if (!(element instanceof TypeElement typeElement)) {
            throw new IllegalStateException("@WamEvent must annotate an interface, got: " + element);
        }

        validate(typeElement);

        var annotation = WamAnnotations.findWamEvent(typeElement);
        if (annotation == null) {
            throw new IllegalStateException("Missing @WamEvent on " + typeElement.getQualifiedName());
        }

        var packageName = env.getElementUtils()
                .getPackageOf(typeElement)
                .getQualifiedName()
                .toString();
        var simpleName = typeElement.getSimpleName().toString();
        var className = ClassName.get(packageName, simpleName);

        var eventId = WamAnnotations.intValue(annotation, "id");
        var channelName = WamAnnotations.enumValue(annotation, "channel");
        var channel = WamChannel.valueOf(channelName);
        var alphaWeight = WamAnnotations.intValue(annotation, "alphaWeight");
        var betaWeight = WamAnnotations.intValue(annotation, "betaWeight");
        var releaseWeight = WamAnnotations.intValue(annotation, "releaseWeight");
        var privateStatsId = WamAnnotations.intValue(annotation, "privateStatsId");

        var properties = new ArrayList<WamPropertyElement>();
        for (var enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) {
                continue;
            }
            var propAnnotation = WamAnnotations.findWamProperty(enclosed);
            if (propAnnotation == null) {
                continue;
            }
            var executableElement = (ExecutableElement) enclosed;
            properties.add(WamPropertyElement.create(executableElement, propAnnotation, env));
        }

        return new WamEventElement(
                typeElement,
                className,
                packageName,
                eventId,
                channel,
                alphaWeight,
                betaWeight,
                releaseWeight,
                privateStatsId,
                Collections.unmodifiableList(properties)
        );
    }

    private static void validate(TypeElement typeElement) {
        if (typeElement.getKind() != ElementKind.INTERFACE) {
            throw new IllegalStateException(
                    "@WamEvent must annotate an interface, got " + typeElement.getKind()
                            + " " + typeElement.getQualifiedName()
            );
        }
    }
}
