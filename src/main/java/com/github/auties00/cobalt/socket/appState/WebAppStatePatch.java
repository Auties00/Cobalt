package com.github.auties00.cobalt.socket.appState;

import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.model.sync.ActionValueSync;
import com.github.auties00.cobalt.model.sync.RecordSync;

import java.util.Collections;

public final class WebAppStatePatch {
    private final ActionValueSync sync;
    private final String index;
    private final RecordSync.Operation operation;

    private WebAppStatePatch(ActionValueSync sync, String index, RecordSync.Operation operation) {
        this.sync = sync;
        this.index = index;
        this.operation = operation;
    }

    public static WebAppStatePatch of(ActionValueSync sync, RecordSync.Operation operation, String... args) {
        var array = new JSONArray(1 + args.length);
        if (sync.action().isPresent()) {
            array.add(sync.action().get().indexName());
        }else if(sync.setting().isPresent()){
            array.add(sync.setting().get().indexName());
        }else {
            throw new IllegalArgumentException("Invalid sync: expected an action or setting");
        }
        Collections.addAll(array, args);
        return new WebAppStatePatch(sync, array.toJSONString(), operation);
    }

    public ActionValueSync sync() {
        return sync;
    }

    public String index() {
        return index;
    }

    public RecordSync.Operation operation() {
        return operation;
    }
}
