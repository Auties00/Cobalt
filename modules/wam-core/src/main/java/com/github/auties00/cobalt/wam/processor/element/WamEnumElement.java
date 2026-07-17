package com.github.auties00.cobalt.wam.processor.element;

import com.github.auties00.cobalt.wam.processor.WamAnnotations;
import com.palantir.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the compile-time metadata extracted from a {@code @WamEnum}-annotated
 * enumeration, mapping each constant name to its wire index.
 *
 * @param enumElement the enum's type element in the AST
 * @param className   the fully-qualified JavaPoet class name
 * @param constants   an insertion-ordered map of constant name to wire index
 */
public record WamEnumElement(
        TypeElement enumElement,
        ClassName className,
        Map<String, Integer> constants
) {
    /**
     * Creates a {@code WamEnumElement} by reading the {@code @WamEnumConstant}
     * annotation value from each enum constant of the given type element.
     *
     * @param enumElement the {@code @WamEnum}-annotated enum type element
     * @param env         the processing environment
     * @return a new element containing all constant-to-index mappings
     * @throws IllegalStateException if a constant is missing {@code @WamEnumConstant}
     */
    public static WamEnumElement create(TypeElement enumElement, ProcessingEnvironment env) {
        Objects.requireNonNull(enumElement, "enumElement");
        var packageName = env.getElementUtils()
                .getPackageOf(enumElement)
                .getQualifiedName()
                .toString();
        var simpleName = enumElement.getSimpleName().toString();
        var className = ClassName.get(packageName, simpleName);
        var constants = new LinkedHashMap<String, Integer>();
        for (var field : ElementFilter.fieldsIn(enumElement.getEnclosedElements())) {
            if (!isEnumConstant(field)) {
                continue;
            }
            var annotation = WamAnnotations.findWamEnumConstant(field);
            if (annotation == null) {
                throw new IllegalStateException(
                        "@WamEnum constant " + enumElement.getSimpleName()
                                + "." + field.getSimpleName()
                                + " is missing @WamEnumConstant"
                );
            }
            var wireIndex = WamAnnotations.intValue(annotation, "value");
            constants.put(field.getSimpleName().toString(), wireIndex);
        }
        return new WamEnumElement(enumElement, className, Map.copyOf(constants));
    }

    private static boolean isEnumConstant(VariableElement field) {
        return field.getKind() == ElementKind.ENUM_CONSTANT;
    }
}
