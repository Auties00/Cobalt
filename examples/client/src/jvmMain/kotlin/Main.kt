
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import nav.NavigationHost
import nav.composable
import nav.rememberNavController
import screen.ChatScreen

fun main() = application {
    Window(
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
    val navController by rememberNavController("Home")
    val currentScreen by remember {
        navController.currentScreen
    }

    MaterialTheme {

            Box(
                    modifier = Modifier.fillMaxSize()
            ) {
                NavigationRail(
                        modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()
                ) {
                    NavigationRailItem(
                            selected = currentScreen == "Home",
                            icon = {
                                Icon(
                                        imageVector = Icons.Filled.Face,
                                        contentDescription = "Home"
                                )
                            },
                            label = {
                                Text("Home")
                            },
                            alwaysShowLabel = false,
                            onClick = {
                                navController.navigate("Home")
                            }
                    )
                }

                Box(
                        modifier = Modifier.fillMaxHeight()
                ) {
                    NavigationHost(navController) {
                        composable("Home") {
                            ChatScreen()
                        }

                        composable("Home1") {
                            ChatScreen()
                        }

                        composable("Home2") {
                            ChatScreen()
                        }

                        composable("Home3") {
                            ChatScreen()
                        }

                    }.build()
                }
            }
    }
}
