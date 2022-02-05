package it.auties.whatsapp.compiler;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import it.auties.whatsapp.api.Unsupported;

import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.Optional;

public class UnsupportedPlugin implements Plugin, TaskListener {
    private Trees trees;

    @Override
    public String getName() {
        return "UnsupportedWarner";
    }

    @Override
    public boolean autoStart() {
        return true;
    }

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(this);
        this.trees = Trees.instance(task);
    }

    @Override
    public void finished(TaskEvent event) {
        if(event.getKind() != TaskEvent.Kind.ANALYZE){
            return;
        }

        var unit = event.getCompilationUnit();
        unit.accept(new Scanner(), unit);
    }

    private class Scanner extends TreeScanner<Void, CompilationUnitTree> {
        @Override
        public Void visitMethodInvocation(MethodInvocationTree invocation, CompilationUnitTree parameter) {
            var invoked = invocation.getMethodSelect();
            getUnsupported(invoked)
                    .ifPresent(unsupported -> printWarning(invocation, parameter, unsupported));
            return super.visitMethodInvocation(invocation, parameter);
        }

        private Optional<Unsupported> getUnsupported(ExpressionTree invoked) {
            return Optional.ofNullable(invoked)
                    .filter(tree -> tree instanceof MethodTree)
                    .map(tree -> (MethodTree) tree)
                    .map(MethodTree::getModifiers)
                    .map(ModifiersTree::getAnnotations)
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(this::isUnsupported)
                    .findFirst()
                    .map(annotation -> (Unsupported) annotation);
        }

        private boolean isUnsupported(AnnotationTree annotation) {
            return annotation.getAnnotationType()
                    .toString()
                    .equals(Unsupported.class.getSimpleName());
        }

        private void printWarning(MethodInvocationTree invocation, CompilationUnitTree parameter, Unsupported unsupported) {
            trees.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                    unsupported.warning(), invocation, parameter);
        }
    }
}