package com.github.auties00.cobalt.wam.processor;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;
import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.annotation.WamProperty;

import javax.lang.model.element.*;

/**
 * Utility methods for reading annotation mirrors from the AST without
 * depending on runtime annotation class loading.
 *
 * <p>All lookups use fully-qualified annotation names resolved from the
 * type mirrors, so they work correctly even when the annotation classes
 * are not yet compiled in the current round.
 */
public final class WamAnnotations {
    static final String WAM_EVENT = WamEvent.class.getName();
    static final String WAM_PROPERTY = WamProperty.class.getName();
    static final String WAM_ENUM = WamEnum.class.getName();
    static final String WAM_ENUM_CONSTANT = WamEnumConstant.class.getName();

    private WamAnnotations() {
        throw new AssertionError();
    }

    /**
     * Finds the {@code @WamEvent} annotation mirror on the given element,
     * or {@code null} if not present.
     */
    public static AnnotationMirror findWamEvent(Element element) {
        return findByName(element, WAM_EVENT);
    }

    /**
     * Finds the {@code @WamProperty} annotation mirror on the given element,
     * or {@code null} if not present.
     */
    public static AnnotationMirror findWamProperty(Element element) {
        return findByName(element, WAM_PROPERTY);
    }

    /**
     * Finds the {@code @WamEnum} annotation mirror on the given element,
     * or {@code null} if not present.
     */
    public static AnnotationMirror findWamEnum(Element element) {
        return findByName(element, WAM_ENUM);
    }

    /**
     * Finds the {@code @WamEnumConstant} annotation mirror on the given element,
     * or {@code null} if not present.
     */
    public static AnnotationMirror findWamEnumConstant(Element element) {
        return findByName(element, WAM_ENUM_CONSTANT);
    }

    /**
     * Reads an integer attribute from the given annotation mirror.
     */
    public static int intValue(AnnotationMirror mirror, String attribute) {
        var value = attributeValue(mirror, attribute);
        if (value == null) {
            throw new IllegalStateException("Missing attribute '" + attribute + "' on " + mirror);
        }
        return ((Number) value.getValue()).intValue();
    }

    /**
     * Reads a string attribute from the given annotation mirror.
     */
    public static String stringValue(AnnotationMirror mirror, String attribute) {
        var value = attributeValue(mirror, attribute);
        if (value == null) {
            throw new IllegalStateException("Missing attribute '" + attribute + "' on " + mirror);
        }
        return (String) value.getValue();
    }

    /**
     * Reads an enum attribute from the given annotation mirror,
     * returning the enum constant's simple name as a string.
     */
    public static String enumValue(AnnotationMirror mirror, String attribute) {
        var value = attributeValue(mirror, attribute);
        if (value == null) {
            throw new IllegalStateException("Missing attribute '" + attribute + "' on " + mirror);
        }
        var variableElement = (VariableElement) value.getValue();
        return variableElement.getSimpleName().toString();
    }

    /**
     * Reads an attribute value from the given annotation mirror,
     * falling back to the annotation's default value if the attribute
     * is not explicitly set.
     *
     * @return the annotation value, or {@code null} if not found
     */
    public static AnnotationValue attributeValue(AnnotationMirror mirror, String attribute) {
        for (var entry : mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(attribute)) {
                return entry.getValue();
            }
        }
        // Check for default value
        for (var method : mirror.getAnnotationType().asElement().getEnclosedElements()) {
            if (method.getSimpleName().contentEquals(attribute)) {
                if (method instanceof ExecutableElement exec) {
                    return exec.getDefaultValue();
                }
            }
        }
        return null;
    }

    private static AnnotationMirror findByName(Element element, String qualifiedName) {
        for (var mirror : element.getAnnotationMirrors()) {
            var annotationType = mirror.getAnnotationType().asElement();
            if (annotationType instanceof TypeElement typeElement) {
                if (typeElement.getQualifiedName().contentEquals(qualifiedName)) {
                    return mirror;
                }
            }
        }
        return null;
    }
}
