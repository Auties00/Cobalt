package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information about all the blocked contacts for a session
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class BlocklistResponse implements JsonResponseModel {
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
}
