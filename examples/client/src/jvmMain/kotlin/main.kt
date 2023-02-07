import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import controller.State
import controller.WhatsappController
import it.auties.whatsapp.model.chat.Chat
import org.succlz123.lib.screen.ScreenContainer
import org.succlz123.lib.screen.ScreenHost
import org.succlz123.lib.screen.rememberScreenNavigator
import screen.Chat
import screen.Home

private val whatsapp by WhatsappController

fun main() = application {
    ScreenContainer(
            onCloseRequest = {
                exitApplication()
            },
            title = "Whatsapp",
            state = WindowState(
                    position = WindowPosition.Aligned(Alignment.Center)
            )
    ) {
        App()
    }
}

@Composable
fun App() {
    val screenNavigator = rememberScreenNavigator()
    val state = mutableStateOf<State>(State.Loading)
    WhatsappController.connect(state)
    MaterialTheme {
        ScreenHost(screenNavigator = screenNavigator, rootScreenName = "home") {
            groupScreen(screenName = "home") {
                Home(state)
            }
            groupScreen(screenName = "chat") {
                Chat(it.arguments.map["chat"] as Chat)
            }
        }
    }
}
