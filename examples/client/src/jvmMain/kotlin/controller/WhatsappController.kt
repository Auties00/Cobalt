package controller

import androidx.compose.runtime.MutableState
import com.google.zxing.client.j2se.MatrixToImageWriter
import it.auties.whatsapp.api.DisconnectReason
import it.auties.whatsapp.api.QrHandler
import it.auties.whatsapp.api.Whatsapp
import it.auties.whatsapp.api.WhatsappOptions
import it.auties.whatsapp.api.WhatsappOptions.WebOptions
import it.auties.whatsapp.listener.OnLoggedIn
import it.auties.whatsapp.model.info.MessageInfo
import java.io.ByteArrayOutputStream
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

private const val LOADED_ALL = 3

object WhatsappController {
    private lateinit var whatsapp: Whatsapp
    private var loadingCounter = 0

    fun connect(state: MutableState<State>) {
        whatsapp = Whatsapp.lastConnection(createOptions(state))
                .addLoggedInListener(OnLoggedIn { println("Connected") })
                .addContactsListener { _, contacts -> println("Contacts: ${contacts.size}") }
                .addChatsListener { chats -> println("Chats: ${chats.size}") }
                .addNodeReceivedListener { incoming -> println("Received node $incoming") }
                .addNodeSentListener { outgoing -> println("Sent node $outgoing") }
                .addActionListener { action, info -> println("New action: $action, info: $info") }
                .addSettingListener { setting -> println("New setting: $setting") }
                .addContactPresenceListener { chat, contact, status -> println("Status of ${contact.name()} changed in ${chat.name()} to ${status.name}") }
                .addAnyMessageStatusListener { _, contact, info, status -> println("Message ${info.id()} in chat ${info.chatName()} now has status $status for ${contact?.name()}") }
                .addDisconnectedListener { reason -> println("Disconnected: $reason") }
                .addLoggedInListener { -> onLoginProgress(state) }
                .addChatsListener { _ -> onLoginProgress(state) }
                .addContactsListener { _ -> onLoginProgress(state) }
                .addNewMessageListener { message -> onNewMessage(message, state) }
                .addHistorySyncProgressListener { progress, recent -> onSyncProgress(state, progress, recent)}
        whatsapp.connect()
    }

    private fun onNewMessage(message: MessageInfo, state: MutableState<State>) {
        println(message.toJson())
        state.value = state.value
    }

    private fun onLoginProgress(state: MutableState<State>) {
        if (++loadingCounter != LOADED_ALL) {
            state.value = State.Loading
            return
        }

        val oldHome = state.value as? State.Home;
        state.value = State.Home(
                oldHome?.messageCounter ?: 0,
                oldHome?.recentProgress,
                oldHome?.historyProgress
        )
    }

    private fun onSyncProgress(state: MutableState<State>, progress: Int, recent: Boolean) {
        val oldState = state.value
        if (oldState !is State.Home) return
        val recentProcess = if(recent) oldState.recentProgress else progress
        val historyProgress = if(!recent) oldState.historyProgress else progress
        state.value = State.Home(
                oldState.messageCounter,
                parsePercentage(recentProcess),
                parsePercentage(historyProgress)
        )
    }

    private fun parsePercentage(input: Int?): Int? =
            if (input == null || input >= 99) null else input

    private fun createOptions(state: MutableState<State>): WhatsappOptions =
        WebOptions.builder()
                    .qrHandler { qr -> onQrCode(qr, state) }
                    .build()

    private fun onQrCode(qr: String, state: MutableState<State>) {
        val matrix = QrHandler.createMatrix(qr, 500, 5)
        val os = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(matrix, "png", os)
        state.value = State.Login(os.toByteArray())
    }


    operator fun getValue(thisRef: Any?, property: KProperty<*>): Whatsapp
            = whatsapp
}