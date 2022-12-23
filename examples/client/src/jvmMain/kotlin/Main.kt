import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import screen.HomeScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Whatsapp Client",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        HomeScreen()
    }
}
