package com.github.auties00.cobalt.meta.processor;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor that collects all WhatsApp source provenance
 * annotations and generates a {@code META-INF/wa-source-manifest.json}
 * resource file at compile time.
 *
 * <p>The manifest is a flat JSON array of entries, each describing a
 * mapping from a Cobalt type or member to a WhatsApp Web module/export
 * or a WhatsApp Mobile class/method.
 */
@SupportedAnnotationTypes({
        "com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule",
        "com.github.auties00.cobalt.meta.annotation.WhatsAppWebModules",
        "com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport",
        "com.github.auties00.cobalt.meta.annotation.WhatsAppWebExports",
        "com.github.auties00.cobalt.meta.annotation.WhatsAppMobileClass",
        "com.github.auties00.cobalt.meta.annotation.WhatsAppMobileClasses",
        "com.github.auties00.cobalt.meta.annotation.WhatsAppMobileMethod",
        "com.github.auties00.cobalt.meta.annotation.WhatsAppMobileMethods"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class WhatsAppSourceProcessor extends AbstractProcessor {
    private final List<JSONObject> entries = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            writeManifest();
            return true;
        }

        collectWebModules(roundEnv);
        collectWebExports(roundEnv);
        collectMobileClasses(roundEnv);
        collectMobileMethods(roundEnv);

        return true;
    }

    private void collectWebModules(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(WhatsAppWebModule.class)) {
            for (var annotation : element.getAnnotationsByType(WhatsAppWebModule.class)) {
                var entry = new JSONObject();
                entry.put("family", "WEB");
                entry.put("platform", annotation.platform().name());
                entry.put("cobaltType", qualifiedName(element));
                entry.put("sourceModule", annotation.moduleName());
                entries.add(entry);
            }
        }
    }

    private void collectWebExports(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(WhatsAppWebExport.class)) {
            for (var annotation : element.getAnnotationsByType(WhatsAppWebExport.class)) {
                var entry = new JSONObject();
                entry.put("family", "WEB");
                entry.put("platform", annotation.platform().name());
                entry.put("cobaltType", enclosingType(element));
                entry.put("cobaltMember", memberName(element));
                entry.put("cobaltMemberKind", memberKind(element));
                entry.put("sourceModule", annotation.moduleName());
                entry.put("sourceExports", toJsonArray(annotation.exports()));
                entry.put("adaptation", annotation.adaptation().name());
                entries.add(entry);
            }
        }
    }

    private void collectMobileClasses(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(WhatsAppMobileClass.class)) {
            for (var annotation : element.getAnnotationsByType(WhatsAppMobileClass.class)) {
                var entry = new JSONObject();
                entry.put("family", "MOBILE");
                entry.put("platform", annotation.platform().name());
                entry.put("cobaltType", qualifiedName(element));
                entry.put("sourceClass", annotation.className());
                entries.add(entry);
            }
        }
    }

    private void collectMobileMethods(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(WhatsAppMobileMethod.class)) {
            for (var annotation : element.getAnnotationsByType(WhatsAppMobileMethod.class)) {
                var entry = new JSONObject();
                entry.put("family", "MOBILE");
                entry.put("platform", annotation.platform().name());
                entry.put("cobaltType", enclosingType(element));
                entry.put("cobaltMember", memberName(element));
                entry.put("cobaltMemberKind", memberKind(element));
                entry.put("sourceClass", annotation.className());
                entry.put("sourceMethods", toJsonArray(annotation.methods()));
                entry.put("adaptation", annotation.adaptation().name());
                entries.add(entry);
            }
        }
    }

    private void writeManifest() {
        if (entries.isEmpty()) {
            return;
        }

        try {
            var resource = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/wa-source-manifest.json"
            );
            var manifest = new JSONArray(entries);
            try (var writer = resource.openWriter()) {
                writer.write(manifest.toJSONString(JSONWriter.Feature.PrettyFormat));
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to write wa-source-manifest.json: " + e.getMessage()
            );
        }
    }

    private static JSONArray toJsonArray(String[] values) {
        var array = new JSONArray(values.length);
        array.addAll(List.of(values));
        return array;
    }

    private static String qualifiedName(Element element) {
        if (element instanceof TypeElement type) {
            return type.getQualifiedName().toString();
        }
        return element.getEnclosingElement() instanceof TypeElement type
                ? type.getQualifiedName().toString()
                : element.toString();
    }

    private static String enclosingType(Element element) {
        var enclosing = element.getEnclosingElement();
        return enclosing instanceof TypeElement type
                ? type.getQualifiedName().toString()
                : enclosing.toString();
    }

    private static String memberName(Element element) {
        return element.getKind() == ElementKind.CONSTRUCTOR
                ? "<init>"
                : element.getSimpleName().toString();
    }

    private static String memberKind(Element element) {
        return switch (element.getKind()) {
            case METHOD -> "METHOD";
            case CONSTRUCTOR -> "CONSTRUCTOR";
            case FIELD -> "FIELD";
            case ENUM_CONSTANT -> "ENUM_CONSTANT";
            default -> element.getKind().name();
        };
    }
}
