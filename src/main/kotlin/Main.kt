import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
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
        MaterialTheme {
            Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                MerchantCenterApp(
                    mainViewModel = mainViewModel,
                )
            }
        }
    }
}