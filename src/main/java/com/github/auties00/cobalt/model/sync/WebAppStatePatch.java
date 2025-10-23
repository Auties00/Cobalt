package com.github.auties00.cobalt.model.sync;

import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.model.proto.sync.ActionValueSync;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;

import java.util.Collections;

public final class WebAppStatePatch {
    private final ActionValueSync sync;
    private final RecordSync.Operation operation;
    private final String index;

    public WebAppStatePatch(ActionValueSync sync, RecordSync.Operation operation, String... args) {
        var array = new JSONArray(1 + args.length);
        if (sync.action().isPresent()) {
            array.add(sync.action().get().indexName());
        }else if(sync.setting().isPresent()){
            array.add(sync.setting().get().indexName());
        }else {
            throw new IllegalArgumentException("Invalid sync: expected an action or setting");
        }
        Collections.addAll(array, args);
        this.sync = sync;
        this.index = array.toJSONString();
        this.operation = operation;
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
