package it.auties.whatsapp.listener;

import com.sun.source.util.TaskListener;
import it.auties.whatsapp.api.Whatsapp;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class RegisterListenerProcessor extends AbstractProcessor implements TaskListener {
    private static final String SIMPLE_CLASS_NAME = "ListenersInitializer";
    private static final String PACKAGE_NAME = Listener.class.getPackageName();
    private static final String QUALIFIED_CLASS_NAME = PACKAGE_NAME + ".ListenersInitializer";
    private static final String METHOD_NAME = "register";
    private static final String WHATSAPP_TYPE = Whatsapp.class.getName();
    private PrintStream outputStream;

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var annotated = roundEnv.getElementsAnnotatedWith(RegisterListener.class);
        for (var element : annotated) {
            if (!(element instanceof TypeElement typeElement)) {
                this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Only classes and records can be annotated with @RegisterListener", element);
                continue;
            }

            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Elements annotated with @RegisterListener cannot be abstract", element);
                continue;
            }

            var result = this.registerConstructors(typeElement);
            if (result) {
                continue;
            }

            this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Elements annotated with @RegisterListener must provide a public no parameters constructor or only a Whatsapp instance", element);
        }

        if (roundEnv.processingOver()) {
            this.closeOutputFile();
        }

        return true;
    }

    private void createOutputFile(Element owner) {
        if (this.outputStream == null) {
            try {
                JavaFileObject resource = this.processingEnv.getFiler().createSourceFile(QUALIFIED_CLASS_NAME, owner);
                this.outputStream = new PrintStream(resource.openOutputStream());
                this.outputStream.printf("package %s;%n", PACKAGE_NAME);
                this.outputStream.printf("public class %s {%n", SIMPLE_CLASS_NAME);
                this.outputStream.printf("    public static void %s(%s whatsapp) {%n", "register", WHATSAPP_TYPE);
            } catch (IOException var3) {
                throw new UncheckedIOException(var3);
            }
        }
    }

    private void closeOutputFile() {
        this.outputStream.println("   }");
        this.outputStream.println("}");
        this.outputStream.close();
    }

    private boolean registerConstructors(TypeElement classTree) {
        for (var member : classTree.getEnclosedElements()) {
            if (!(member instanceof ExecutableElement executableElement)) {
                continue;
            }

            if (!executableElement.getModifiers().contains(Modifier.PUBLIC) || executableElement.getKind() != ElementKind.CONSTRUCTOR) {
                continue;
            }

            if (executableElement.getParameters().isEmpty()) {
                this.createOutputFile(classTree);
                this.outputStream.printf("            whatsapp.addListener(new %s());%n", classTree.getQualifiedName());
                return true;
            }

            if (executableElement.getParameters().size() == 1 && this.isWhatsappType(executableElement)) {
                this.createOutputFile(classTree);
                this.outputStream.printf("            whatsapp.addListener(new %s(whatsapp));%n", classTree.getQualifiedName());
                return true;
            }
        }

        return false;
    }

    private boolean isWhatsappType(ExecutableElement executableElement) {
        var parameterType = executableElement.getParameters().getFirst();
        return parameterType instanceof TypeElement typeElement
                && typeElement.getQualifiedName().contentEquals(WHATSAPP_TYPE);
    }

    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(RegisterListener.class.getName());
    }

    public static String qualifiedClassName() {
        return QUALIFIED_CLASS_NAME;
    }

    public static String methodName() {
        return METHOD_NAME;
    }
}
