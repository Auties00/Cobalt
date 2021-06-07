package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * A json model that contains information about the requested metadata of a Whatsapp group
 *
 * @param jid                            the jid of the group
 * @param founderJid                     the jid of the user that created this group
 * @param foundationTimestamp            the time in seconds since {@link Instant#EPOCH} when the group was created
 * @param subject                        the name of the group
 * @param lastSubjectUpdateTimestamp     the nullable last time in seconds since {@link Instant#EPOCH} when the group's subject was last updated
 * @param lastSubjectUpdateJid           the nullable jid of the user that changed the subject of the group
 * @param onlyAdminsCanChangeSettings    a flag that indicated if only admins are allowed to modify the settings of the group
 * @param onlyAdminsCanWriteMessages     a flag that indicated if only admins are allowed to send messages in the group
 * @param description                    the nullable description of the group
 * @param descriptionMessageId           the nullable id associated with the description of this group
 * @param lastDescriptionUpdateJid       the nullable id associated with the previous description of this group
 * @param lastDescriptionUpdateTimestamp the nullable time in seconds since {@link Instant#EPOCH} when the group's description was last updated
 * @param participants                   a non null list of all the participants of the group
 */
public record GroupMetadataResponse(@JsonProperty("id") @NotNull String jid,
                                          @JsonProperty("owner") @NotNull String founderJid,
                                          @JsonProperty("creation") int foundationTimestamp, @NotNull String subject,
                                          @JsonProperty("subjectTime") Integer lastSubjectUpdateTimestamp,
                                          @JsonProperty("subjectOwner") String lastSubjectUpdateJid,
                                          @JsonProperty("restrict") boolean onlyAdminsCanChangeSettings,
                                          @JsonProperty("announce") boolean onlyAdminsCanWriteMessages,
                                          @JsonProperty("desc") String description,
                                          @JsonProperty("descId") String descriptionMessageId,
                                          @JsonProperty("descOwner") String lastDescriptionUpdateJid,
                                          @JsonProperty("descTime") Integer lastDescriptionUpdateTimestamp,
                                          @NotNull List<GroupParticipant> participants) implements JsonResponseModel {
}