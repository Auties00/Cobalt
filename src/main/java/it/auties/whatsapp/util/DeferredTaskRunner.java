package it.auties.whatsapp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeferredTaskRunner {
    private final AtomicBoolean condition;
    private final List<Runnable> deferredTasks;
    public DeferredTaskRunner() {
        this.condition = new AtomicBoolean(false);
        this.deferredTasks = new ArrayList<>();
    }

    public void execute() {
        condition.set(true);
        deferredTasks.forEach(Runnable::run);
        deferredTasks.clear();
    }

    public void schedule(Runnable runnable){
        if(condition.get()){
            runnable.run();
            return;
        }

        deferredTasks.add(runnable);
    }
}
