package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;


import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information about the requested metadata of a Whatsapp group
 *
 */
public final class GroupMetadataResponse implements JsonResponseModel<GroupMetadataResponse> {
    @JsonProperty("id")
    private final @NotNull String jid;
    @JsonProperty("owner")
    private final @NotNull String founderJid;
    @JsonProperty("creation")
    private final int foundationTimestamp;
    private final @NotNull String subject;
    @JsonProperty("subjectTime")
    private final Integer lastSubjectUpdateTimestamp;
    @JsonProperty("subjectOwner")
    private final String lastSubjectUpdateJid;
    @JsonProperty("restrict")
    private final boolean onlyAdminsCanChangeSettings;
    @JsonProperty("announce")
    private final boolean onlyAdminsCanWriteMessages;
    @JsonProperty("desc")
    private final String description;
    @JsonProperty("descId")
    private final String descriptionMessageId;
    @JsonProperty("descOwner")
    private final String lastDescriptionUpdateJid;
    @JsonProperty("descTime")
    private final Integer lastDescriptionUpdateTimestamp;
    private final @NotNull List<GroupParticipant> participants;

    /**
     * @param jid the jid of the group
     * @param founderJid the jid of the user that created this group
     * @param foundationTimestamp the time in seconds since {@link Instant#EPOCH} when the group was created
     * @param subject the name of the group
     * @param lastSubjectUpdateTimestamp the nullable last time in seconds since {@link Instant#EPOCH} when the group's subject was last updated
     * @param lastSubjectUpdateJid the nullable jid of the user that changed the subject of the group
     * @param onlyAdminsCanChangeSettings a flag that indicated if only admins are allowed to modify the settings of the group
     * @param onlyAdminsCanWriteMessages a flag that indicated if only admins are allowed to send messages in the group
     * @param description the nullable description of the group
     * @param descriptionMessageId the nullable id associated with the description of this group
     * @param lastDescriptionUpdateJid the nullable id associated with the previous description of this group
     * @param lastDescriptionUpdateTimestamp the nullable time in seconds since {@link Instant#EPOCH} when the group's description was last updated
     * @param participants a non null list of all the participants of the group
     */
    public GroupMetadataResponse(@JsonProperty("id") @NotNull String jid,
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
                                 @NotNull List<GroupParticipant> participants) {
        this.jid = jid;
        this.founderJid = founderJid;
        this.foundationTimestamp = foundationTimestamp;
        this.subject = subject;
        this.lastSubjectUpdateTimestamp = lastSubjectUpdateTimestamp;
        this.lastSubjectUpdateJid = lastSubjectUpdateJid;
        this.onlyAdminsCanChangeSettings = onlyAdminsCanChangeSettings;
        this.onlyAdminsCanWriteMessages = onlyAdminsCanWriteMessages;
        this.description = description;
        this.descriptionMessageId = descriptionMessageId;
        this.lastDescriptionUpdateJid = lastDescriptionUpdateJid;
        this.lastDescriptionUpdateTimestamp = lastDescriptionUpdateTimestamp;
        this.participants = participants;
    }

    @JsonProperty("id")
    public @NotNull String jid() {
        return jid;
    }

    @JsonProperty("owner")
    public @NotNull String founderJid() {
        return founderJid;
    }

    @JsonProperty("creation")
    public int foundationTimestamp() {
        return foundationTimestamp;
    }

    public @NotNull String subject() {
        return subject;
    }

    @JsonProperty("subjectTime")
    public Integer lastSubjectUpdateTimestamp() {
        return lastSubjectUpdateTimestamp;
    }

    @JsonProperty("subjectOwner")
    public String lastSubjectUpdateJid() {
        return lastSubjectUpdateJid;
    }

    @JsonProperty("restrict")
    public boolean onlyAdminsCanChangeSettings() {
        return onlyAdminsCanChangeSettings;
    }

    @JsonProperty("announce")
    public boolean onlyAdminsCanWriteMessages() {
        return onlyAdminsCanWriteMessages;
    }

    @JsonProperty("desc")
    public String description() {
        return description;
    }

    @JsonProperty("descId")
    public String descriptionMessageId() {
        return descriptionMessageId;
    }

    @JsonProperty("descOwner")
    public String lastDescriptionUpdateJid() {
        return lastDescriptionUpdateJid;
    }

    @JsonProperty("descTime")
    public Integer lastDescriptionUpdateTimestamp() {
        return lastDescriptionUpdateTimestamp;
    }

    public @NotNull List<GroupParticipant> participants() {
        return participants;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupMetadataResponse) obj;
        return Objects.equals(this.jid, that.jid) &&
                Objects.equals(this.founderJid, that.founderJid) &&
                this.foundationTimestamp == that.foundationTimestamp &&
                Objects.equals(this.subject, that.subject) &&
                Objects.equals(this.lastSubjectUpdateTimestamp, that.lastSubjectUpdateTimestamp) &&
                Objects.equals(this.lastSubjectUpdateJid, that.lastSubjectUpdateJid) &&
                this.onlyAdminsCanChangeSettings == that.onlyAdminsCanChangeSettings &&
                this.onlyAdminsCanWriteMessages == that.onlyAdminsCanWriteMessages &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.descriptionMessageId, that.descriptionMessageId) &&
                Objects.equals(this.lastDescriptionUpdateJid, that.lastDescriptionUpdateJid) &&
                Objects.equals(this.lastDescriptionUpdateTimestamp, that.lastDescriptionUpdateTimestamp) &&
                Objects.equals(this.participants, that.participants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, founderJid, foundationTimestamp, subject, lastSubjectUpdateTimestamp, lastSubjectUpdateJid, onlyAdminsCanChangeSettings, onlyAdminsCanWriteMessages, description, descriptionMessageId, lastDescriptionUpdateJid, lastDescriptionUpdateTimestamp, participants);
    }

    @Override
    public String toString() {
        return "GroupMetadataResponse[" +
                "jid=" + jid + ", " +
                "founderJid=" + founderJid + ", " +
                "foundationTimestamp=" + foundationTimestamp + ", " +
                "subject=" + subject + ", " +
                "lastSubjectUpdateTimestamp=" + lastSubjectUpdateTimestamp + ", " +
                "lastSubjectUpdateJid=" + lastSubjectUpdateJid + ", " +
                "onlyAdminsCanChangeSettings=" + onlyAdminsCanChangeSettings + ", " +
                "onlyAdminsCanWriteMessages=" + onlyAdminsCanWriteMessages + ", " +
                "description=" + description + ", " +
                "descriptionMessageId=" + descriptionMessageId + ", " +
                "lastDescriptionUpdateJid=" + lastDescriptionUpdateJid + ", " +
                "lastDescriptionUpdateTimestamp=" + lastDescriptionUpdateTimestamp + ", " +
                "participants=" + participants + ']';
    }


}