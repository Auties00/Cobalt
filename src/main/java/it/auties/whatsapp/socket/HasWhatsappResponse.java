package it.auties.whatsapp.socket;

import it.auties.whatsapp.protobuf.contact.ContactJid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Optional;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public class HasWhatsappResponse extends Response {
    private ContactJid contact;
    private boolean hasWhatsapp;

    public HasWhatsappResponse(@NonNull Node source) {
        super(source);
        this.contact = source.attributes().getJid("jid").orElse(null);
        this.hasWhatsapp = source.findNode("contact")
                .attributes()
                .getString("type")
                .equals("in");
    }

    public Optional<ContactJid> contact(){
        return Optional.ofNullable(contact);
    }
}
