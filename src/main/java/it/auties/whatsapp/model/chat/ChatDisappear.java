package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various actors that can initialize disappearing messages in a chat
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum ChatDisappear {
    /**
     * Changed in chat
     */
    CHANGED_IN_CHAT(0),

    /**
     * Initiated by me
     */
    INITIATED_BY_ME(1),

    /**
     * Initiated by other
     */
    INITIATED_BY_OTHER(2);

    @Getter
    private final int index;

    @JsonCreator
    public static ChatDisappear forJson(Linker linker) {
        return linker.disappear();
    }

    private static ChatDisappear forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Linker {
        @JsonProperty("1")
        @JsonPropertyDescription("enum")
        private ChatDisappear disappear;

        @JsonCreator
        public Linker(long index){
            this.disappear = ChatDisappear.forIndex((int) index);
        }
    }
}
