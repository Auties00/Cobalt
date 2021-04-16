package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information about all the blocked contacts for a session
 *
 */
public final class BlocklistResponse implements JsonResponseModel<BlocklistResponse> {
    private final int id;
    @JsonProperty("blocklist")
    private final @NotNull List<String> blockedJids;

    /**
     * @param id an unsigned unique identifier for this request
     * @param blockedJids a non null List of blocked contacts for a session, might be empty
     */
    public BlocklistResponse(int id,
                             @NotNull @JsonProperty("blocklist") List<String> blockedJids) {
        this.id = id;
        this.blockedJids = blockedJids;
    }

    public int id() {
        return id;
    }

    @JsonProperty("blocklist")
    public @NotNull List<String> blockedJids() {
        return blockedJids;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BlocklistResponse) obj;
        return this.id == that.id &&
                Objects.equals(this.blockedJids, that.blockedJids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, blockedJids);
    }

    @Override
    public String toString() {
        return "BlocklistResponse[" +
                "id=" + id + ", " +
                "blockedJids=" + blockedJids + ']';
    }

}
