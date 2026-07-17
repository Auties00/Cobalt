package com.github.auties00.cobalt.media.transcode.text.preview;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.media.transcode.text.link.DeepLinkParser;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;

import java.lang.System.Logger.Level;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Builds preview cards for {@code chat.whatsapp.com} group-invite deep
 * links.
 *
 * <p>This resolver is invoked from
 * {@link com.github.auties00.cobalt.media.transcode.text.TextPipeline#run}
 * on the {@link DeepLinkParser.DeepLink.GroupInvite} branch. It queries
 * the server for the target group's metadata, derives the preview
 * description from the group's community relationship, and downloads the
 * group's profile picture as the inline thumbnail.
 */
@WhatsAppWebModule(moduleName = "WAWebLinkPreviewGroupUtils")
public final class GroupInvitePreviewResolver {
    /** The logger for {@link GroupInvitePreviewResolver}. */
    private static final System.Logger LOGGER = Log.get(GroupInvitePreviewResolver.class);

    /**
     * Holds the default description rendered when no community or
     * sub-group hint applies.
     *
     * <p>Surfaced when the invite points at a plain group outside a
     * community.
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewGroupUtils", exports = "GROUP_INVITE_DEFAULT_DESCRIPTION",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String GROUP_INVITE_DEFAULT_DESCRIPTION = "Group chat invite";

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private GroupInvitePreviewResolver() {
        throw new AssertionError();
    }

    /**
     * Resolves the preview for a group-invite URL and stamps the result
     * onto {@code message}.
     *
     * <p>Queries the server for the target group's metadata via
     * {@link LinkedWhatsAppClient#queryInviteGroupInfo(String)} and downloads
     * the group's profile picture as the inline JPEG thumbnail. On a
     * successful resolve the {@code title}, {@code description},
     * {@code previewType}, {@code doNotPlayInline}, and
     * {@code jpegThumbnail} fields are written onto {@code message} in
     * place. Returns {@code false} without mutating {@code message} when
     * {@code client}, {@code code}, or {@code message} is {@code null},
     * when {@code code} is empty, or when the metadata query fails or
     * yields no group.
     *
     * @implNote This implementation hands the server-supplied picture
     * bytes to {@link PreviewThumbnailFetcher#download(HttpClient, java.net.URI, Duration)},
     * which resizes them to 100x100 when {@code java.desktop} is on the
     * runtime path and otherwise embeds the source bytes unchanged.
     *
     * @param client     the WhatsApp client used to query the server
     * @param code       the invite code parsed from the URL
     * @param httpClient the HTTP client used to download the group
     *                   profile picture
     * @param timeout    the per-download timeout, derived from
     *                   {@code link_preview_wait_time}
     * @param message    the outgoing message to enrich; mutated in place
     * @return {@code true} when a preview was applied, {@code false}
     *         otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewGroupUtils", exports = "getGroupInviteLinkPreview",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean resolve(LinkedWhatsAppClient client, String code, HttpClient httpClient,
                                  Duration timeout, ExtendedTextMessage message) {
        if (client == null || code == null || code.isEmpty() || message == null) {
            return false;
        }
        GroupMetadata metadata;
        try {
            metadata = client.queryInviteGroupInfo(code).orElse(null);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "group invite metadata query failed for code " + new LogRedactable.Token(code), e);
            return false;
        }
        if (metadata == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "group invite preview skipped, no metadata for code {0}", new LogRedactable.Token(code));
            return false;
        }
        message.setTitle(metadata.subject());
        message.setDescription(inviteLinkDescription(client, metadata));
        message.setPreviewType(ExtendedTextMessage.PreviewType.NONE);
        message.setDoNotPlayInline(Boolean.TRUE);
        var thumbnailBytes = downloadGroupPicture(client, metadata.jid(), httpClient, timeout);
        if (thumbnailBytes != null) {
            message.setJpegThumbnail(thumbnailBytes);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "group invite preview resolved for {0}", metadata.jid());
        return true;
    }

    /**
     * Resolves the group profile picture URL and downloads its bytes.
     *
     * <p>Resolves the picture URL through
     * {@link LinkedWhatsAppClient#queryPicture(JidProvider)} and downloads it
     * via {@link PreviewThumbnailFetcher#download(HttpClient, java.net.URI, Duration)}.
     * Returns {@code null} when {@code groupJid} is {@code null}, when
     * the group has no picture set, or when the resolution or download
     * fails, so the caller can still attach a preview without an inline
     * thumbnail.
     *
     * @param client     the WhatsApp client used to resolve the URL
     * @param groupJid   the group whose picture is requested
     * @param httpClient the HTTP client used for the download
     * @param timeout    the per-download timeout
     * @return the downloaded JPEG bytes, or {@code null} when no picture
     *         was set or the download failed
     */
    private static byte[] downloadGroupPicture(LinkedWhatsAppClient client,
                                               Jid groupJid,
                                               HttpClient httpClient,
                                               Duration timeout) {
        if (groupJid == null) {
            return null;
        }
        try {
            var pictureUri = client.queryPicture(groupJid).orElse(null);
            if (pictureUri == null) {
                return null;
            }
            return PreviewThumbnailFetcher.download(httpClient, pictureUri, timeout);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "group picture download failed for " + new LogRedactable.User(String.valueOf(groupJid)), e);
            return null;
        }
    }

    /**
     * Picks the preview description based on the group's type.
     *
     * <p>The default-subgroup branch surfaces "Announcements"; the
     * sub-group branch surfaces {@code Group in "{community title}"}
     * using the parent community's resolved subject; everything else
     * falls back to {@link #GROUP_INVITE_DEFAULT_DESCRIPTION}.
     *
     * @param client   the WhatsApp client used to resolve the parent
     *                 community's display title when the invite points
     *                 at a sub-group
     * @param metadata the resolved group metadata
     * @return the preview description string
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewGroupUtils", exports = "getInviteLinkDescription",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String inviteLinkDescription(LinkedWhatsAppClient client,
                                                GroupMetadata metadata) {
        if (metadata.isDefaultSubgroup()) {
            return "Announcements";
        }
        var parentCommunity = metadata.parentCommunityJid().orElse(null);
        if (parentCommunity != null) {
            var parentTitle = client.store().chatStore().findChatMetadata(parentCommunity)
                    .filter(GroupMetadata.class::isInstance)
                    .map(GroupMetadata.class::cast)
                    .map(GroupMetadata::subject)
                    .orElse(null);
            if (parentTitle != null && !parentTitle.isEmpty()) {
                return "Group in \"" + parentTitle + "\"";
            }
        }
        return GROUP_INVITE_DEFAULT_DESCRIPTION;
    }
}
