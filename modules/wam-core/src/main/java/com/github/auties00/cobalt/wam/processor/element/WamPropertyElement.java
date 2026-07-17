package com.github.auties00.cobalt.wam.processor.element;

import com.github.auties00.cobalt.wam.processor.WamAnnotations;
import com.github.auties00.cobalt.wam.model.WamType;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * Represents the compile-time metadata extracted from a single
 * {@code @WamProperty}-annotated method within a {@code @WamEvent} interface.
 *
 * @param fieldName   the method name, which doubles as the property name
 *                    (e.g. {@code "mediaType"})
 * @param index       the WAM wire field identifier from {@code @WamProperty.index()}
 * @param wamType     the wire encoding type from {@code @WamProperty.type()}
 * @param javaType    the raw value type (unwrapped from {@code Optional})
 * @param enumElement the enum metadata, non-{@code null} only when
 *                    {@code wamType == ENUM}
 */
public record WamPropertyElement(
        String fieldName,
        int index,
        WamType wamType,
        TypeMirror javaType,
        WamEnumElement enumElement
) {
    /**
     * Creates a {@code WamPropertyElement} by reading the {@code @WamProperty}
     * annotation from the given method element.
     *
     * @param method     the annotated method element
     * @param annotation the {@code @WamProperty} annotation mirror
     * @param env        the processing environment
     * @return a new property element
     */
    public static WamPropertyElement create(ExecutableElement method, AnnotationMirror annotation, ProcessingEnvironment env) {
        var fieldName = method.getSimpleName().toString();
        var index = WamAnnotations.intValue(annotation, "index");
        var wamTypeName = WamAnnotations.enumValue(annotation, "type");
        var wamType = WamType.valueOf(wamTypeName);
        var returnType = method.getReturnType();
        var javaType = unwrapOptional(returnType);
        WamEnumElement enumElement = null;
        if (wamType == WamType.ENUM) {
            enumElement = resolveEnumElement(javaType, env);
        }
        return new WamPropertyElement(fieldName, index, wamType, javaType, enumElement);
    }

    private static TypeMirror unwrapOptional(TypeMirror returnType) {
        if (!(returnType instanceof DeclaredType declaredType)) {
            return returnType;
        }
        var element = declaredType.asElement();
        if (!(element instanceof TypeElement typeElement)) {
            return returnType;
        }
        var qualifiedName = typeElement.getQualifiedName().toString();
        if (qualifiedName.equals("java.util.Optional")) {
            var typeArgs = declaredType.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return typeArgs.getFirst();
            }
        }
        return returnType;
    }

    private static WamEnumElement resolveEnumElement(TypeMirror javaType, ProcessingEnvironment env) {
        if (!(javaType instanceof DeclaredType declaredType)) {
            throw new IllegalStateException(
                    "ENUM-typed method must return Optional<EnumType>, got: " + javaType
            );
        }
        var typeElement = (TypeElement) declaredType.asElement();
        var wamEnumAnnotation = WamAnnotations.findWamEnum(typeElement);
        if (wamEnumAnnotation == null) {
            throw new IllegalStateException(
                    "ENUM-typed method references " + typeElement.getQualifiedName()
                            + " which is not annotated with @WamEnum"
            );
        }
        return WamEnumElement.create(typeElement, env);
    }
}
