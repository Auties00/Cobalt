package widget

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ScrollBox(
        modifier: Modifier = Modifier,
        content: LazyListScope.() -> Unit
) {
    val verticalScrollState = rememberLazyListState(0)
    Box(modifier.fillMaxHeight()) {
        LazyColumn(state = verticalScrollState) {
            content()
        }
        VerticalScrollbar(
                adapter = rememberScrollbarAdapter(verticalScrollState),
                modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}