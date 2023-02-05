package screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import it.auties.whatsapp.api.Whatsapp
import org.succlz123.lib.screen.LocalScreenNavigator
import org.succlz123.lib.screen.ScreenArgs
import widget.ScrollBox


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Home(whatsapp: Whatsapp, state: MutableState<Int>) = Column {
    val navigator = LocalScreenNavigator.current
    val chats = whatsapp.store().chats().toList()
    val contacts = whatsapp.store().contacts()
    TopAppBar(
            content = {
                Text("Chats: ${chats.size}, Contacts: ${contacts.size}, Updates: ${state.value}")
            }
    )
    ScrollBox {
        items(chats) {
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