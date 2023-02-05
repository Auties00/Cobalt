package controller

import it.auties.whatsapp.api.DisconnectReason
import it.auties.whatsapp.api.Whatsapp
import it.auties.whatsapp.api.Whatsapp.Options
import it.auties.whatsapp.listener.OnLoggedIn
import kotlin.reflect.KProperty

class WhatsappController {
    private val whatsapp: Whatsapp = createWhatsappInstance()

    private fun createWhatsappInstance(): Whatsapp = Whatsapp.lastConnection(createOptions())
            .addLoggedInListener(OnLoggedIn { println("Connected") })
            .addNewMessageListener { message -> println(message.toJson()) }
            .addContactsListener { _, contacts -> println("Contacts: ${contacts.size}") }
            .addChatsListener { chats -> println("Chats: ${chats.size}") }
            .addNodeReceivedListener { incoming -> println("Received node $incoming") }
            .addNodeSentListener { outgoing -> println("Sent node $outgoing") }
            .addActionListener { action, info -> println("New action: $action, info: $info") }
            .addSettingListener { setting -> println("New setting: $setting") }
            .addContactPresenceListener { chat, contact, status -> println("Status of ${contact.name()} changed in ${chat.name()} to ${status.name}") }
            .addAnyMessageStatusListener { _, contact, info, status -> println("Message ${info.id()} in chat ${info.chatName()} now has status $status for ${contact?.name()}",) }
            .addDisconnectedListener { reason -> println("Disconnected: $reason") }

    private fun createOptions(): Options =
            Options.defaultOptions().withAutomaticallySubscribeToPresences(false)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Whatsapp
            = whatsapp
}