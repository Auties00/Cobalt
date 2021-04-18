package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;


import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information about an update about the metadata of a WhatsappChat
 *
 */
public final class ChatCmdResponse implements JsonResponseModel {
    @JsonProperty("id")
    private final @NotNull String jid;
    private final String cmd;
    private final @NotNull List<Object> data;

    /**
     * @param jid the jid of the WhatsappChat this update regards
     * @param cmd a nullable String used to describe the update
     * @param data a list of objects that represent the encoded update
     */
    public ChatCmdResponse(@JsonProperty("id") @NotNull String jid, String cmd,
                           @NotNull List<Object> data) {
        this.jid = jid;
        this.cmd = cmd;
        this.data = data;
    }

    @JsonProperty("id")
    public @NotNull String jid() {
        return jid;
    }

    public String cmd() {
        return cmd;
    }

    public @NotNull List<Object> data() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ChatCmdResponse) obj;
        return Objects.equals(this.jid, that.jid) &&
                Objects.equals(this.cmd, that.cmd) &&
                Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, cmd, data);
    }

    @Override
    public String toString() {
        return "ChatCmdResponse[" +
                "jid=" + jid + ", " +
                "cmd=" + cmd + ", " +
                "data=" + data + ']';
    }


}