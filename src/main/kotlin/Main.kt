import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import navigation.NavControllers
import navigation.Route
import presentation.MerchantCenterTheme
import viewmodel.MainViewModel

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1640.dp, 868.dp))
    val mainViewModel = remember { MainViewModel() }

    Window(
        state = windowState,
        onCloseRequest = ::exitApplication,
        title = "Merchant Center",
        icon = painterResource("merchant.png")
    ) {
        MerchantCenterTheme(darkTheme = isSystemInDarkTheme()) {
            NavControllers(
                startDestination = Route.Sales,
                mainViewModel = mainViewModel
            )
        }
    }
}