package screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import controller.State
import controller.WhatsappController
import org.jetbrains.skia.Image
import org.succlz123.lib.screen.LocalScreenNavigator
import org.succlz123.lib.screen.ScreenArgs
import widget.ScrollBox

private val whatsapp by WhatsappController

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Home(state: MutableState<State>) = Column {
    val navigator = LocalScreenNavigator.current
    when(val value = state.value) {
        is State.Loading -> {
            Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is State.Home -> {
            TopAppBar(
                    content = {
                        Row(
                                modifier = Modifier.fillMaxWidth().align(Alignment.CenterVertically),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                    "Chats: ${whatsapp.store().chats().toList().size}, " +
                                            "Contacts: ${whatsapp.store().contacts().size}" +
                                            if(value.recentProgress != null) ", Recent sync: ${value.recentProgress}%" else "" +
                                                    if(value.historyProgress != null) ", History sync: ${value.historyProgress}%" else ""
                            )

                            Button(
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                                    onClick = { whatsapp.logout() }
                            ) {
                                Text(text = "LOGOUT")
                            }
                        }
                    }
            )
            ScrollBox {
                items(whatsapp.store().chats().filterNot { it.archived() }.toList()) {
                    ListItem(
                            modifier = Modifier.clickable {
                                navigator.push(
                                        screenName = "chat",
                                        arguments = ScreenArgs.putValue("chat", it)
                                )
                            },
                            text = {
                                Text(it.name())
                            }
                    )
                }
            }
        }

        is State.Login -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                        bitmap = Image.makeFromEncoded(value.qr).toComposeImageBitmap(),
                        "Qr code"
                )
            }
        }
    }
}