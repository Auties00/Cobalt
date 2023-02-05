import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import controller.WhatsappController
import it.auties.whatsapp.model.chat.Chat
import org.succlz123.lib.screen.ScreenContainer
import org.succlz123.lib.screen.ScreenHost
import org.succlz123.lib.screen.rememberScreenNavigator
import screen.Chat
import screen.Home
import java.util.concurrent.CountDownLatch

fun main() = application {
    ScreenContainer(
            onCloseRequest = ::exitApplication,
            title = "Whatsapp Client",
            state = WindowState(
                    position = WindowPosition.Aligned(Alignment.Center)
            )
    ) {
        App()
    }
}

@Composable
fun App() {
    val state = mutableStateOf(0)
    val whatsapp by WhatsappController()
    val latch = CountDownLatch(3)
    whatsapp.addLoggedInListener { ->
        latch.countDown()
    }
    whatsapp.addChatsListener { _ ->
        latch.countDown()
    }
    whatsapp.addContactsListener { _ ->
        latch.countDown()
    }
    whatsapp.addNewMessageListener { _ ->
        onUpdate(state)
    }
    whatsapp.addHistorySyncProgressListener { _, _ ->
        onUpdate(state)
    }
    whatsapp.addNewContactListener { _ ->
        onUpdate(state)
    }
    whatsapp.addNewStatusListener { _ ->
        onUpdate(state)
    }
    whatsapp.connect()
    latch.await()
    val screenNavigator = rememberScreenNavigator()
    MaterialTheme {
        ScreenHost(screenNavigator = screenNavigator, rootScreenName = "home") {
            groupScreen(screenName = "home") {
                Home(whatsapp, state)
            }
            groupScreen(screenName = "chat") {
                Chat(it.arguments.map["chat"] as Chat)
            }
        }
    }
}

private fun onUpdate(state: MutableState<Int>) = runCatching {
    state.value++
}
