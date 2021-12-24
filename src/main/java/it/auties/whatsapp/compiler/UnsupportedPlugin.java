package it.auties.whatsapp.compiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.*;
import it.auties.whatsapp.api.Unsupported;

import javax.tools.Diagnostic;

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
            var unsupported = getUnsupported(invoked);
            if(unsupported != null){
                trees.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                        unsupported.warning(), invocation, parameter);
            }

            return super.visitMethodInvocation(invocation, parameter);
        }

        private Unsupported getUnsupported(ExpressionTree invoked) {
            if(!(invoked instanceof MethodTree method)){
                return null;
            }

            return (Unsupported) method.getModifiers()
                    .getAnnotations()
                    .stream()
                    .filter(annotation -> annotation.getAnnotationType().toString().equals(Unsupported.class.getSimpleName()))
                    .findFirst()
                    .orElse(null);
        }
    }
}