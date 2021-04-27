package it.auties.whatsapp4j.utils;

import com.google.protobuf.ByteString;
import ezvcard.VCard;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappCoordinates;
import it.auties.whatsapp4j.model.WhatsappMediaMessageType;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.model.WhatsappUserMessage;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * A utility class used to build raw protobuf objects easily.
 * This class really shouldn't be this complicated.
 */
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

    public @NotNull WhatsappProtobuf.Message createTextMessage(@NotNull String text, WhatsappUserMessage quotedMessage, boolean forwarded){
        if (!forwarded && quotedMessage == null) {
            return WhatsappProtobuf.Message.newBuilder().setConversation(text).build();
        }

        return WhatsappProtobuf.Message.newBuilder()
                        .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
                                .setText(text)
                                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                                .build())
                        .build();

    }

    public @NotNull WhatsappProtobuf.Message createMediaMessage(String caption, byte @NotNull [] media, @NotNull WhatsappMediaMessageType type, WhatsappUserMessage quotedMessage, boolean forwarded){
        var upload = CypherUtils.mediaEncrypt(MANAGER.mediaConnection(), media, type);
        var message = WhatsappProtobuf.Message.newBuilder();

        switch (type){
            case IMAGE -> message.setImageMessage(WhatsappProtobuf.ImageMessage.newBuilder()
                    .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                    .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                    .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                    .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                    .setUrl(upload.url())
                    .setContextInfo(createContextInfo(quotedMessage, forwarded))
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
                        .setContextInfo(createContextInfo(quotedMessage, forwarded))
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
                        .setContextInfo(createContextInfo(quotedMessage, forwarded))
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
                    .setContextInfo(createContextInfo(quotedMessage, forwarded))
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
                        .setContextInfo(createContextInfo(quotedMessage, forwarded))
                        .setMimetype("image/webp")
                        .build());
            }
        }

        return message.build();
    }

    public @NotNull WhatsappProtobuf.Message createLocationMessage(@NotNull WhatsappCoordinates coordinates,  String caption, byte  [] thumbnail, boolean live, int accuracy, float speed, WhatsappUserMessage quotedMessage, boolean forwarded) {
        var message = WhatsappProtobuf.Message.newBuilder();
        if(live){
            message.setLiveLocationMessage(WhatsappProtobuf.LiveLocationMessage.newBuilder()
                    .setContextInfo(createContextInfo(quotedMessage, forwarded))
                    .setDegreesLatitude(coordinates.latitude())
                    .setDegreesLongitude(coordinates.longitude())
                    .setCaption(caption)
                    .setAccuracyInMeters(accuracy)
                    .setSpeedInMps(speed)
                    .setJpegThumbnail(thumbnail == null ? null : ByteString.copyFrom(thumbnail)));

            return message.build();
        }

        Validate.isTrue(caption == null, "WhatsappAPI: Cannot create a LocationMessage with a caption");
        message.setLocationMessage(WhatsappProtobuf.LocationMessage.newBuilder()
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setDegreesLatitude(coordinates.latitude())
                .setDegreesLongitude(coordinates.longitude())
                .setAccuracyInMeters(accuracy)
                .setSpeedInMps(speed)
                .setJpegThumbnail(thumbnail == null ? null : ByteString.copyFrom(thumbnail)));

        return message.build();
    }

    public @NotNull WhatsappProtobuf.Message createGroupInviteMessage(@NotNull String jid, @NotNull String name,  String caption, @NotNull String code,  ByteBuffer thumbnail,  ZonedDateTime expiration, WhatsappUserMessage quotedMessage, boolean forwarded) {
        var invite = WhatsappProtobuf.GroupInviteMessage.newBuilder()
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
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

    private @NotNull WhatsappProtobuf.ContextInfo createContextInfo(WhatsappUserMessage quotedMessage, boolean forwarded){
        var builder =  WhatsappProtobuf.ContextInfo.newBuilder().setIsForwarded(forwarded);
        if(quotedMessage == null){
            return builder.build();
        }

        return builder.setQuotedMessage(quotedMessage.info().getMessage())
                .setParticipant(quotedMessage.senderJid())
                .setStanzaId(quotedMessage.info().getKey().getId())
                .setRemoteJid(quotedMessage.info().getKey().getRemoteJid())
                .setIsForwarded(forwarded)
                .build();
    }
}
