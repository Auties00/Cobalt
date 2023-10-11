package it.auties.whatsapp.model.jid;

/**
 * The constants of this enumerated type describe the various types of jids currently supported
 */
public enum JidType {
    /**
     * Represents a device connected using the multi device beta
     */
    COMPANION,
    /**
     * Regular Whatsapp contact Jid
     */
    USER,
    /**
     * Official survey account
     */
    OFFICIAL_SURVEY_ACCOUNT,
    /**
     * Lid
     */
    LID,
    /**
     * Broadcast list
     */
    BROADCAST,
    /**
     * Official business account
     */
    OFFICIAL_BUSINESS_ACCOUNT,
    /**
     * Group Chat Jid
     */
    GROUP,
    /**
     * Group Call Jid
     */
    GROUP_CALL,
    /**
     * Server Jid: Used to send nodes to Whatsapp usually
     */
    SERVER,
    /**
     * Announcements Chat Jid: Read only chat, usually used by Whatsapp for log updates
     */
    ANNOUNCEMENT,
    /**
     * IAS Chat jid
     */
    IAS,
    /**
     * Image Status Jid of a contact
     */
    STATUS,
    /**
     * Unknown Jid type
     */
    UNKNOWN,
    /**
     * Channel
     */
    NEWSLETTER
}
