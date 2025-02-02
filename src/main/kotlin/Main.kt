import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import presentation.MerchantCenterTheme
import viewmodel.MainViewModel

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1440.dp, 768.dp))
    val mainViewModel = MainViewModel()

    LaunchedEffect(Unit, mainViewModel) {
        mainViewModel.loadProducts()
        mainViewModel.loadAllProducts()
        mainViewModel.loadCurrentDateTime()
    }

    val icon = painterResource("merchant.png")

    if (isTraySupported) {
        Tray(
            icon = icon,
            menu = {
                Item("Quit App", onClick = ::exitApplication)
            }
        )
    }

    Window(
        state = windowState,
        onCloseRequest = ::exitApplication,
        title = "Merchant Center",
        icon = icon
    ) {
        MerchantCenterTheme(
            darkTheme = isSystemInDarkTheme()
        ) {
            MerchantCenterApp(
                mainViewModel = mainViewModel,
            )
        }
    }
}