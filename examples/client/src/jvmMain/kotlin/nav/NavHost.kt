package nav

import androidx.compose.runtime.Composable

class NavigationHost(
        val navController: NavController,
        val contents: @Composable NavigationGraphBuilder.() -> Unit
) {

    @Composable
    fun build() {
        NavigationGraphBuilder().renderContents()
    }

    inner class NavigationGraphBuilder(
            val navController: NavController = this@NavigationHost.navController
    ) {
        @Composable
        fun renderContents() {
            this@NavigationHost.contents(this)
        }
    }
}


/**
 * Composable to build the Navigation Host
 */
@Composable
fun NavigationHost.NavigationGraphBuilder.composable(
        route: String,
        content: @Composable () -> Unit
) {
    if (navController.currentScreen.value == route) {
        content()
    }
}