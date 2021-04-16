package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public final class SubjectChangeResponse implements JsonResponseModel<SubjectChangeResponse> {
    private final @NotNull String subject;
    @JsonProperty("s_t")
    private final long timestamp;
    @JsonProperty("s_o")
    private final @NotNull String authorJid;

    public SubjectChangeResponse(@NotNull String subject, @JsonProperty("s_t") long timestamp, @JsonProperty("s_o") @NotNull String authorJid) {
        this.subject = subject;
        this.timestamp = timestamp;
        this.authorJid = authorJid;
    }

    public @NotNull String subject() {
        return subject;
    }

    @JsonProperty("s_t")
    public long timestamp() {
        return timestamp;
    }

    @JsonProperty("s_o")
    public @NotNull String authorJid() {
        return authorJid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SubjectChangeResponse) obj;
        return Objects.equals(this.subject, that.subject) &&
                this.timestamp == that.timestamp &&
                Objects.equals(this.authorJid, that.authorJid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, timestamp, authorJid);
    }

    @Override
    public String toString() {
        return "SubjectChangeResponse[" +
                "subject=" + subject + ", " +
                "timestamp=" + timestamp + ", " +
                "authorJid=" + authorJid + ']';
    }

}
