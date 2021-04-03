package it.auties.whatsapp4j.utils;

import com.google.protobuf.ByteString;
import ezvcard.VCard;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappCoordinates;
import it.auties.whatsapp4j.model.WhatsappMediaMessageType;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.model.WhatsappUserMessage;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@UtilityClass
public class ProtobufUtils {
    private final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    public @NotNull WhatsappProtobuf.WebMessageInfo createMessageInfo(@NotNull WhatsappProtobuf.Message message, @NotNull String recipientJid){
        return WhatsappProtobuf.WebMessageInfo.newBuilder()
                .setMessage(message)
                .setKey(WhatsappProtobuf.MessageKey.newBuilder()
                        .setFromMe(true)
                        .setRemoteJid(recipientJid)
                        .setId(WhatsappUtils.randomId())
                        .build())
                .setMessageTimestamp(Instant.now().getEpochSecond())
                .setStatus(WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus.PENDING)
                .build();
    }

    public @NotNull WhatsappProtobuf.Message createTextMessage(@NotNull String text, @Nullable WhatsappUserMessage quotedMessage, boolean forwarded){
        if (!forwarded && quotedMessage == null) {
            return WhatsappProtobuf.Message.newBuilder().setConversation(text).build();
        }

        var context = WhatsappProtobuf.ContextInfo.newBuilder().setIsForwarded(true);
        if(quotedMessage != null){
            context
                    .setQuotedMessage(quotedMessage.info().getMessage())
                    .setParticipant(quotedMessage.senderJid().orElse(MANAGER.phoneNumber()))
                    .setStanzaId(quotedMessage.info().getKey().getId())
                    .setRemoteJid(quotedMessage.info().getKey().getRemoteJid())
                    .setIsForwarded(forwarded);
        }

        return WhatsappProtobuf.Message.newBuilder()
                        .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
                                .setText(text)
                                .setContextInfo(context.build())
                                .build())
                        .build();

    }

    public @NotNull WhatsappProtobuf.Message createMediaMessage(@Nullable String caption, @NotNull ByteBuffer media, @NotNull WhatsappMediaMessageType type){
        var upload = CypherUtils.mediaEncrypt(MANAGER.mediaConnection(), media.array(), type);
        var message = WhatsappProtobuf.Message.newBuilder();

        switch (type){
            case IMAGE -> message.setImageMessage(WhatsappProtobuf.ImageMessage.newBuilder()
                    .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                    .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                    .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                    .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                    .setUrl(upload.url())
                    .setCaption(caption)
                    .setMimetype("image/jpeg")
                    .build());

            case DOCUMENT -> {
                Validate.isTrue(caption == null, "WhatsappAPI: Cannot create a DocumentMessage with a caption");
                message.setDocumentMessage(WhatsappProtobuf.DocumentMessage.newBuilder()
                        .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                        .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                        .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                        .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                        .setUrl(upload.url())
                        .setMimetype("application/pdf")
                        .build());
            }

            case AUDIO -> {
                Validate.isTrue(caption == null, "WhatsappAPI: Cannot create an AudioMessage with a caption");
                message.setAudioMessage(WhatsappProtobuf.AudioMessage.newBuilder()
                        .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                        .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                        .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                        .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                        .setUrl(upload.url())
                        .setMimetype("audio/ogg; codecs=opus")
                        .setStreamingSidecar(ByteString.copyFrom(upload.sidecar()))
                        .build());
            }

            case VIDEO -> message.setVideoMessage(WhatsappProtobuf.VideoMessage.newBuilder()
                    .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                    .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                    .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                    .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                    .setUrl(upload.url())
                    .setCaption(caption)
                    .setMimetype("video/mp4")
                    .setStreamingSidecar(ByteString.copyFrom(upload.sidecar()))
                    .build());

            case STICKER -> {
                Validate.isTrue(caption == null, "WhatsappAPI: Cannot create a StickerMessage with a caption");
                message.setStickerMessage(WhatsappProtobuf.StickerMessage.newBuilder()
                        .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                        .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                        .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                        .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                        .setUrl(upload.url())
                        .setMimetype("image/webp")
                        .build());
            }
        }

        return message.build();
    }

    public @NotNull WhatsappProtobuf.Message createLocationMessage(@NotNull String text, @Nullable WhatsappUserMessage quotedMessage, boolean forwarded){
        if (!forwarded && quotedMessage == null) {
            return WhatsappProtobuf.Message.newBuilder().setConversation(text).build();
        }

        var context = WhatsappProtobuf.ContextInfo.newBuilder().setIsForwarded(true);
        if(quotedMessage != null){
            context
                    .setQuotedMessage(quotedMessage.info().getMessage())
                    .setParticipant(quotedMessage.senderJid().orElse(MANAGER.phoneNumber()))
                    .setStanzaId(quotedMessage.info().getKey().getId())
                    .setRemoteJid(quotedMessage.info().getKey().getRemoteJid())
                    .setIsForwarded(forwarded);
        }

        return WhatsappProtobuf.Message.newBuilder()
                .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
                        .setText(text)
                        .setContextInfo(context.build())
                        .build())
                .build();

    }

    public @NotNull WhatsappProtobuf.Message createLocationMessage(@NotNull WhatsappCoordinates coordinates, @Nullable String caption, @Nullable ByteBuffer thumbnail, boolean live, int accuracy, float speed) {
        var message = WhatsappProtobuf.Message.newBuilder();
        if(live){
            message.setLiveLocationMessage(WhatsappProtobuf.LiveLocationMessage.newBuilder()
                    .setDegreesLatitude(coordinates.latitude())
                    .setDegreesLongitude(coordinates.longitude())
                    .setCaption(caption)
                    .setAccuracyInMeters(accuracy)
                    .setSpeedInMps(speed)
                    .setJpegThumbnail(thumbnail == null ? null : ByteString.copyFrom(thumbnail.array())));

            return message.build();
        }

        Validate.isTrue(caption == null, "WhatsappAPI: Cannot create a LocationMessage with a caption");
        message.setLocationMessage(WhatsappProtobuf.LocationMessage.newBuilder()
                .setDegreesLatitude(coordinates.latitude())
                .setDegreesLongitude(coordinates.longitude())
                .setAccuracyInMeters(accuracy)
                .setSpeedInMps(speed)
                .setJpegThumbnail(thumbnail == null ? null : ByteString.copyFrom(thumbnail.array())));

        return message.build();
    }

    public @NotNull WhatsappProtobuf.Message createGroupInviteMessage(@NotNull String jid, @NotNull String name, @Nullable String caption, @NotNull String code, @Nullable ByteBuffer thumbnail, @Nullable ZonedDateTime expiration) {
        var invite = WhatsappProtobuf.GroupInviteMessage.newBuilder()
                .setGroupJid(jid)
                .setGroupName(name)
                .setCaption(caption)
                .setInviteCode(code)
                .setJpegThumbnail(thumbnail == null ? null : ByteString.copyFrom(thumbnail.array()))
                .setInviteExpiration(expiration == null ? ZonedDateTime.now().plusDays(3).toEpochSecond() : expiration.toEpochSecond())
                .build();

        return WhatsappProtobuf.Message.newBuilder().setGroupInviteMessage(invite).build();
    }

    public @NotNull WhatsappProtobuf.Message createContactMessage(@NotNull List<VCard> sharedContacts) {
        if(sharedContacts.size() == 1){
            return WhatsappProtobuf.Message.newBuilder()
                    .setContactMessage(toContactMessage(sharedContacts.get(0)))
                    .build();
        }

        var contactsArrayMessageBuilder = WhatsappProtobuf.ContactsArrayMessage.newBuilder();
        for(var x = 0; x < sharedContacts.size(); x++){
            contactsArrayMessageBuilder.setContacts(x, toContactMessage(sharedContacts.get(x)));
        }

        return WhatsappProtobuf.Message.newBuilder()
                .setContactsArrayMessage(contactsArrayMessageBuilder.build())
                .build();
    }

    private @NotNull WhatsappProtobuf.ContactMessage toContactMessage(@NotNull VCard vCard){
        return WhatsappProtobuf.ContactMessage.newBuilder()
                .setVcard(vCard.write())
                .build();
    }
}
