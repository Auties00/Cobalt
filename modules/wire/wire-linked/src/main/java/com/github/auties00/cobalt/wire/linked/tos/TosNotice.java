package com.github.auties00.cobalt.wire.linked.tos;

import com.github.auties00.cobalt.wire.linked.props.ABProp;

import java.util.Objects;

/**
 * Represents a WhatsApp Terms-of-Service notice definition, identified by the
 * stable notice id the relay echoes in the {@code w:tos} acceptance protocol.
 *
 * <p>A TOS notice is a versioned agreement (interoperability terms, bot terms,
 * marketing-message disclosure, newsletter terms, and so on) whose per-user
 * acceptance state the client tracks and, for some surfaces, gates behaviour on.
 * The notice id is resolved at runtime by the {@code TosService} from this
 * definition:
 * <ul>
 *   <li>the active AB-prop is {@link #smbProp} when the linked device is a
 *       WhatsApp Business (SMB) client and it is set, otherwise {@link #webProp};</li>
 *   <li>the resolved id is the active AB-prop's value when it is non-blank,
 *       otherwise the static {@link #defaultId};</li>
 *   <li>when {@link #multiValued} is {@code true} the value is a comma-separated
 *       list of ids (a notice group) rather than a single id.</li>
 * </ul>
 *
 * <p>This record is immutable and thread-safe.
 *
 * <p>The constants in this record are generated automatically by
 * {@code tools/web/tos-codegen} from the WhatsApp Web TOS modules; each
 * constant's documentation records its source definition. Do not edit the
 * constants manually.
 *
 * @param defaultId    the static notice id baked into the source, or {@code null}
 *                     when the id is supplied entirely by an AB-prop
 * @param webProp      the AB-prop supplying or overriding the id for the web
 *                     (non-SMB) client, or {@code null} when the id is fixed
 * @param smbProp      the AB-prop supplying the id for the SMB (Business) client,
 *                     or {@code null} when the notice has no SMB variant
 * @param multiValued  whether the resolved AB-prop value is a comma-separated list
 *                     of ids rather than a single id
 */
public record TosNotice(String defaultId, ABProp webProp, ABProp smbProp, boolean multiValued) {

    /**
     * The BIZ_BOT Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotTosIds.getBizBotTosId}.
     */
    public static final TosNotice BIZ_BOT = new TosNotice("20231027", null, null, false);

    /**
     * The BIZ_BROADCAST Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBizBroadcastTos.getBizBroadcastTosId}.
     */
    public static final TosNotice BIZ_BROADCAST = new TosNotice("20250915", null, null, false);

    /**
     * The BIZ_BROADCAST_GENAI Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBizBroadcastGenAIToS.BIZ_BROADCAST_GENAI_TOS_ID}.
     */
    public static final TosNotice BIZ_BROADCAST_GENAI = new TosNotice("20251104", null, null, false);

    /**
     * The BOT_AGENT Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotTosIds.getBotAgentTosId}.
     */
    public static final TosNotice BOT_AGENT = new TosNotice("20230901", ABProp.AI_PDFN_TOS_SHORTCUT_NOTICE_ID, null, false);

    /**
     * The BOT_INVOKE Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotTosIds.getBotInvokeTosId}.
     */
    public static final TosNotice BOT_INVOKE = new TosNotice("20230902", ABProp.AI_PDFN_TOS_INVOKE_NOTICE_ID, null, false);

    /**
     * The BOT_SHORTCUT Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotTosIds.getBotShortcutTosId}.
     */
    public static final TosNotice BOT_SHORTCUT = new TosNotice("20240216", ABProp.AI_PDFN_TOS_SHORTCUT_NOTICE_ID, null, false);

    /**
     * The INLINE_BOT Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotGating.getInlineBotNoticeIds}.
     */
    public static final TosNotice INLINE_BOT = new TosNotice(null, ABProp.AI_PDFN_TOS_INLINE_NOTICES, null, true);

    /**
     * The MASTER_BOT Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotGating.getMasterBotNoticeId}.
     */
    public static final TosNotice MASTER_BOT = new TosNotice(null, ABProp.AI_PDFN_TOS_MASTER_NOTICE_ID, null, false);

    /**
     * The MM_SIGNAL_SHARING_DISCLOSURE Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebMmSignalSharingTos.getMmSignalSharingDisclosureTosId}.
     */
    public static final TosNotice MM_SIGNAL_SHARING_DISCLOSURE = new TosNotice("20231028", null, null, false);

    /**
     * The NEWSLETTER_ADMIN_INVITE Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebNewsletterGatingUtils.getNewsletterAdminInviteTos}.
     */
    public static final TosNotice NEWSLETTER_ADMIN_INVITE = new TosNotice(null, ABProp.NEWSLETTER_ADMIN_INVITE_TOS_ID, ABProp.NEWSLETTER_ADMIN_INVITE_TOS_ID_SMB_WEB, false);

    /**
     * The NEWSLETTER_ADMIN_INVITE_NUX Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebWamoNewsletterGatingUtils.getNewsletterAdminInviteNux}.
     */
    public static final TosNotice NEWSLETTER_ADMIN_INVITE_NUX = new TosNotice(null, ABProp.NEWSLETTER_ADMIN_INVITE_NUX_ID, null, false);

    /**
     * The NEWSLETTER_CONSUMER Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebNewsletterGatingUtils.getNewsletterConsumerTos}.
     */
    public static final TosNotice NEWSLETTER_CONSUMER = new TosNotice(null, ABProp.NEWSLETTER_TOS_NOTICE_ID, ABProp.NEWSLETTER_TOS_NOTICE_ID_SMB_WEB, false);

    /**
     * The NEWSLETTER_CONSUMER_NUX Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebWamoNewsletterGatingUtils.getNewsletterConsumerNux}.
     */
    public static final TosNotice NEWSLETTER_CONSUMER_NUX = new TosNotice(null, ABProp.NEWSLETTER_NUX_NOTICE_ID, null, false);

    /**
     * The NEWSLETTER_PRODUCER Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebNewsletterGatingUtils.getNewsletterProducerTos}.
     */
    public static final TosNotice NEWSLETTER_PRODUCER = new TosNotice(null, ABProp.NEWSLETTER_CREATION_TOS_ID, ABProp.NEWSLETTER_CREATION_TOS_ID_SMB_WEB, false);

    /**
     * The NEWSLETTER_PRODUCER_NUX Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebNewsletterGatingUtils.getNewsletterProducerNux}.
     */
    public static final TosNotice NEWSLETTER_PRODUCER_NUX = new TosNotice(null, ABProp.NEWSLETTER_CREATION_NUX_ID, null, false);

    /**
     * The NON_BLOCKING_BOT Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotGating.getNonBlockingBotNoticeIds}.
     */
    public static final TosNotice NON_BLOCKING_BOT = new TosNotice(null, ABProp.AI_PDFN_TOS_NON_BLOCKING_NOTICES, null, true);

    /**
     * The TOS_3 Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebTos.TOS_3_ID}.
     */
    public static final TosNotice TOS_3 = new TosNotice("20210210", null, null, false);

    /**
     * The UGC_AI_STUDIO Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebBotTosIds.getUgcAiStudioTosId}.
     */
    public static final TosNotice UGC_AI_STUDIO = new TosNotice("20240729", null, null, false);

    /**
     * The WAMO_LINKED Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebWamoNewsletterGatingUtils.getWamoLinkedTos}.
     */
    public static final TosNotice WAMO_LINKED = new TosNotice(null, ABProp.WAMO_PRIVACY_TOS_LINKED_HIGHLIGHTED_NOTICE_ID, null, false);

    /**
     * The WAMO_UNLINKED Terms-of-Service notice.
     *
     * <p>Source: {@code WAWebWamoNewsletterGatingUtils.getWamoUnlinkedTos}.
     */
    public static final TosNotice WAMO_UNLINKED = new TosNotice(null, ABProp.WAMO_PRIVACY_TOS_UNLINKED_HIGHLIGHTED_NOTICE_ID, null, false);

    /**
     * Constructs a new {@code TosNotice} definition.
     *
     * @throws NullPointerException if both {@code defaultId} and {@code webProp}
     *         are {@code null}, leaving the notice with no id source
     */
    public TosNotice {
        if (defaultId == null) {
            Objects.requireNonNull(webProp, "a notice with no defaultId must have a webProp");
        }
    }
}
