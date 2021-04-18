package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.response.model.NoResponse;
import it.auties.whatsapp4j.response.model.Response;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Accessors(chain = true,fluent = true)
@Getter
@Setter
@EqualsAndHashCode(of = {"tag"})
@ToString(of = "tag")
public class WhatsappResponse<J extends WhatsappResponse<J>> {
    @NotNull
    private String tag;
    private String description;
    private Response<?,?> data;

    public WhatsappResponse() {
        //No config required
        tag = "pong";
        data = new NoResponse<>();
    }

    /**
     * A model that contains information about a response sent by Whatsapp for a request
     *
     * @param tag         the tag used for the request
     * @param description a nullable String that describes how to categorize the data that is object holds
     */
    public WhatsappResponse(@NotNull String tag, String description) {
        this.tag = tag;
        this.description = description;
    }

    private static @NotNull String parseContent(@NotNull String content, int index) {
        return content.length() > index && content.charAt(index) == ',' ? parseContent(content, index + 1) : content.substring(index);
    }


}
