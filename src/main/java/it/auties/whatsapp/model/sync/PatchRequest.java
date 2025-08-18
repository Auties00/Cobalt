package it.auties.whatsapp.model.sync;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.sync.RecordSync.Operation;

import java.util.ArrayList;
import java.util.List;

public record PatchRequest(PatchType type, List<PatchEntry> entries) {
    public record PatchEntry(ActionValueSync sync, String index, Operation operation) {
        public static PatchEntry of(ActionValueSync sync, Operation operation, String... args) {
            var index = JSON.toJSONString(toJsonArgs(sync, args));
            return new PatchEntry(sync, index, operation);
        }

        @SuppressWarnings("all")
        private static List<String> toJsonArgs(ActionValueSync sync, String... args) {
            var action = sync.action();
            if (action.isPresent()) {
                var index = new ArrayList<String>(args.length + 1);
                index.add(action.get().indexName());
                for(var arg : args) {
                    index.add(arg);
                }
                return index;
            }

            var setting = sync.setting();
            if (setting.isPresent()) {
                return List.of(setting.get().indexName());
            }

            return List.of();
        }
    }
}
