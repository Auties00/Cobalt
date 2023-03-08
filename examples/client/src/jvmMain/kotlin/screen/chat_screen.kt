package screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import it.auties.whatsapp.model.chat.Chat
import it.auties.whatsapp.model.message.standard.TextMessage
import org.succlz123.lib.screen.LocalScreenNavigator
import widget.ScrollBox
import kotlin.math.max

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Chat(chat: Chat) = Column {
    val navigator = LocalScreenNavigator.current
    val messages = chat.messages()
            .map { it.message().content() }
            .toList()
    TopAppBar(
            title = { Text(chat.name()) },
            navigationIcon = {
                IconButton(onClick = {
                    navigator.remove(screenName = "chat")
                }) {
                    Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                    )
                }
            }
    )
    ScrollBox(firstVisibleIndex = max(messages.size - 1, 0)) {
        items(messages) {
            ListItem(
                    text = {
                        Text(when(it){
                            is TextMessage -> it.text()
                            else -> "[${it.javaClass.name}]"
                        })
                    }
            )
        }
    }
}