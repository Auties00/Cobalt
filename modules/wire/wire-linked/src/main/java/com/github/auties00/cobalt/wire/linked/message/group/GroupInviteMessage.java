package com.github.auties00.cobalt.wire.linked.message.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A message that carries an invitation link to join a WhatsApp group.
 *
 * <p>Group invite messages are a special kind of chat payload that let a
 * sender share access to one of their groups without the recipient having
 * to be added explicitly by an administrator. When the recipient taps the
 * invitation inside the chat, the client uses the embedded invite code to
 * join the referenced group, subject to the invite's expiration time.
 *
 * <p>Besides the core join data ({@link Jid group JID} and invite code),
 * the message also carries a user-facing preview composed of the group
 * name, an optional JPEG thumbnail and a free-form caption, so that the
 * recipient sees a rich preview of the group before accepting.
 *
 * <p>As a {@link ContextualMessage}, a group invite can also be sent as a
 * reply or with additional metadata through its {@link ContextInfo}.
 */
@ProtobufMessage(name = "Message.GroupInviteMessage")
public final class GroupInviteMessage implements ContextualMessage {
    /**
     * The JID of the group that this invitation grants access to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid groupJid;

    /**
     * The opaque invite code that, combined with the group JID, allows the
     * recipient to join the group.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String inviteCode;

    /**
     * The instant, expressed as seconds since the Unix epoch, at which the
     * invite code stops being valid.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    Long inviteExpiration;

    /**
     * The human-readable name of the group, shown in the invite preview.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String groupName;

    /**
     * The raw bytes of a JPEG thumbnail representing the group's picture,
     * used to render a preview alongside the invitation.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] jpegThumbnail;

    /**
     * An optional free-form caption that accompanies the invitation.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String caption;

    /**
     * Contextual metadata attached to this message, such as quoted content
     * or forwarding information.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The kind of group the invitation refers to, which distinguishes a
     * regular group from a community parent group.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    GroupType groupType;


    /**
     * Constructs a new group invite message with the given values.
     *
     * <p>This constructor is package-private because instances are normally
     * created through the generated {@code GroupInviteMessageBuilder} or by
     * the protobuf deserializer; callers should not invoke it directly.
     *
     * @param groupJid the JID of the target group, or {@code null} if unknown
     * @param inviteCode the opaque invite code granting access to the group
     * @param inviteExpiration the invite expiration time, as seconds since the Unix epoch
     * @param groupName the display name of the group for the preview
     * @param jpegThumbnail the JPEG bytes of the group's thumbnail, or {@code null}
     * @param caption an optional caption shown together with the invite
     * @param contextInfo optional contextual metadata for the message
     * @param groupType the kind of group this invitation refers to
     */
    GroupInviteMessage(Jid groupJid, String inviteCode, Long inviteExpiration, String groupName, byte[] jpegThumbnail, String caption, ContextInfo contextInfo, GroupType groupType) {
        this.groupJid = groupJid;
        this.inviteCode = inviteCode;
        this.inviteExpiration = inviteExpiration;
        this.groupName = groupName;
        this.jpegThumbnail = jpegThumbnail;
        this.caption = caption;
        this.contextInfo = contextInfo;
        this.groupType = groupType;
    }

    /**
     * Returns the JID of the group this invitation points to.
     *
     * @return an {@link Optional} containing the group JID, or empty if not set
     */
    public Optional<Jid> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns the opaque invite code used to join the group.
     *
     * <p>The recipient's client combines this code with the group JID to
     * perform the actual join operation against the server.
     *
     * @return an {@link Optional} containing the invite code, or empty if not set
     */
    public Optional<String> inviteCode() {
        return Optional.ofNullable(inviteCode);
    }

    /**
     * Returns the expiration time of the invite code, expressed as seconds
     * since the Unix epoch.
     *
     * <p>Once this instant has passed the invite code is no longer accepted
     * by the server and attempting to join will fail.
     *
     * @return an {@link OptionalLong} containing the expiration time, or empty if not set
     */
    public OptionalLong inviteExpiration() {
        return inviteExpiration == null ? OptionalLong.empty() : OptionalLong.of(inviteExpiration);
    }

    /**
     * Returns the display name of the group as it should appear in the
     * invitation preview.
     *
     * @return an {@link Optional} containing the group name, or empty if not set
     */
    public Optional<String> groupName() {
        return Optional.ofNullable(groupName);
    }

    /**
     * Returns the JPEG-encoded thumbnail bytes for the group picture.
     *
     * <p>The thumbnail is intended for rendering a visual preview of the
     * group next to the invite and is not required for joining.
     *
     * @return an {@link Optional} containing the JPEG bytes, or empty if no thumbnail is set
     */
    public Optional<byte[]> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }

    /**
     * Returns the optional caption shown together with the invite.
     *
     * @return an {@link Optional} containing the caption, or empty if none is set
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the contextual metadata associated with this message, such as
     * quoted content, forwarding markers or ephemeral settings.
     *
     * @return an {@link Optional} containing the {@link ContextInfo}, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the kind of group this invitation refers to.
     *
     * @return an {@link Optional} containing the {@link GroupType}, or empty if not set
     */
    public Optional<GroupType> groupType() {
        return Optional.ofNullable(groupType);
    }

    /**
     * Sets the JID of the group this invitation refers to.
     *
     * @param groupJid the new group JID, or {@code null} to clear it
     */
    public void setGroupJid(Jid groupJid) {
        this.groupJid = groupJid;
    }

    /**
     * Sets the opaque invite code used to join the group.
     *
     * @param inviteCode the new invite code, or {@code null} to clear it
     */
    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    /**
     * Sets the invite expiration time expressed as seconds since the Unix
     * epoch.
     *
     * @param inviteExpiration the new expiration time, or {@code null} to clear it
     */
    public void setInviteExpiration(Long inviteExpiration) {
        this.inviteExpiration = inviteExpiration;
    }

    /**
     * Sets the display name of the group used in the invitation preview.
     *
     * @param groupName the new group name, or {@code null} to clear it
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * Sets the JPEG-encoded thumbnail bytes for the group picture.
     *
     * @param jpegThumbnail the new thumbnail bytes, or {@code null} to clear them
     */
    public void setJpegThumbnail(byte[] jpegThumbnail) {
        this.jpegThumbnail = jpegThumbnail;
    }

    /**
     * Sets the optional caption shown together with the invitation.
     *
     * @param caption the new caption, or {@code null} to clear it
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Sets the contextual metadata associated with this message.
     *
     * @param contextInfo the new {@link ContextInfo}, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the kind of group this invitation refers to.
     *
     * @param groupType the new {@link GroupType}, or {@code null} to clear it
     */
    public void setGroupType(GroupType groupType) {
        this.groupType = groupType;
    }

    /**
     * Enumerates the flavors of group that a {@link GroupInviteMessage} can
     * point to.
     *
     * <p>WhatsApp groups can either be regular standalone groups or the
     * parent of a community, which in turn contains several sub-groups.
     * Clients use this discriminator to decide how to present the invite
     * preview and which join flow to trigger on acceptance.
     */
    @ProtobufEnum(name = "Message.GroupInviteMessage.GroupType")
    public static enum GroupType {
        /**
         * A regular WhatsApp group, not associated with any community.
         */
        DEFAULT(0),
        /**
         * The parent group of a community, which is a special group that
         * acts as the top-level container for a collection of sub-groups.
         */
        PARENT(1);

        /**
         * Constructs a new enum constant associated with the given protobuf
         * wire index.
         *
         * @param index the protobuf enum index used on the wire
         */
        GroupType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index for this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index associated with this constant.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return this.index;
        }
    }
}
