package it.auties.whatsapp4j.response.model.json;

import it.auties.whatsapp4j.response.impl.json.*;
import it.auties.whatsapp4j.response.model.common.ResponseModel;

/**
 * An interface to represent a class that may represent a JSON String sent by WhatsappWeb's WebSocket
 */
public sealed interface JsonResponseModel extends ResponseModel permits AckResponse, BlocklistResponse, ChatCmdResponse, ChatPictureResponse, CommonGroupsResponse, DescriptionChangeResponse, DiscardResponse, GroupActionResponse, GroupInviteCodeResponse, GroupMetadataResponse, GroupModificationResponse, InitialResponse, MediaConnectionResponse, MessageResponse, PhoneBatteryResponse, PresenceResponse, PropsResponse, SimpleStatusResponse, SubjectChangeResponse, TakeOverResponse, UserInformationResponse, UserStatusResponse {
}
