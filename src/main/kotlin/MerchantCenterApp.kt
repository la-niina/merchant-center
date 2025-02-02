import androidx.compose.runtime.Composable
import navigation.NavControllers
import navigation.Route
import viewmodel.MainViewModel

@Composable
fun MerchantCenterApp(
    mainViewModel: MainViewModel,
) {
    NavControllers(
        startDestination = Route.Sales,
        mainViewModel = mainViewModel,
    )
}