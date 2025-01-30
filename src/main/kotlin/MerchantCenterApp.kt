import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import navigation.NavControllers
import navigation.Route
import viewmodel.MainViewModel

@Composable
fun MerchantCenterApp(
    mainViewModel: MainViewModel,
) {
    Surface(
        modifier = Modifier.fillMaxSize().background(
            color = MaterialTheme.colorScheme.background
        )
    ) {
        NavControllers(
            startDestination = Route.Sales,
            mainViewModel = mainViewModel,
        )
    }
}